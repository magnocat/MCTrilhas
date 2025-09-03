package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.data.PlayerDataManager;
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
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor and getDescription
public class ScoutCommand implements CommandExecutor, TabCompleter {

    private final MCTrilhasPlugin plugin;
    private final PlayerDataManager playerDataManager;
    // Cache para o /scout top
    private final Map<UUID, Integer> badgeCountsCache = new HashMap<>();

    public ScoutCommand(MCTrilhasPlugin plugin) {
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
        badgeCountsCache.putAll(playerDataManager.getAllBadgeCounts_UNSAFE_SYNCHRONOUS());
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
                handleBadges(sender, args);
                break;
            case "progress":
                handleProgress(sender, args);
                break;
            case "stats":
                handleStats(sender, args);
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
                 if (!sender.hasPermission("mctrilhas.scout.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
                    return true;
                }
                plugin.reloadPluginConfig(); // Este método recarrega config.yml, badges.yml e as insígnias no BadgeManager
                rebuildCache();
                sender.sendMessage(ChatColor.GREEN + "Configurações (config.yml e badges.yml) e cache do MCTrilhas recarregados.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Comando desconhecido. Use /scout help para ver a lista de comandos.");
                break;
        }
        return true;
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos do MCTrilhas ---");
        sender.sendMessage(ChatColor.YELLOW + "/scout badges [jogador] [gui]" + ChatColor.GRAY + " - Vê as insígnias em lista ou GUI.");
        sender.sendMessage(ChatColor.YELLOW + "/scout progress [jogador]" + ChatColor.GRAY + " - Vê seu progresso para novas insígnias.");
        sender.sendMessage(ChatColor.YELLOW + "/scout toggleprogress" + ChatColor.GRAY + " - Ativa/desativa mensagens de progresso.");
        sender.sendMessage(ChatColor.YELLOW + "/scout top" + ChatColor.GRAY + " - Mostra os jogadores com mais insígnias.");
        sender.sendMessage(ChatColor.YELLOW + "/scout version" + ChatColor.GRAY + " - Mostra a versão do plugin.");
        if (sender.hasPermission("mctrilhas.scout.stats")) {
            sender.sendMessage(ChatColor.RED + "/scout stats <jogador>" + ChatColor.GRAY + " - Vê as estatísticas completas de um jogador.");
        }
        if (sender.hasPermission("mctrilhas.scout.admin")) {
            sender.sendMessage(ChatColor.RED + "/scout addbadge <jogador> <insígnia>" + ChatColor.GRAY + " - Concede uma insígnia.");
            sender.sendMessage(ChatColor.RED + "/scout removebadge <jogador> <insígnia>" + ChatColor.GRAY + " - Remove uma insígnia.");
            sender.sendMessage(ChatColor.RED + "/scout reload" + ChatColor.GRAY + " - Recarrega as configurações.");
            sender.sendMessage(ChatColor.RED + "/scout saveall" + ChatColor.GRAY + " - Salva os dados de todos e reconstrói os caches.");
        }
    }

    private void handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.GOLD + "MCTrilhas " + ChatColor.WHITE + "versão " + version);
    }

    private void handleBadges(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mctrilhas.scout.use")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }

        OfflinePlayer targetPlayer = null;
        boolean openGui = false;

        // /scout badges
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador: /scout badges <jogador>");
                return;
            }
            targetPlayer = (Player) sender;
        }
        // /scout badges <player|gui>
        else if (args.length == 2) {
            if (args[1].equalsIgnoreCase("gui")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador: /scout badges <jogador> gui");
                    return;
                }
                targetPlayer = (Player) sender;
                openGui = true;
            } else {
                targetPlayer = Bukkit.getOfflinePlayer(args[1]);
            }
        }
        // /scout badges <player> gui
        else if (args.length >= 3) {
            targetPlayer = Bukkit.getOfflinePlayer(args[1]);
            if (args[2].equalsIgnoreCase("gui")) {
                openGui = true;
            } else {
                sender.sendMessage(ChatColor.RED + "Uso: /scout badges <jogador> [gui]");
                return;
            }
        }

        if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline())) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado.");
            return;
        }

        // Verifica permissão para ver insígnias de outros
        boolean isSelf = (sender instanceof Player) && ((Player) sender).getUniqueId().equals(targetPlayer.getUniqueId());
        if (!isSelf && !sender.hasPermission("mctrilhas.scout.badges.other")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver as insígnias de outros jogadores.");
            return;
        }

        if (openGui) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "A GUI de insígnias só pode ser aberta por um jogador.");
                return;
            }
            // A classe BadgeMenu lida com o carregamento de dados do jogador alvo
            plugin.getBadgeMenu().open((Player) sender, targetPlayer.getUniqueId(), targetPlayer.getName());
        } else {
            displayBadgesInChat(sender, targetPlayer);
        }
    }

    private void displayBadgesInChat(CommandSender viewer, OfflinePlayer targetPlayer) {
        List<String> earnedBadgesIds = playerDataManager.getEarnedBadges(targetPlayer.getUniqueId());

        viewer.sendMessage(ChatColor.GOLD + "--- Insígnias de " + targetPlayer.getName() + " ---");

        if (earnedBadgesIds.isEmpty()) {
            boolean isSelf = (viewer instanceof Player) && ((Player) viewer).getUniqueId().equals(targetPlayer.getUniqueId());
            viewer.sendMessage(ChatColor.GRAY + (isSelf ? "Você ainda não conquistou" : "Este jogador não conquistou") + " nenhuma insígnia.");
            return;
        }

        for (String badgeId : earnedBadgesIds) {
            Badge badge = plugin.getBadgeManager().getBadge(badgeId);
            if (badge != null) {
                viewer.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.YELLOW + badge.getName() + ": " + ChatColor.GRAY + badge.getDescription());
            } else {
                // Fallback caso a insígnia não esteja carregada no BadgeManager mas exista nos dados do jogador
                viewer.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.YELLOW + badgeId);
            }
        }
        viewer.sendMessage(ChatColor.GOLD + "--------------------------");
        boolean isSelf = (viewer instanceof Player) && ((Player) viewer).getUniqueId().equals(targetPlayer.getUniqueId());
        viewer.sendMessage(ChatColor.AQUA + "Para uma visualização melhor, use " + ChatColor.WHITE + "/scout badges " + (isSelf ? "" : targetPlayer.getName() + " ") + "gui");
    }

    private void handleProgress(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mctrilhas.scout.use")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }

        OfflinePlayer targetPlayer;
        if (args.length > 1) {
            if (!sender.hasPermission("mctrilhas.scout.progress.other")) {
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

        PlayerData playerData = playerDataManager.getPlayerData(targetPlayer.getUniqueId());
        if (playerData == null) {
            // Tenta carregar se não estiver no cache
            playerDataManager.loadPlayerData(targetPlayer.getUniqueId());
            playerData = playerDataManager.getPlayerData(targetPlayer.getUniqueId());
            if (playerData == null) {
                sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de progresso para " + targetPlayer.getName() + ".");
                return;
            }
        }

        // A variável precisa ser final ou efetivamente final para ser usada na expressão lambda abaixo.
        final PlayerData finalPlayerData = playerData;

        List<Badge> unearnedBadges = plugin.getBadgeManager().getAllBadges().stream()
                .filter(badge -> !finalPlayerData.hasBadge(badge.getId()))
                .collect(Collectors.toList());

        if (unearnedBadges.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "Parabéns! " + (targetPlayer.getUniqueId().equals(sender instanceof Player ? ((Player) sender).getUniqueId() : null) ? "Você" : targetPlayer.getName()) + " já conquistou todas as insígnias!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + targetPlayer.getName() + " ---");
        String format = plugin.getConfig().getString("progress-message-format", "&e{badgeName}: &a{progress}&8/&7{required} &b({percentage}%)");

        for (Badge badge : unearnedBadges) {
            double currentProgress = finalPlayerData.getProgress(badge.getType());
            double requiredProgress = badge.getRequirement();
            if (requiredProgress <= 0) continue;

            double percentage = Math.min(100.0, (currentProgress * 100.0) / requiredProgress);

            String message = format.replace("{badgeName}", badge.getName())
                                   .replace("{progress}", String.valueOf((long) currentProgress))
                                   .replace("{required}", String.valueOf((long) requiredProgress))
                                   .replace("{percentage}", String.format("%.1f", percentage));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mctrilhas.scout.stats")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /scout stats <jogador>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado: " + args[1]);
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Buscando estatísticas de " + target.getName() + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                plugin.getPlayerDataManager().loadPlayerData(target.getUniqueId());
                playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            }

            if (playerData == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados para " + target.getName() + "."));
                return;
            }

            final PlayerData finalPlayerData = playerData;
            final int totalBadges = plugin.getBadgeManager().getAllBadges().size();
            final NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Estatísticas de " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " ---");
                sender.sendMessage(ChatColor.YELLOW + "Insígnias: " + ChatColor.AQUA + finalPlayerData.getEarnedBadges().size() + " / " + totalBadges);
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.GOLD + "--- Progresso Total (Vida Inteira) ---");

                for (BadgeType type : BadgeType.values()) {
                    double progress = finalPlayerData.getProgress(type);
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + type.getName() + ": " + ChatColor.GREEN + numberFormat.format(progress));
                }
                sender.sendMessage(ChatColor.GOLD + "------------------------------------");
            });
        });
    }

    private void handleAddBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mctrilhas.scout.admin")) {
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

        // Carrega os dados do jogador, mesmo que offline, para modificação.
        PlayerData playerData = getPlayerDataForModification(sender, target);
        if (playerData == null) {
            return; // Mensagem de erro já foi enviada pelo método helper.
        }

        if (playerData.hasBadge(badgeId)) {
            sender.sendMessage(ChatColor.YELLOW + "O jogador já possui a insígnia '" + badgeId + "'.");
        } else {
            // Modifica a lista de insígnias diretamente no objeto de dados.
            playerData.getEarnedBadges().add(badgeId);
            // Atualiza o cache local
            badgeCountsCache.merge(target.getUniqueId(), 1, Integer::sum);
            sender.sendMessage(ChatColor.GREEN + "Insígnia '" + badgeId + "' adicionada para " + target.getName() + ".");

            // Notifica o jogador se ele estiver online
            if (target.isOnline()) {
                ((Player) target).sendMessage(ChatColor.GOLD + "Você recebeu a insígnia: " + badgeId);
            }
        }

        // Se o jogador estiver offline, salva e descarrega os dados para persistir a mudança.
        if (!target.isOnline()) {
            playerDataManager.unloadPlayerData(target.getUniqueId());
        }
    }

    private void handleRemoveBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mctrilhas.scout.admin")) {
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

        // Carrega os dados do jogador, mesmo que offline, para modificação.
        PlayerData playerData = getPlayerDataForModification(sender, target);
        if (playerData == null) {
            return; // Mensagem de erro já foi enviada pelo método helper.
        }

        if (playerData.hasBadge(badgeId)) {
            // Modifica a lista de insígnias diretamente no objeto de dados.
            playerData.getEarnedBadges().remove(badgeId);
            // Atualiza o cache local
            badgeCountsCache.merge(target.getUniqueId(), -1, Integer::sum);
            sender.sendMessage(ChatColor.GREEN + "Insígnia '" + badgeId + "' removida de " + target.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "O jogador não possui a insígnia '" + badgeId + "'.");
        }

        // Se o jogador estiver offline, salva e descarrega os dados para persistir a mudança.
        if (!target.isOnline()) {
            playerDataManager.unloadPlayerData(target.getUniqueId());
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
        if (!sender.hasPermission("mctrilhas.scout.admin")) {
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
        if (!sender.hasPermission("mctrilhas.scout.use")) {
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

    /**
     * Um método helper para obter os dados de um jogador, carregando-os do disco se necessário.
     * Essencial para modificar dados de jogadores offline.
     * @param sender O autor do comando, para enviar mensagens de erro.
     * @param target O jogador alvo.
     * @return O objeto PlayerData do alvo, ou null se não puder ser carregado.
     */
    private PlayerData getPlayerDataForModification(CommandSender sender, OfflinePlayer target) {
        UUID targetUUID = target.getUniqueId();
        PlayerData playerData = playerDataManager.getPlayerData(targetUUID);
        if (playerData == null) {
            playerDataManager.loadPlayerData(targetUUID);
            playerData = playerDataManager.getPlayerData(targetUUID);
            if (playerData == null) {
                sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados do jogador " + target.getName() + ".");
                return null;
            }
        }
        return playerData;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("help", "version", "badges", "progress", "top", "toggleprogress"));
            if (sender.hasPermission("mctrilhas.scout.stats")) {
                subcommands.add("stats");
            }
            if (sender.hasPermission("mctrilhas.scout.admin")) {
                subcommands.addAll(Arrays.asList("addbadge", "removebadge", "reload", "saveall"));
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        String subCommand = args[0].toLowerCase();

        if (args.length == 2 && (subCommand.equals("addbadge") || subCommand.equals("removebadge") || subCommand.equals("progress") || subCommand.equals("stats"))) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }

        if (args.length == 2 && subCommand.equals("badges")) {
            List<String> completions = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            if ("gui".startsWith(args[1].toLowerCase())) {
                completions.add("gui");
            }
            return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
        }

        if (args.length == 3 && (subCommand.equals("addbadge") || subCommand.equals("removebadge"))) {
            var badgeConfig = plugin.getBadgeConfigManager().getBadgeConfig();
            Set<String> badgeIds = badgeConfig.getKeys(false);
            return StringUtil.copyPartialMatches(args[2], badgeIds, new ArrayList<>());
        }

        if (args.length == 3 && subCommand.equals("badges")) {
            if (Bukkit.getOfflinePlayer(args[1]).hasPlayedBefore() && "gui".startsWith(args[2].toLowerCase())) {
                return Collections.singletonList("gui");
            }
        }

        return Collections.emptyList();
    }
}