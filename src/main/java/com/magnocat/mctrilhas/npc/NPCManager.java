package com.magnocat.mctrilhas.npc;

// Bukkit & Paper API Imports
import com.destroystokyo.paper.profile.ProfileProperty;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

// Java Standard Library Imports
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia a criação, persistência e interação com NPCs (Non-Player Characters).
 * Esta classe é responsável por:
 * - Carregar as definições de NPCs do arquivo `npcs.yml`.
 * - Gerar os NPCs no mundo quando o servidor inicia.
 * - Salvar novos NPCs ou alterações no arquivo de configuração.
 * - Fornecer métodos para criar e gerenciar NPCs via comandos.
 */
public class NPCManager {

    // =========================================================================
    // Fields
    // =========================================================================

    private final MCTrilhasPlugin plugin;
    private final File npcConfigFile;
    private final FileConfiguration npcConfig;
    private final Map<String, Npc> npcs = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityIdToNpcId = new ConcurrentHashMap<>();

    // =========================================================================
    // Constructor
    // =========================================================================

    public NPCManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.npcConfigFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcConfigFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
        this.npcConfig = YamlConfiguration.loadConfiguration(npcConfigFile);

        // Initialization sequence
        loadNpcs();
        startBehaviorTask();
        startGreetingTask();
    }

    // =========================================================================
    // NPC Loading & Spawning
    // =========================================================================

    /**
     * Carrega todos os NPCs definidos no `npcs.yml` e os gera no mundo.
     */
    private void loadNpcs() {
        ConfigurationSection section = npcConfig.getConfigurationSection("npcs");
        if (section == null) {
            plugin.logInfo("Nenhum NPC encontrado para carregar em npcs.yml.");
            return;
        }

        for (String id : section.getKeys(false)) {
            Location loc = section.getLocation(id + ".location");
            if (loc == null || loc.getWorld() == null) {
                plugin.logWarn("Localização inválida ou mundo não carregado para o NPC '" + id + "'. O NPC não será gerado.");
                continue;
            }

            String name = ChatColor.translateAlternateColorCodes('&', section.getString(id + ".name", "NPC"));
            String texture = section.getString(id + ".skin.texture");
            String signature = section.getString(id + ".skin.signature");
            String startDialogueId = section.getString(id + ".start-dialogue-id");

            spawnNpc(id, name, loc, texture, signature, startDialogueId);
        }
        plugin.logInfo(npcs.size() + " NPCs foram carregados e gerados no mundo.");
    }

    /**
     * Gera um NPC no mundo e o adiciona ao mapa de gerenciamento.
     */
    private void spawnNpc(String id, String name, Location location, String texture, String signature, String startDialogueId) {
        if (npcs.containsKey(id.toLowerCase())) {
            plugin.logWarn("Tentativa de gerar um NPC com ID duplicado: '" + id + "'. Ignorando.");
            return;
        }

        // A API do Paper permite criar NPCs de forma nativa e estável.
        Player npcEntity = (Player) location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.PLAYER);
        npcEntity.setInvulnerable(true);
        npcEntity.setAI(false);
        npcEntity.setSilent(true);

        // Aplica a skin customizada usando a API de profile do Paper.
        if (texture != null && signature != null && !texture.isEmpty() && !signature.isEmpty()) {
            com.destroystokyo.paper.profile.PlayerProfile profile = npcEntity.getPlayerProfile();
            profile.getProperties().add(new ProfileProperty("textures", texture, signature));
            npcEntity.setPlayerProfile(profile);
        }

        // Define o nome de exibição customizado.
        npcEntity.setCustomName(name);
        npcEntity.setCustomNameVisible(true);

        // Adiciona o NPC ao nosso mapa de gerenciamento.
        Npc npcData = new Npc(id, name, location, texture, signature, startDialogueId, npcEntity.getUniqueId());
        npcs.put(id.toLowerCase(), npcData);
        entityIdToNpcId.put(npcEntity.getUniqueId(), id.toLowerCase());
    }

    // =========================================================================
    // NPC Management (Admin Commands)
    // =========================================================================

    /**
     * Cria um novo NPC na localização do jogador que executou o comando, usando a skin do mesmo.
     * @param creator O jogador que está criando o NPC.
     * @param id O ID único para o novo NPC.
     * @param name O nome de exibição para o novo NPC.
     */
    public void createNpc(Player creator, String id, String name) {
        if (npcs.containsKey(id.toLowerCase())) {
            creator.sendMessage(ChatColor.RED + "Já existe um NPC com o ID '" + id + "'.");
            return;
        }

        com.destroystokyo.paper.profile.PlayerProfile profile = creator.getPlayerProfile();
        ProfileProperty textures = profile.getProperties().stream().filter(p -> p.getName().equals("textures")).findFirst().orElse(null);

        if (textures == null) {
            creator.sendMessage(ChatColor.RED + "Não foi possível obter a textura da sua skin.");
            return;
        }

        spawnNpc(id, ChatColor.translateAlternateColorCodes('&', name), creator.getLocation(), textures.getValue(), textures.getSignature(), null); // Diálogo inicial nulo por padrão
        saveNpcs();
        creator.sendMessage(ChatColor.GREEN + "NPC '" + name + "' (ID: " + id + ") criado com sucesso na sua localização!");
    }

    /**
     * Deleta um NPC existente do mundo e do arquivo de configuração.
     * @param sender Quem está executando o comando (jogador ou console).
     * @param id O ID do NPC a ser deletado.
     */
    public void deleteNpc(CommandSender sender, String id) {
        String lowerId = id.toLowerCase();
        Npc npcData = npcs.get(lowerId);

        if (npcData == null) {
            sender.sendMessage(ChatColor.RED + "Nenhum NPC encontrado com o ID '" + id + "'.");
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Entity npcEntity = Bukkit.getEntity(npcData.entityId());
            if (npcEntity != null) npcEntity.remove();
        });

        npcs.remove(lowerId);
        entityIdToNpcId.remove(npcData.entityId());
        saveNpcs();
        sender.sendMessage(ChatColor.GREEN + "NPC com ID '" + id + "' foi removido com sucesso.");
    }

    // =========================================================================
    // NPC Behavior Tasks
    // =========================================================================

    /**
     * Inicia uma tarefa periódica que gerencia o comportamento dos NPCs, como
     * olhar para jogadores, andar aleatoriamente e fazer emotes.
     */
    private void startBehaviorTask() {
        new BukkitRunnable() {
            private final java.util.Random random = new java.util.Random();

            @Override
            public void run() {
                for (Npc npcData : npcs.values()) {
                    Entity npcEntity = Bukkit.getEntity(npcData.entityId());
                    if (npcEntity == null || !npcEntity.isValid()) {
                        continue;
                    }

                    List<Player> nearbyPlayers = npcEntity.getNearbyEntities(10, 10, 10).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .toList();

                    if (!nearbyPlayers.isEmpty()) {
                        Player closestPlayer = nearbyPlayers.stream()
                                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(npcEntity.getLocation())))
                                .orElse(null);

                        if (closestPlayer != null) {
                            Location npcLocation = npcEntity.getLocation();
                            npcLocation.setDirection(closestPlayer.getLocation().toVector().subtract(npcLocation.toVector()));
                            npcEntity.teleport(npcLocation);
                        }

                        if (npcData.id().equalsIgnoreCase("chefe_magno") && random.nextInt(100) < 5) {
                            int action = random.nextInt(5);
                            Player npcPlayer = (Player) npcEntity;

                            switch (action) {
                                case 0: // Acenar
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "emote acenar " + npcPlayer.getName());
                                    break;
                                case 1: // Andar um pouco
                                    Location originalLoc = npcData.location();
                                    if (npcPlayer.getLocation().distanceSquared(originalLoc) < 4) {
                                        Location newLoc = npcPlayer.getLocation().clone().add(random.nextDouble() * 2 - 1, 0, random.nextDouble() * 2 - 1);
                                        npcPlayer.teleport(newLoc);
                                    }
                                    break;
                                case 2: // Agachar e levantar
                                    npcPlayer.setSneaking(true);
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (npcPlayer.isValid()) npcPlayer.setSneaking(false);
                                        }
                                    }.runTaskLater(plugin, 20L);
                                    break;
                                case 3: // Pular
                                    npcPlayer.setVelocity(npcPlayer.getVelocity().setY(0.4));
                                    break;
                                case 4: // Trocar item na mão
                                    Material[] items = {Material.COMPASS, Material.MAP, Material.WRITABLE_BOOK, Material.DIAMOND_AXE, Material.AIR};
                                    Material nextItem = items[random.nextInt(items.length)];
                                    if (nextItem == npcPlayer.getInventory().getItemInMainHand().getType()) {
                                        nextItem = Material.AIR;
                                    }
                                    npcPlayer.getInventory().setItemInMainHand(new ItemStack(nextItem));
                                    break;
                            } // Fim do switch
                        } // Fim do if de comportamento aleatório
                    } // Fim do if de jogadores próximos
                } // Fim do for de NPCs
            } // Fim do método run()
        }.runTaskTimer(plugin, 100L, 20L); // Inicia após 5s, repete a cada 1s
    }

    /**
     * Inicia uma tarefa periódica que faz o "Chefe Magno" saudar jogadores
     * próximos uma vez por dia.
     */
    private void startGreetingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Npc chefeMagno = npcs.get("chefe_magno");
                if (chefeMagno == null) return;

                Entity npcEntity = Bukkit.getEntity(chefeMagno.entityId());
                if (npcEntity == null || !npcEntity.isValid()) return;

                long oneDayInMillis = 24 * 60 * 60 * 1000;

                for (Player player : npcEntity.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(npcEntity.getLocation()) < 100) {
                        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                        if (playerData != null && (System.currentTimeMillis() - playerData.getLastNpcGreetingTime() > oneDayInMillis)) {
                            player.sendMessage(ChatColor.GOLD + "[Chefe Magno]: " + ChatColor.WHITE + "Sempre alerta, " + player.getName() + "! Que bom te ver por aqui.");
                            playerData.setLastNpcGreetingTime(System.currentTimeMillis());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60 * 5); // Inicia após 1 min, repete a cada 5 min
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Retorna os dados de um NPC com base no UUID de sua entidade no mundo.
     * @param entityId O UUID da entidade.
     * @return O objeto Npc correspondente, ou null se não for um NPC gerenciado.
     */
    public Npc getNpcByEntityId(UUID entityId) {
        String npcId = entityIdToNpcId.get(entityId);
        if (npcId != null) {
            return npcs.get(npcId);
        }
        return null;
    }

    /**
     * Salva todos os NPCs gerenciados de volta para o arquivo `npcs.yml`.
     * Este método deve ser chamado quando um novo NPC é criado ou removido.
     */
    public void saveNpcs() {
        // Limpa a seção antiga para garantir que NPCs removidos não persistam.
        npcConfig.set("npcs", null);

        for (Npc npc : npcs.values()) {
            String path = "npcs." + npc.id().toLowerCase();
            npcConfig.set(path + ".name", npc.name().replace('§', '&')); // Salva com '&'
            npcConfig.set(path + ".location", npc.location());
            npcConfig.set(path + ".skin.texture", npc.skinTexture());
            npcConfig.set(path + ".skin.signature", npc.skinSignature());
            npcConfig.set(path + ".start-dialogue-id", npc.startDialogueId());
        }

        try {
            npcConfig.save(npcConfigFile);
        } catch (IOException e) {
            plugin.logSevere("Não foi possível salvar o arquivo npcs.yml: " + e.getMessage());
        }
    }

    /**
     * Retorna uma coleção de todos os NPCs gerenciados.
     *
     * @return Uma coleção não modificável dos dados dos NPCs.
     */
    public Collection<Npc> getAllNpcs() {
        return Collections.unmodifiableCollection(npcs.values());
    }


}