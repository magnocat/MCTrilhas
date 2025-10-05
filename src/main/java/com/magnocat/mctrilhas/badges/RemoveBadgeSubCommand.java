package com.magnocat.mctrilhas.badges;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementa o subcomando de administrador `/scout admin removebadge`.
 * <p>
 * Este comando permite que um administrador remova uma insígnia de um jogador
 * (online ou offline) e zere o progresso associado a ela. A operação é
 * executada de forma assíncrona para não impactar a performance do servidor.
 */
public class RemoveBadgeSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public RemoveBadgeSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "removebadge";
    }

    @Override
    public String getDescription() {
        return "Remove uma insígnia de um jogador (online ou offline).";
    }

    @Override
    public String getSyntax() {
        return "/scout admin removebadge <jogador> <insignia>";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    /**
     * Executa a lógica para remover uma insígnia de um jogador.
     * A operação é assíncrona para suportar jogadores offline.
     *
     * @param sender A entidade que executou o comando.
     * @param args Os argumentos fornecidos. Espera-se: [nome_do_jogador, id_da_insignia].
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso incorreto. Sintaxe: " + getSyntax());
            return;
        }

        String badgeId = args[1];
        String targetName = args[0];

        sender.sendMessage(ChatColor.YELLOW + "Processando remoção da especialidade '" + badgeId + "' para " + targetName + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' nunca foi visto neste servidor."));
                return;
            }

            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            boolean wasOnline = playerData != null;

            if (!wasOnline) {
                playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(target.getUniqueId());
            }

            if (playerData == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de " + targetName + "."));
                return;
            }

            // Lógica para remover a insígnia e zerar o progresso
            playerData.getEarnedBadgesMap().remove(badgeId.toLowerCase());
            try {
                BadgeType type = BadgeType.valueOf(badgeId.toUpperCase());
                playerData.getProgressMap().put(type, 0.0);
            } catch (IllegalArgumentException e) {
                // Ignora, pois nem toda insígnia tem um tipo de progresso rastreável.
            }

            // Se o jogador estava offline, precisamos salvar os dados de volta no arquivo.
            if (!wasOnline) {
                plugin.getPlayerDataManager().savePlayerData(playerData);
            }

            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.GREEN + "A especialidade '" + badgeId + "' e seu progresso foram removidos de " + targetName + "."));
        });
    }

    /**
     * Fornece sugestões de autocompletar para o comando.
     * Sugere apenas as insígnias que o jogador alvo (se online) possui.
     *
     * @param sender A entidade que está tentando autocompletar o comando.
     * @param args Os argumentos atuais digitados pelo remetente.
     * @return Uma lista de sugestões para o próximo argumento.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return null; // Usa o completador padrão do Bukkit para nomes de jogadores.
        }

        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return Collections.emptyList(); // Não autocompleta para jogadores offline.

            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) return Collections.emptyList();

            String partialBadge = args[1].toLowerCase();
            return new ArrayList<>(playerData.getEarnedBadgesMap().keySet()).stream()
                    .filter(id -> id.toLowerCase().startsWith(partialBadge))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}