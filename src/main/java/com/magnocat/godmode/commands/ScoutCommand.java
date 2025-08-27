package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScoutCommand implements CommandExecutor, TabCompleter {

    private final GodModePlugin plugin;
    // Cache para o /scout top
    private final Map<UUID, Integer> badgeCountsCache = new HashMap<>();
    // Set para o /scout toggleprogress
    private final Set<UUID> progressToggledOff = new HashSet<>();

    public ScoutCommand(GodModePlugin plugin) {
        this.plugin = plugin;
        // Carrega o cache quando o plugin inicia
        rebuildCache();
    }

    /**
     * Reconstrói o cache de contagem de insígnias a partir dos arquivos dos jogadores.
     * Essencial para a performance do /scout top.
     */
    public void rebuildCache() {
        badgeCountsCache.clear();
        File dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (playerFiles == null) return;

        for (File playerFile : playerFiles) {
            try {
                UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                int count = playerData.getStringList("badges").size();
                badgeCountsCache.put(uuid, count);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
            }
        }
        plugin.getLogger().info("Cache do /scout top reconstruído com " + badgeCountsCache.size() + " jogadores.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Enviar uma mensagem de ajuda ou versão
            sender.sendMessage(ChatColor.GOLD + "Plugin GodMode-MCTrilhas. Use /scout help para ver os comandos.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "addbadge":
                handleAddBadge(sender, args);
                break;
            case "removebadge":
                handleRemoveBadge(sender, args);
                break;
            case "toggleprogress":
                handleToggleProgress(sender);
                break;
            case "top":
                handleTop(sender);
                break;
            case "reload":
                 if (!sender.hasPermission("godmode.scout.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
                    return true;
                }
                plugin.reloadConfig();
                rebuildCache();
                sender.sendMessage(ChatColor.GREEN + "Configuração e cache do GodMode-MCTrilhas recarregados.");
                break;
            // Adicione aqui os outros comandos (badges, progress, version)
            default:
                sender.sendMessage(ChatColor.RED + "Comando desconhecido. Use /scout help.");
                break;
        }
        return true;
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
        // Validação: Checa se a insígnia existe no config.yml
        if (plugin.getConfig().getConfigurationSection("badges." + badgeId) == null) {
            sender.sendMessage(ChatColor.RED + "A insígnia '" + badgeId + "' não existe. Verifique o config.yml.");
            return;
        }

        File playerFile = new File(plugin.getDataFolder(), "playerdata/" + target.getUniqueId() + ".yml");
        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);

        List<String> badges = playerData.getStringList("badges");
        if (!badges.contains(badgeId)) {
            badges.add(badgeId);
            playerData.set("badges", badges);
            try {
                playerData.save(playerFile);
                // Atualiza o cache
                badgeCountsCache.merge(target.getUniqueId(), 1, Integer::sum);
                sender.sendMessage(ChatColor.GREEN + "Insígnia '" + badgeId + "' adicionada para " + target.getName() + ".");
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Erro ao salvar os dados do jogador.");
                e.printStackTrace();
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "O jogador já possui a insígnia '" + badgeId + "'.");
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
        File playerFile = new File(plugin.getDataFolder(), "playerdata/" + target.getUniqueId() + ".yml");
        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);

        List<String> badges = playerData.getStringList("badges");
        if (badges.contains(badgeId)) {
            badges.remove(badgeId);
            playerData.set("badges", badges);
            try {
                playerData.save(playerFile);
                // Atualiza o cache
                badgeCountsCache.merge(target.getUniqueId(), -1, Integer::sum);
                sender.sendMessage(ChatColor.GREEN + "Insígnia '" + badgeId + "' removida de " + target.getName() + ".");
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Erro ao salvar os dados do jogador.");
                e.printStackTrace();
            }
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
        UUID playerUUID = player.getUniqueId();

        // Lógica de toggle usando o Set em memória
        if (progressToggledOff.contains(playerUUID)) {
            progressToggledOff.remove(playerUUID);
            player.sendMessage(ChatColor.GREEN + "Mensagens de progresso de insígnias ativadas.");
        } else {
            progressToggledOff.add(playerUUID);
            player.sendMessage(ChatColor.YELLOW + "Mensagens de progresso de insígnias desativadas.");
        }
        // Lembre-se de salvar o estado em player quit/join events
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
            List<String> subcommands = new ArrayList<>(Arrays.asList("badges", "progress", "toggleprogress", "top", "version"));
            if (sender.hasPermission("godmode.scout.admin")) {
                subcommands.addAll(Arrays.asList("addbadge", "removebadge", "reload"));
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("addbadge") || args[0].equalsIgnoreCase("removebadge") || args[0].equalsIgnoreCase("progress"))) {
            // Suggest online player names
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("addbadge") || args[0].equalsIgnoreCase("removebadge"))) {
            var badgeSection = plugin.getConfig().getConfigurationSection("badges");
            if (badgeSection != null) {
                Set<String> badgeIds = badgeSection.getKeys(false);
                return StringUtil.copyPartialMatches(args[2], badgeIds, new ArrayList<>());
            }
        }

        return Collections.emptyList();
    }
}