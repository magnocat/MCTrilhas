package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout progress [jogador]`.
 * <p>
 * Este comando exibe um resumo do progresso de um jogador, mostrando as metas
 * para as próximas insígnias que ele ainda não conquistou e também o progresso
 * para o próximo ranque. Funciona para jogadores online e offline.
 */
public class ProgressSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ProgressSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "progress"; }

    @Override
    public String getDescription() { return "Mostra seu progresso para as próximas insígnias e ranque."; }

    @Override
    public String getSyntax() { return "/scout progress [jogador]"; }

    @Override
    public String getPermission() { return "mctrilhas.scout.use"; }

    @Override
    public boolean isAdminCommand() { return false; }

    /**
     * Executa a lógica para exibir o progresso de um jogador.
     * A busca de dados é feita de forma assíncrona para suportar jogadores offline sem travar o servidor.
     *
     * @param sender A entidade que executou o comando.
     * @param args Os argumentos fornecidos. Pode conter o nome de um jogador alvo.
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        OfflinePlayer target = getTargetPlayer(sender, args);
        if (target == null) return;

        sender.sendMessage(ChatColor.YELLOW + "Buscando progresso de " + target.getName() + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(target.getUniqueId());
            }

            if (playerData == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de " + target.getName() + "."));
                return;
            }

            final PlayerData finalPlayerData = playerData;
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + target.getName() + " ---");
                displayBadgeProgress(sender, finalPlayerData);
                displayRankProgress(sender, target, finalPlayerData);
                sender.sendMessage(ChatColor.GOLD + "---------------------------------");
            });
        });
    }

    private void displayBadgeProgress(CommandSender sender, PlayerData playerData) {
        sender.sendMessage(ChatColor.DARK_AQUA + "» Progresso das Insígnias:");
        boolean hasUnearnedBadges = false;

        List<Badge> allBadges = plugin.getBadgeManager().getAllBadges();
        for (Badge badge : allBadges) {
            if (!playerData.hasBadge(badge.id())) {
                hasUnearnedBadges = true;
                long required = (long) badge.requirement();
                long current = (long) playerData.getProgress(badge.type());
                MessageUtils.displayRequirement(sender, badge.name(), current, required);
            }
        }

        if (!hasUnearnedBadges) {
            sender.sendMessage(ChatColor.GREEN + "   Todas as insígnias foram conquistadas!");
        }
    }

    private void displayRankProgress(CommandSender sender, OfflinePlayer target, PlayerData playerData) {
        sender.sendMessage(""); // Spacer
        sender.sendMessage(ChatColor.DARK_AQUA + "» Progresso de Ranque:");

        MessageUtils.displayRankProgress(sender, target, playerData, plugin);
    }

    private OfflinePlayer getTargetPlayer(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("mctrilhas.progress.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver o progresso de outros jogadores.");
                return null;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[0] + "' nunca foi visto neste servidor.");
                return null;
            }
            return target;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Especifique um jogador para ver o progresso.");
            return null;
        }
        return (Player) sender;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("mctrilhas.progress.other")) {
            return null; // Deixa o Bukkit autocompletar nomes de jogadores
        }
        return Collections.emptyList();
    }
}