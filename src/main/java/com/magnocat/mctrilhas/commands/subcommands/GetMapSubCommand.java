package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementa o subcomando de jogador `/scout getmap <insignia>`.
 * <p>
 * Este comando permite que um jogador recupere uma cópia do mapa-troféu
 * de uma insígnia que ele já conquistou, caso a insígnia ofereça tal recompensa.
 */
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
        return "Recupera o mapa-troféu de uma insígnia já conquistada.";
    }

    @Override
    public String getSyntax() {
        return "/scout getmap <id_da_insignia>";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.getmap";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }

    /**
     * Executa a lógica para entregar um mapa-troféu a um jogador.
     *
     * @param sender A entidade que executou o comando (deve ser um jogador).
     * @param args Os argumentos fornecidos. Espera-se: [id_da_insignia].
     */
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

        // 1. Verifica se o jogador possui a insígnia.
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.hasBadge(badgeId)) {
            player.sendMessage(ChatColor.RED + "Você ainda não conquistou a insígnia '" + badgeId + "'.");
            return;
        }

        // 2. Obtém o objeto Badge para verificar se ele tem uma recompensa de mapa.
        Badge badge = plugin.getBadgeManager().getBadge(badgeId);
        if (badge == null) {
            player.sendMessage(ChatColor.RED + "A insígnia '" + badgeId + "' não é válida.");
            return;
        }

        // 3. Verifica se a insígnia realmente oferece um mapa como recompensa.
        String mapRewardPath = "badges." + badge.id() + ".reward-map";
        if (!plugin.getBadgeConfigManager().getBadgeConfig().isConfigurationSection(mapRewardPath)) {
            player.sendMessage(ChatColor.RED + "Esta insígnia não possui um mapa-troféu como recompensa.");
            return;
        }

        // 4. Verifica se o jogador já tem o mapa no inventário para evitar duplicação.
        String mapNameTemplate = plugin.getBadgeConfigManager().getBadgeConfig().getString(mapRewardPath + ".name", "");
        String finalMapName = ChatColor.translateAlternateColorCodes('&', mapName
                .replace("{badgeName}", badge.name())
                .replace("{player}", player.getName()));

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && finalMapName.equals(item.getItemMeta().getDisplayName())) {
                player.sendMessage(ChatColor.YELLOW + "Você já possui este troféu no seu inventário.");
                return;
            }
        }

        // 5. Se passou por todas as verificações, cria e entrega um novo mapa.
        ItemStack mapReward = plugin.getMapRewardManager().createMapReward(player, badge.id());
        if (mapReward != null) {
            player.getInventory().addItem(mapReward);
            player.sendMessage(ChatColor.GREEN + "Você recebeu uma nova cópia do seu troféu!");
        }
    }

    /**
     * Fornece sugestões de autocompletar para o comando.
     * Sugere apenas as insígnias que o jogador já conquistou e que possuem um mapa-troféu.
     *
     * @param sender A entidade que está tentando autocompletar o comando.
     * @param args Os argumentos atuais digitados pelo remetente.
     * @return Uma lista de sugestões para o próximo argumento.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (playerData == null) return Collections.emptyList();

            String partialBadge = args[0].toLowerCase();

            // Filtra apenas as insígnias que o jogador possui E que têm um mapa como recompensa.
            return playerData.getEarnedBadgesMap().keySet().stream()
                    .filter(id -> plugin.getBadgeConfigManager().getBadgeConfig().isSet("badges." + id + ".reward-map"))
                    .filter(id -> id.toLowerCase().startsWith(partialBadge))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}