package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia todo o sistema de duelos 1v1.
 * <p>
 * Responsabilidades:
 * <ul>
 *     <li>Carregar e gerenciar as arenas de duelo.</li>
 *     <li>Processar desafios entre jogadores.</li>
 *     <li>Iniciar e finalizar partidas de duelo.</li>
 *     <li>Manter uma lista de jogos ativos.</li>
 * </ul>
 */
public class DuelManager {
    private final MCTrilhasPlugin plugin;
    private final List<DuelArena> loadedArenas = new ArrayList<>();
    private final Map<UUID, Challenge> pendingChallenges = new ConcurrentHashMap<>();
    private File arenaConfigFile;
    private FileConfiguration arenaConfig;

    private static final long CHALLENGE_TIMEOUT_SECONDS = 60;

    public DuelManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carrega as arenas do arquivo duel_arenas.yml.
     * Este método deve ser chamado no onEnable do plugin.
     */
    public void loadArenas() {
        createArenaConfigFile();

        loadedArenas.clear();

        ConfigurationSection arenasSection = arenaConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.logInfo("[Duels] Nenhuma arena configurada em duel_arenas.yml.");
            return;
        }

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection currentArenaSection = arenasSection.getConfigurationSection(arenaId);
            DuelArena arena = DuelArena.fromConfig(arenaId, currentArenaSection);
            if (arena != null) {
                loadedArenas.add(arena);
            }
        }
        plugin.logInfo("[Duels] " + loadedArenas.size() + " arena(s) de duelo carregada(s).");
    }

    /**
     * Envia um desafio de duelo de um jogador para outro.
     * @param challenger O jogador que está desafiando.
     * @param target O jogador que está sendo desafiado.
     */
    public void sendChallenge(Player challenger, Player target) {
        if (challenger.equals(target)) {
            challenger.sendMessage(ChatColor.RED + "Você não pode desafiar a si mesmo!");
            return;
        }

        if (pendingChallenges.containsKey(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " já tem um desafio pendente.");
            return;
        }

        // Verifica se o desafiante já enviou um desafio para outra pessoa
        for (Challenge existingChallenge : pendingChallenges.values()) {
            if (existingChallenge.getChallenger().equals(challenger)) {
                challenger.sendMessage(ChatColor.RED + "Você já enviou um desafio. Aguarde a resposta ou o desafio expirar.");
                return;
            }
        }

        Challenge challenge = new Challenge(challenger, target);
        pendingChallenges.put(target.getUniqueId(), challenge);

        challenger.sendMessage(ChatColor.GREEN + "Você desafiou " + target.getName() + " para um duelo! Aguardando resposta...");

        // Mensagem para o alvo com botões clicáveis
        target.sendMessage(ChatColor.GOLD + "---------------------------------");
        target.sendMessage(ChatColor.YELLOW + challenger.getName() + " te desafiou para um duelo!");

        TextComponent acceptComponent = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "[ACEITAR]");
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo aceitar " + challenger.getName()));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GREEN + "Clique para aceitar o duelo").create()));

        TextComponent denyComponent = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "[NEGAR]");
        denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo negar " + challenger.getName()));
        denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.RED + "Clique para negar o duelo").create()));

        TextComponent message = new TextComponent(" ");
        message.addExtra(acceptComponent);
        message.addExtra("  ");
        message.addExtra(denyComponent);

        target.spigot().sendMessage(message);
        target.sendMessage(ChatColor.GRAY + "O desafio expira em " + CHALLENGE_TIMEOUT_SECONDS + " segundos.");
        target.sendMessage(ChatColor.GOLD + "---------------------------------");
    }

    private void createArenaConfigFile() {
        arenaConfigFile = new File(plugin.getDataFolder(), "duel_arenas.yml");
        if (!arenaConfigFile.exists()) {
            plugin.saveResource("duel_arenas.yml", false);
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);
    }
}