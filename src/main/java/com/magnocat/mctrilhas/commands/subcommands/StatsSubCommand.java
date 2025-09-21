package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.List;

/**
 * Implementa o subcomando de administrador `/scout admin stats <jogador>`.
 * <p>
 * Este comando exibe um resumo detalhado das estatísticas de um jogador,
 * incluindo o total de insígnias conquistadas e o progresso acumulado em
 * cada categoria de atividade. Funciona para jogadores online e offline.
 */
@SuppressWarnings("deprecation")
public class StatsSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final NumberFormat numberFormat;

    public StatsSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        this.numberFormat.setGroupingUsed(true);
    }

    /**
     * Executa a lógica para exibir as estatísticas de um jogador.
     * A busca de dados é feita de forma assíncrona para suportar jogadores offline sem travar o servidor.
     *
     * @param sender A entidade que executou o comando.
     * @param args Os argumentos fornecidos. Espera-se: [nome_do_jogador].
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado: " + args[0]);
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Buscando estatísticas de " + target.getName() + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(target.getUniqueId());
            }

            if (playerData == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados para " + target.getName() + "."));
                return;
            }

            final PlayerData finalPlayerData = playerData;
            final int totalBadges = plugin.getBadgeManager().getAllBadges().size();

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Estatísticas de " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " ---");
                sender.sendMessage(ChatColor.YELLOW + "Insígnias: " + ChatColor.AQUA + finalPlayerData.getEarnedBadgesMap().size() + " / " + totalBadges);
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.GOLD + "--- Progresso Total (Vida Inteira) ---");

                for (BadgeType type : BadgeType.values()) {
                    double progress = finalPlayerData.getProgress(type);
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + type.getDisplayName() + ": " + ChatColor.GREEN + numberFormat.format(progress));
                }
                sender.sendMessage(ChatColor.GOLD + "------------------------------------");
            });
        });
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin";
    }

    @Override
    public String getSyntax() {
        return "/scout admin stats <jogador>";
    }

    @Override
    public String getDescription() {
        return "Vê as estatísticas completas de um jogador.";
    }

    @Override
    public String getName() {
        return "stats";
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
        if (args.length == 1) {
            return null; // Usa o completador padrão do Bukkit para nomes de jogadores
        }
        return Collections.emptyList();
    }
}