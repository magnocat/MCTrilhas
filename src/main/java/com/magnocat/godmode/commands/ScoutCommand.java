package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

public class ScoutCommand implements CommandExecutor {

    private final GodModePlugin plugin;

    public ScoutCommand(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "badges":
                return handleBadges(sender);
            case "progress":
                return handleProgress(sender, args);
            case "top":
                return handleTop(sender);
            case "toggleprogress":
                return handleToggleProgress(sender);
            case "addbadge":
                return handleAddBadge(sender, args);
            case "removebadge":
                return handleRemoveBadge(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos de Escoteiro ---");
        if (sender.hasPermission("godmode.scout.use")) {
            sender.sendMessage(ChatColor.AQUA + "/scout badges" + ChatColor.GRAY + " - Exibe suas insígnias.");
            sender.sendMessage(ChatColor.AQUA + "/scout progress [jogador]" + ChatColor.GRAY + " - Mostra seu progresso ou de outro jogador.");
            sender.sendMessage(ChatColor.AQUA + "/scout top" + ChatColor.GRAY + " - Mostra os jogadores com mais insígnias.");
            sender.sendMessage(ChatColor.AQUA + "/scout toggleprogress" + ChatColor.GRAY + " - Ativa/desativa mensagens de progresso.");
        }
        if (sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "/scout addbadge <jogador> <badgeId>" + ChatColor.GRAY + " - (Admin) Concede uma insígnia.");
            sender.sendMessage(ChatColor.RED + "/scout removebadge <jogador> <badgeId>" + ChatColor.GRAY + " - Remove uma insígnia.");
        }
    }

    private boolean handleBadges(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;
        List<String> earnedBadges = plugin.getPlayerDataManager().getEarnedBadges(player);

        if (earnedBadges.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Você ainda não conquistou nenhuma insígnia. Continue se esforçando!");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "--- Suas Insígnias Conquistadas ---");
        for (String badgeId : earnedBadges) {
            // Busca o nome da insígnia no config.yml para uma exibição mais amigável
            String badgeName = plugin.getConfig().getString("badges." + badgeId + ".name", badgeId); // Usa o ID como fallback
            player.sendMessage(ChatColor.AQUA + "- " + badgeName);
        }
        return true;
    }

    private boolean handleProgress(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /scout progress [jogador]");
            return true;
        }

        OfflinePlayer target;
        if (args.length == 2) {
            // Admin checking another player's progress
            if (!sender.hasPermission("godmode.scout.progress.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver o progresso de outros jogadores.");
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[1] + "' nunca foi visto neste servidor.");
                return true;
            }
        } else {
            // Player checking their own progress
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador. Uso: /scout progress <jogador>");
                return true;
            }
            target = (Player) sender;
        }

        displayProgressFor(sender, target);
        return true;
    }

    private void displayProgressFor(CommandSender sender, OfflinePlayer target) {
        String targetName = target.getName() != null ? target.getName() : "Desconhecido";
        sender.sendMessage(ChatColor.YELLOW + "Calculando progresso para " + targetName + "...");

        // Run asynchronously to avoid blocking the main server thread with file I/O.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> earnedBadges = plugin.getPlayerDataManager().getEarnedBadges(target.getUniqueId());
            Set<String> allBadgeIds = plugin.getConfig().getConfigurationSection("badges").getKeys(false);

            List<String> progressMessages = new ArrayList<>();
            for (String badgeId : allBadgeIds) {
                if (!earnedBadges.contains(badgeId)) {
                    String badgeName = plugin.getConfig().getString("badges." + badgeId + ".name", badgeId);
                    int currentProgress = plugin.getPlayerDataManager().getProgress(target.getUniqueId(), badgeId);
                    int requiredProgress = plugin.getConfig().getInt("badges." + badgeId + ".required-progress", 1);

                    String progressBar = buildProgressBar(currentProgress, requiredProgress);
                    String message = ChatColor.YELLOW + badgeName + ": " + ChatColor.AQUA + currentProgress + "/" + requiredProgress + " " + progressBar;
                    progressMessages.add(message);
                }
            }

            // Schedule the message sending back to the main thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (progressMessages.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "Parabéns! " + targetName + " já conquistou todas as insígnias disponíveis!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + targetName + " ---");
                    progressMessages.forEach(sender::sendMessage);
                }
            });
        });
    }

    private boolean handleToggleProgress(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        Player player = (Player) sender;
        boolean currentStatus = plugin.getPlayerDataManager().areProgressMessagesEnabled(player);
        boolean newStatus = !currentStatus;
        plugin.getPlayerDataManager().setProgressMessagesEnabled(player, newStatus);

        if (newStatus) {
            player.sendMessage(ChatColor.GREEN + "Mensagens de progresso de insígnia ativadas.");
        } else {
            player.sendMessage(ChatColor.RED + "Mensagens de progresso de insígnia desativadas.");
        }
        return true;
    }

    private String buildProgressBar(int current, int max) {
        if (max <= 0) max = 1; // Prevent division by zero
        double percentage = (double) current / max;
        if (percentage > 1.0) percentage = 1.0;

        int totalBars = 10;
        int progressBars = (int) (totalBars * percentage);

        return ChatColor.GREEN + "■".repeat(progressBars) + ChatColor.GRAY + "■".repeat(totalBars - progressBars);
    }

    private boolean handleAddBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /scout addbadge <jogador> <badgeId>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "O jogador '" + args[1] + "' não está online.");
            return true;
        }

        String badgeId = args[2].toLowerCase();

        // A lógica de premiação e verificação está no BadgeManager
        plugin.getBadgeManager().awardBadge(targetPlayer, badgeId);

        sender.sendMessage(ChatColor.GREEN + "Tentativa de conceder a insígnia '" + badgeId + "' para " + targetPlayer.getName() + ".");
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Calculando o ranking de escoteiros...");

        // Run asynchronously to avoid blocking the main server thread with file I/O.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File dataFolder = new File(plugin.getDataFolder(), "playerdata");
            File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles == null || playerFiles.length == 0) {
                sender.sendMessage(ChatColor.RED + "Nenhum dado de jogador encontrado para gerar o ranking.");
                return;
            }

            Map<UUID, Integer> badgeCounts = new HashMap<>();
            for (File playerFile : playerFiles) {
                try {
                    String uuidString = playerFile.getName().replace(".yml", "");
                    UUID uuid = UUID.fromString(uuidString);
                    FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                    int count = playerConfig.getStringList("badges").size();
                    if (count > 0) {
                        badgeCounts.put(uuid, count);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid UUID in filename, skip.
                }
            }

            // Sort the map by value (badge count) in descending order.
            List<Map.Entry<UUID, Integer>> sortedList = new ArrayList<>(badgeCounts.entrySet());
            sortedList.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());

            // Schedule the message sending back to the main thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Top 10 Escoteiros ---");
                int rank = 1;
                for (Map.Entry<UUID, Integer> entry : sortedList) {
                    if (rank > 10) {
                        break;
                    }
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                    String playerName = player.getName() != null ? player.getName() : "Desconhecido";
                    int count = entry.getValue();
                    String badgeText = count == 1 ? "insígnia" : "insígnias";
                    sender.sendMessage(ChatColor.AQUA + "" + rank + ". " + ChatColor.WHITE + playerName + ChatColor.GRAY + " - " + ChatColor.YELLOW + count + " " + badgeText);
                    rank++;
                }
                if (sortedList.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "Ninguém conquistou insígnias ainda.");
                }
            });
        });

        return true;
    }

    private boolean handleRemoveBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /scout removebadge <jogador> <badgeId>");
            return true;
        }

        String badgeId = args[2].toLowerCase();

        // Valida se a insígnia existe no config.yml
        if (plugin.getConfig().getConfigurationSection("badges." + badgeId) == null) {
            sender.sendMessage(ChatColor.RED + "A insígnia '" + badgeId + "' não existe.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "O jogador '" + args[1] + "' não está online.");
            return true;
        }

        plugin.getPlayerDataManager().removeBadge(targetPlayer, badgeId);
        sender.sendMessage(ChatColor.GREEN + "A insígnia '" + badgeId + "' foi removida de " + targetPlayer.getName() + ".");
        targetPlayer.sendMessage(ChatColor.YELLOW + "Sua insígnia '" + plugin.getConfig().getString("badges." + badgeId + ".name", badgeId) + "' foi removida por um administrador.");
        return true;
    }
}