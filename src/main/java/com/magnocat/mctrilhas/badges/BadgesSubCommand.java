package com.magnocat.mctrilhas.badges;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementa o subcomando de jogador `/scout badges [jogador]`.
 * <p>
 * Este comando exibe as insígnias conquistadas por um jogador. Ele pode
 * abrir uma interface gráfica (GUI) se configurado, ou exibir uma lista
 * no chat, lidando corretamente com jogadores online e offline.
 */
@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public class BadgesSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public BadgesSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "badges";
    }

    @Override
    public String getDescription() {
        return "Exibe as insígnias conquistadas.";
    }

    @Override
    public String getSyntax() {
        return "/scout badges [jogador]";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.use";
    }

    @Override
    public boolean isAdminCommand() { return false; }

    /**
     * Verifica se o módulo de Menu de Insígnias está ativo.
     */
    @Override
    public boolean isModuleEnabled(MCTrilhasPlugin plugin) { return plugin.getBadgeMenu() != null; }

    /**
     * Executa a lógica para exibir as insígnias de um jogador.
     *
     * @param sender A entidade que executou o comando.
     * @param args Os argumentos fornecidos. Pode conter o nome de um jogador alvo.
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        OfflinePlayer target;
        if (args.length == 1) {
            if (!sender.hasPermission("mctrilhas.progress.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver as insígnias de outros jogadores.");
                return;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[0] + "' nunca foi visto neste servidor.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador. Uso: /scout badges <jogador>");
                return;
            }
            target = (Player) sender;
        }

        boolean useGUI = plugin.getConfig().getBoolean("badges.use-gui", false);

        // A GUI só pode ser aberta para jogadores online e por um jogador.
        if (useGUI && sender instanceof Player && target.isOnline()) {
            // Abre a GUI para o jogador alvo, com o remetente como visualizador.
            plugin.getBadgeMenu().open((Player) sender, target.getUniqueId(), target.getName(), 1); // Abre a primeira página por padrão.
        } else {
            // Se a GUI não for usada, ou o alvo estiver offline, ou o remetente for o console, mostra no chat.
            if (useGUI && !(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console não pode abrir a GUI. As insígnias serão mostradas no chat.");
            } else if (useGUI && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Não é possível abrir a GUI para um jogador offline. As insígnias serão mostradas no chat.");
            }
            showBadgesInChat(sender, target);
        }
    }

    /**
     * Busca os dados de um jogador (online ou offline) e exibe suas insígnias no chat.
     * A busca de dados é feita de forma assíncrona para não travar o servidor.
     *
     * @param sender A entidade que receberá a mensagem.
     * @param target O jogador cujas insígnias serão exibidas.
     */
    private void showBadgesInChat(CommandSender sender, OfflinePlayer target) {
        String targetName = Objects.toString(target.getName(), "Desconhecido");
        sender.sendMessage(ChatColor.YELLOW + "Buscando insígnias de " + targetName + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Tenta obter dados do cache (se online) ou carrega do arquivo (se offline).
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(target.getUniqueId());
            }

            if (playerData == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de " + targetName + "."));
                return;
            }

            // CORREÇÃO: Usa getEarnedBadgesMap().keySet() para obter a lista de IDs de insígnias.
            final List<String> earnedBadges = new ArrayList<>(playerData.getEarnedBadgesMap().keySet());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (earnedBadges.isEmpty()) {
                    String message = (sender.getName().equals(targetName))
                            ? "Você ainda não conquistou nenhuma insígnia. Continue se esforçando!"
                            : targetName + " ainda não conquistou nenhuma insígnia.";
                    sender.sendMessage(ChatColor.YELLOW + message);
                    return;
                }

                sender.sendMessage(ChatColor.GOLD + "--- Insígnias de " + targetName + " ---");
                earnedBadges.forEach(badgeId -> {
                    Badge badge = plugin.getBadgeManager().getBadge(badgeId);
                    if (badge != null) {
                        sender.sendMessage(ChatColor.AQUA + "- " + badge.name() + ": " + ChatColor.GRAY + badge.description());
                    }
                });
            });
        });
    }

    /**
     * Fornece sugestões de autocompletar para o comando.
     *
     * @param sender A entidade que está tentando autocompletar o comando.
     * @param args Os argumentos atuais digitados pelo remetente.
     * @return Uma lista de sugestões para o próximo argumento.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("mctrilhas.progress.other")) {
            return null; // Usa o completador padrão do Bukkit para nomes de jogadores
        }
        return Collections.emptyList();
    }
}