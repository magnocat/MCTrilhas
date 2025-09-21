package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import com.magnocat.mctrilhas.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Implementa o comando `/ranque`.
 * <p>
 * Este comando exibe ao jogador seu ranque atual e os requisitos
 * necessários para alcançar o próximo, utilizando {@link MessageUtils}
 * para formatar a exibição do progresso.
 */
public class RankCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    /**
     * Construtor do comando de ranque.
     * @param plugin A instância principal do plugin.
     */
    public RankCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executa a lógica do comando `/ranque`.
     *
     * @param sender A entidade que executou o comando.
     * @param command O comando que foi executado.
     * @param label O alias do comando que foi usado.
     * @param args Argumentos do comando (não utilizados neste comando).
     * @return {@code true} se o comando foi tratado com sucesso.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-only-command", "&cEste comando só pode ser usado por jogadores.")));
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.rank-command.data-error", "&cNão foi possível carregar seus dados. Tente novamente em alguns instantes.")));
            return true;
        }

        Rank currentRank = playerData.getRank();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.rank-command.header", "&6--- Seu Progresso de Ranque ---")));
        String currentRankMessage = plugin.getConfig().getString("messages.rank-command.current-rank", "&eSeu ranque atual: {rank}");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', currentRankMessage.replace("{rank}", currentRank.getColor() + currentRank.getDisplayName())));

        // Se o jogador já atingiu o ranque máximo, exibe uma mensagem especial.
        if (currentRank == Rank.PIONEIRO || currentRank == Rank.CHEFE) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.rank-command.max-rank", "&aVocê alcançou o topo da jornada escoteira! Parabéns!")));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.rank-command.footer", "&6---------------------------------")));
            return true;
        }

        player.sendMessage(""); // Linha em branco para espaçamento

        // A lógica de exibição do progresso foi centralizada em MessageUtils
        MessageUtils.displayRankProgress(player, player, playerData, plugin);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.rank-command.footer", "&6---------------------------------")));

        return true;
    }
}