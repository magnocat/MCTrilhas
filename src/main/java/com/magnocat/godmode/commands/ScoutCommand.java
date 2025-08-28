package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor and getDescription
public class ScoutCommand implements CommandExecutor, TabCompleter {

    private final GodModePlugin plugin;
    private final PlayerDataManager playerDataManager;
    // Cache para o /scout top
    private final Map<UUID, Integer> badgeCountsCache = new HashMap<>();

    public ScoutCommand(GodModePlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        // Carrega o cache quando o plugin inicia
        rebuildCache();
    }

    /**
     * Reconstrói o cache de contagem de insígnias a partir dos arquivos dos jogadores.
     * Essencial para a performance do /scout top.
     */
    public void rebuildCache() {
        badgeCountsCache.clear();
        // Popula o cache a partir do PlayerDataManager, que já tem os dados carregados ou sabe como buscá-los.
        badgeCountsCache.putAll(playerDataManager.getAllBadgeCounts());
        plugin.getLogger().info("Cache do /scout top reconstruído com " + badgeCountsCache.size() + " jogadores.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            handleHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                handleHelp(sender);
                break;
            case "version":
                handleVersion(sender);
                break;
            case "badges":
                handleBadges(sender);
                break;
            case "progress":
                handleProgress(sender, args);
                break;
            case "top":
                handleTop(sender);
                break;
            case "toggleprogress":
                handleToggleProgress(sender);
                break;
            case "addbadge":
                handleAddBadge(sender, args);
                break;
            case "removebadge":
                handleRemoveBadge(sender, args);
                break;
            case "saveall":
                handleSaveAll(sender);
                break;
            case "reload":
                 if (!sender.hasPermission("godmode.scout.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
                    return true;
                }
                plugin.reloadPluginConfig(); // Este método recarrega config.yml, badges.yml e as insígnias no BadgeManager
                rebuildCache();
                sender.sendMessage(ChatColor.GREEN + "Configurações (config.yml e badges.yml) e cache do GodMode-MCTrilhas recarregados.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Comando desconhecido. Use /scout help para ver a lista de comandos.");
                break;
        }
        return true;
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos do GodMode-MCTrilhas ---");
        sender.sendMessage(ChatColor.YELLOW + "/scout badges" + ChatColor.GRAY + " - Vê suas insígnias.");
        sender.sendMessage(ChatColor.YELLOW + "/scout progress [jogador]" + ChatColor.GRAY + " - Vê seu progresso para novas insígnias.");
        sender.sendMessage(ChatColor.YELLOW + "/scout toggleprogress" + ChatColor.GRAY + " - Ativa/desativa mensagens de progresso.");
        sender.sendMessage(ChatColor.YELLOW + "/scout top" + ChatColor.GRAY + " - Mostra os jogadores com mais insígnias.");
        sender.sendMessage(ChatColor.YELLOW + "/scout version" + ChatColor.GRAY + " - Mostra a versão do plugin.");
        if (sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "/scout addbadge <jogador> <insígnia>" + ChatColor.GRAY + " - Concede uma insígnia.");
            sender.sendMessage(ChatColor.RED + "/scout removebadge <jogador> <insígnia>" + ChatColor.GRAY + " - Remove uma insígnia.");
            sender.sendMessage(ChatColor.RED + "/scout reload" + ChatColor.GRAY + " - Recarrega as configurações.");
            sender.sendMessage(ChatColor.RED + "/scout saveall" + ChatColor.GRAY + " - Salva os dados de todos e reconstrói os caches.");
        }
    }

    private void handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.GOLD + "GodMode-MCTrilhas " + ChatColor.WHITE + "versão " + version);
    }

    private void handleBadges(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        Player player = (Player) sender;
        List<String> earnedBadges = playerDataManager.getEarnedBadges(player.getUniqueId());

        if (earnedBadges.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Você ainda não conquistou nenhuma insígnia. Continue explorando!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "--- Suas Insígnias Conquistadas ---");
        for (String badgeId : earnedBadges) {
            var badgeConfig = plugin.getBadgeConfigManager().getBadgeConfig();
            String badgeName = badgeConfig.getString(badgeId + ".name", badgeId);
            String description = badgeConfig.getString(badgeId + ".description", "Sem descrição.");
            player.sendMessage(ChatColor.GREEN + "- " + badgeName + ": " + ChatColor.GRAY + description);
        }
    }

    private void handleProgress(CommandSender sender, String[] args) {
        if (!sender.hasPermission("godmode.scout.use")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }

        OfflinePlayer targetPlayer;
        if (args.length > 1) {
            if (!sender.hasPermission("godmode.scout.progress.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver o progresso de outros jogadores.");
                return;
            }
            targetPlayer = Bukkit.getOfflinePlayer(args[1]);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[1] + "' não encontrado.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador: /scout progress <jogador>");
                return;
            }
            targetPlayer = (Player) sender;
        }

        var badgeConfig = plugin.getBadgeConfigManager().getBadgeConfig();
        List<String> earnedBadges = playerDataManager.getEarnedBadges(targetPlayer.getUniqueId());
        Set<String> allBadgeIds = badgeConfig.getKeys(false);
        allBadgeIds.removeAll(earnedBadges);

        if (allBadgeIds.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "Parabéns! " + (targetPlayer.getUniqueId().equals(sender instanceof Player ? ((Player) sender).getUniqueId() : null) ? "Você" : targetPlayer.getName()) + " já conquistou todas as insígnias!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + targetPlayer.getName() + " ---");
        String format = plugin.getConfig().getString("progress-message-format", "&e{badgeName}: &a{progress}&8/&7{required} &b({percentage}%)");

        allBadgeIds.forEach(badgeId -> {
            long currentProgress = playerDataManager.getProgress(targetPlayer.getUniqueId(), badgeId);
            long requiredProgress = badgeConfig.getLong(badgeId + ".required-progress", -1);
            if (requiredProgress <= 0) return;

            String badgeName = badgeConfig.getString(badgeId + ".name", badgeId);
            double percentage = (currentProgress * 100.0) / requiredProgress;

            String message = format.replace("{badgeName}", badgeName)
                                   .replace("{progress}", String.valueOf(currentProgress))
                                   .replace("{required}", String.valueOf(requiredProgress))
                                   .replace("{percentage}", String.format("%.1f", percentage));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        });
    }

    private void handleAddBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /scout addbadge <jogador> <badgeId>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Jogador '" + args[1] + "' não encontrado.");
            return;
        }

        String badgeId = args[2].toLowerCase();
        // Validação: Checa se a insígnia existe no badges.yml
        if (!plugin.getBadgeConfigManager().getBadgeConfig().contains(badgeId)) {
            sender.sendMessage(ChatColor.RED + "A insígnia '" + badgeId + "' não existe. Verifique o badges.yml.");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        // Usa o PlayerDataManager para verificar e adicionar a insígnia
        if (playerDataManager.hasBadge(targetUUID, badgeId)) {
            sender.sendMessage(ChatColor.YELLOW + "O jogador já possui a insígnia '" + badgeId + "'.");
        } else {
            playerDataManager.addBadge(targetUUID, badgeId);
            // Atualiza o cache local
            badgeCountsCache.merge(targetUUID, 1, Integer::sum);
            sender.sendMessage(ChatColor.GREEN + "Insígnia '" + badgeId + "' adicionada para " + target.getName() + ".");

            // Notifica o jogador se ele estiver online
            if (target.isOnline()) {
                ((Player) target).sendMessage(ChatColor.GOLD + "Você recebeu a insígnia: " + badgeId);
            }
        }
    }

    private void handleRemoveBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /scout removebadge <jogador> <badgeId>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
         if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Jogador '" + args[1] + "' não encontrado.");
            return;
        }

        String badgeId = args[2].toLowerCase();
        UUID targetUUID = target.getUniqueId();

        // Usa o PlayerDataManager
        if (playerDataManager.hasBadge(targetUUID, badgeId)) {
            playerDataManager.removeBadge(targetUUID, badgeId);
            // Atualiza o cache local
            badgeCountsCache.merge(targetUUID, -1, Integer::sum);
            sender.sendMessage(ChatColor.GREEN + "Insígnia '" + badgeId + "' removida de " + target.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "O jogador não possui a insígnia '" + badgeId + "'.");
        }
    }

    private void handleToggleProgress(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        Player player = (Player) sender;

        // A lógica de toggle e persistência é delegada ao PlayerDataManager
        boolean isDisabled = playerDataManager.toggleProgressMessages(player.getUniqueId());

        if (isDisabled) {
            player.sendMessage(ChatColor.YELLOW + "Mensagens de progresso de insígnias desativadas.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Mensagens de progresso de insígnias ativadas.");
        }
    }

    private void handleSaveAll(CommandSender sender) {
        if (!sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Iniciando salvamento de dados e reconstrução de caches...");

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        int playerCount = onlinePlayers.size();

        // Salva os dados dos jogadores online (e os remove do cache principal)
        for (Player player : onlinePlayers) {
            playerDataManager.unloadPlayerData(player.getUniqueId());
        }

        // Recarrega os dados dos jogadores online para o cache principal
        for (Player player : onlinePlayers) {
            playerDataManager.loadPlayerData(player.getUniqueId());
        }
        sender.sendMessage(ChatColor.GREEN + "Dados de " + playerCount + " jogador(es) online salvos com sucesso.");

        // Reconstrói o cache específico do /scout top
        rebuildCache();
        sender.sendMessage(ChatColor.GREEN + "Todos os dados foram salvos e os caches reconstruídos.");
    }

    private void handleTop(CommandSender sender) {
        if (!sender.hasPermission("godmode.scout.use")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Top 10 Jogadores com Mais Insígnias ---");

        if (badgeCountsCache.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhum jogador com insígnias encontrado.");
            return;
        }

        // Ordena o cache em memória, sem ler arquivos
        List<Map.Entry<UUID, Integer>> sortedList = badgeCountsCache.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (int i = 0; i < sortedList.size(); i++) {
            Map.Entry<UUID, Integer> entry = sortedList.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = player.getName() != null ? player.getName() : "Desconhecido";
            sender.sendMessage(ChatColor.YELLOW + "" + (i + 1) + ". " + playerName + " - " + entry.getValue() + " insígnias");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("help", "version", "badges", "progress", "top", "toggleprogress"));
            if (sender.hasPermission("godmode.scout.admin")) {
                subcommands.addAll(Arrays.asList("addbadge", "removebadge", "reload", "saveall"));
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("addbadge") || args[0].equalsIgnoreCase("removebadge") || args[0].equalsIgnoreCase("progress"))) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("addbadge") || args[0].equalsIgnoreCase("removebadge"))) {
            var badgeConfig = plugin.getBadgeConfigManager().getBadgeConfig();
            Set<String> badgeIds = badgeConfig.getKeys(false);
            return StringUtil.copyPartialMatches(args[2], badgeIds, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}