package com.magnocat.mctrilhas.npc;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Gerencia a criação, persistência e interação com NPCs (Non-Player Characters).
 * <p>
 * Esta classe é responsável por:
 * - Carregar as definições de NPCs do arquivo `npcs.yml`.
 * - Gerar os NPCs no mundo quando o servidor inicia.
 * - Salvar novos NPCs ou alterações no arquivo de configuração.
 * - Fornecer métodos para criar e gerenciar NPCs via comandos.
 */
public class NPCManager {

    private final MCTrilhasPlugin plugin;
    private final File npcConfigFile;
    private final FileConfiguration npcConfig;
    private final Map<String, Npc> npcs = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityIdToNpcId = new ConcurrentHashMap<>();

    public NPCManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.npcConfigFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcConfigFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
        this.npcConfig = YamlConfiguration.loadConfiguration(npcConfigFile);
        loadNpcs();
        startLookTask();
    }

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

            // Gera o NPC no mundo
            spawnNpc(id, name, loc, texture, signature, startDialogueId);
        }
        plugin.logInfo(npcs.size() + " NPCs foram carregados e gerados no mundo.");
    }

    /**
     * Gera um NPC no mundo e o adiciona ao mapa de gerenciamento.
     *
     * @param id O ID único do NPC.
     * @param name O nome de exibição.
     * @param location A localização para gerar o NPC.
     * @param texture A textura da skin.
     * @param signature A assinatura da skin.
     * @param startDialogueId O ID do diálogo inicial.
     */
    private void spawnNpc(String id, String name, Location location, String texture, String signature, String startDialogueId) {
        if (npcs.containsKey(id.toLowerCase())) {
            plugin.logWarn("Tentativa de gerar um NPC com ID duplicado: '" + id + "'. Ignorando.");
            return;
        }

        // A API do Paper permite criar NPCs de forma nativa e estável.
        // Usamos um nome temporário para evitar que o nome final apareça antes da skin ser aplicada.
        Player npcEntity = (Player) location.getWorld().spawnEntity(location, EntityType.PLAYER);
        npcEntity.setInvulnerable(true);
        npcEntity.setAI(false);
        npcEntity.setSilent(true);

        // Aplica a skin customizada usando a API de profile do Paper.
        if (texture != null && signature != null && !texture.isEmpty() && !signature.isEmpty()) {
            com.destroystokyo.paper.profile.PlayerProfile profile = npcEntity.getPlayerProfile();
            profile.getProperties().add(new ProfileProperty("textures", texture, signature));
            npcEntity.setPlayerProfile(profile);
        }

        // Define o nome de exibição customizado após a skin ser aplicada.
        npcEntity.setCustomName(name);
        npcEntity.setCustomNameVisible(true);

        // Adiciona o NPC ao nosso mapa de gerenciamento.
        Npc npcData = new Npc(id, name, location, texture, signature, startDialogueId, npcEntity.getUniqueId());
        npcs.put(id.toLowerCase(), npcData);
        entityIdToNpcId.put(npcEntity.getUniqueId(), id.toLowerCase());
    }

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

    public void deleteNpc(CommandSender sender, String id) {
        String lowerId = id.toLowerCase();
        Npc npcData = npcs.get(lowerId);

        if (npcData == null) {
            sender.sendMessage(ChatColor.RED + "Nenhum NPC encontrado com o ID '" + id + "'.");
            return;
        }

        // Remove a entidade do mundo. A busca é feita na thread principal para segurança.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Entity npcEntity = Bukkit.getEntity(npcData.entityId());
            if (npcEntity != null) {
                npcEntity.remove();
            }
        });

        // Remove dos mapas de gerenciamento
        npcs.remove(lowerId);
        entityIdToNpcId.remove(npcData.entityId());

        saveNpcs();
        sender.sendMessage(ChatColor.GREEN + "NPC com ID '" + id + "' foi removido com sucesso.");
    }

    /**
     * Inicia uma tarefa periódica que faz os NPCs olharem para o jogador mais próximo.
     * Isso adiciona um comportamento mais dinâmico e imersivo aos NPCs.
     */
    private void startLookTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Npc npcData : npcs.values()) {
                    Entity npcEntity = Bukkit.getEntity(npcData.entityId());
                    if (npcEntity == null || !npcEntity.isValid()) {
                        continue;
                    }

                    // Encontra o jogador mais próximo em um raio de 10 blocos.
                    Player closestPlayer = npcEntity.getWorld().getPlayers().stream()
                            .filter(p -> p.getLocation().distanceSquared(npcEntity.getLocation()) < 100) // 10*10 = 100 (mais eficiente que distance())
                            .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(npcEntity.getLocation())))
                            .orElse(null);

                    if (closestPlayer != null) {
                        // Faz o NPC olhar para o jogador.
                        Location npcLocation = npcEntity.getLocation();
                        npcLocation.setDirection(closestPlayer.getLocation().toVector().subtract(npcLocation.toVector()));
                        npcEntity.teleport(npcLocation);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L); // Inicia após 5s, repete a cada 1s (20 ticks).
    }
    /**
     * Retorna uma coleção de todos os NPCs gerenciados.
     *
     * @return Uma coleção não modificável dos dados dos NPCs.
     */
    public Collection<Npc> getAllNpcs() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    /**
     * Retorna os dados de um NPC com base no UUID de sua entidade no mundo.
     *
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
}