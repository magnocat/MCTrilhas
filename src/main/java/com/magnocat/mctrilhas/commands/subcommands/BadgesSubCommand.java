package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

        if (useGUI) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console não pode abrir a GUI. As insígnias serão mostradas no chat.");
                showBadgesInChat(sender, target);
                return;
            }
            if (!target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Não é possível abrir a GUI para um jogador offline. As insígnias serão mostradas no chat.");
                showBadgesInChat(sender, target);
                return;
            }
            // Abre a GUI para o jogador alvo, com o remetente como visualizador.
            plugin.getBadgeMenu().open((Player) sender, target.getUniqueId(), target.getName());
        } else {
            showBadgesInChat(sender, target);
        }
    }

    private void showBadgesInChat(CommandSender sender, OfflinePlayer target) {
        String targetName = Objects.toString(target.getName(), "Desconhecido");
        sender.sendMessage(ChatColor.YELLOW + "Buscando insígnias de " + targetName + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                plugin.getPlayerDataManager().loadPlayerData(target.getUniqueId());
                playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
                if (playerData == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de " + targetName + "."));
                    return;
                }
            }

            final List<String> earnedBadges = playerData.getEarnedBadges();

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
                    String configKey = plugin.getBadgeConfigManager().getBadgeConfigKey(badgeId);
                    if (configKey == null) return;
                    String badgeName = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + configKey + ".name", configKey);
                    String description = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + configKey + ".description", "Sem descrição.");
                    sender.sendMessage(ChatColor.AQUA + "- " + badgeName + ": " + ChatColor.GRAY + description);
                });
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("mctrilhas.progress.other")) {
            return null; // Usa o completador padrão do Bukkit para nomes de jogadores
        }
        return Collections.emptyList();
    }
}