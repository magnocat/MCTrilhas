package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GetMapSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public GetMapSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "getmap";
    }

    @Override
    public String getDescription() {
        return "Recupera o mapa-troféu de uma insígnia conquistada.";
    }

    @Override
    public String getSyntax() {
        return "/scout getmap <insignia>";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.getmap";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso incorreto. Sintaxe: " + getSyntax());
            return;
        }

        Player player = (Player) sender;
        String badgeId = args[0];

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.hasBadge(badgeId)) {
            player.sendMessage(ChatColor.RED + "Você ainda não conquistou a insígnia '" + badgeId + "'.");
            return;
        }

        String configKey = plugin.getBadgeConfigManager().getBadgeConfigKey(badgeId);
        if (configKey == null) {
            player.sendMessage(ChatColor.RED + "A insígnia '" + badgeId + "' não é válida.");
            return;
        }

        // Verifica se o jogador já tem o mapa no inventário
        String mapName = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + configKey + ".reward-map.name", "");
        String badgeName = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + configKey + ".name", configKey);
        String finalMapName = ChatColor.translateAlternateColorCodes('&', mapName
                .replace("{badgeName}", badgeName)
                .replace("{player}", player.getName()));

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(finalMapName)) {
                player.sendMessage(ChatColor.YELLOW + "Você já possui este troféu no seu inventário.");
                return;
            }
        }

        // Se não encontrou, cria e entrega um novo mapa.
        ItemStack mapReward = plugin.getMapRewardManager().createMapReward(player, configKey);
        if (mapReward != null) {
            player.getInventory().addItem(mapReward);
            player.sendMessage(ChatColor.GREEN + "Você recebeu uma nova cópia do seu troféu!");
        } else {
            player.sendMessage(ChatColor.RED + "Esta insígnia não possui um mapa-troféu como recompensa.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // Lógica para autocompletar o comando /scout getmap <insignia>
        if (args.length == 1) {
            String partialBadge = args[0].toLowerCase();

            // Obtém todos os IDs de insígnias da configuração (ex: MINING, LUMBERJACK)
            Set<String> badgeIds = plugin.getBadgeConfigManager().getBadgeConfig().getConfigurationSection("badges").getKeys(false);

            // Filtra e retorna as insígnias que começam com o que o jogador digitou
            return badgeIds.stream()
                    .filter(id -> !id.equalsIgnoreCase("use-gui")) // Ignora a chave de configuração da GUI
                    .filter(id -> id.toLowerCase().startsWith(partialBadge))
                    .sorted() // Ordena alfabeticamente para uma melhor experiência
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}