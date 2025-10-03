package com.magnocat.mctrilhas.quests;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

/**
 * Implementa o comando `/tesouro` e seus subcomandos.
 * <p>
 * Esta classe atua como um roteador, delegando as ações de iniciar,
 * obter pistas e cancelar a caça ao tesouro para o {@link com.magnocat.mctrilhas.quests.TreasureHuntManager}.
 */
public class TreasureHuntCommand implements CommandExecutor, TabCompleter {

    private final MCTrilhasPlugin plugin;

    /**
     * Construtor do comando da Caça ao Tesouro.
     * @param plugin A instância principal do plugin.
     */
    public TreasureHuntCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executa a lógica do comando `/tesouro`.
     *
     * @param sender A entidade que executou o comando.
     * @param command O comando que foi executado.
     * @param label O alias do comando que foi usado.
     * @param args Argumentos do comando (ex: "iniciar", "pista").
     * @return {@code true} se o comando foi tratado com sucesso.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-only-command", "&cEste comando só pode ser usado por jogadores.")));
            return true;
        }
        Player player = (Player) sender;

        // Fail-safe: Verifica se o módulo está ativo
        if (plugin.getTreasureHuntManager() == null) {
            player.sendMessage(ChatColor.RED + "O sistema de Caça ao Tesouro está temporariamente desativado. Contate um administrador.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "iniciar":
                plugin.getTreasureHuntManager().startHunt(player);
                break;
            case "pista":
                plugin.getTreasureHuntManager().giveClue(player);
                break;
            case "cancelar":
                plugin.getTreasureHuntManager().cancelHunt(player);
                break;
            default:
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.treasure-command.unknown-subcommand", "&cComando desconhecido. Use /tesouro para ver a ajuda.")));
                break;
        }
        return true;
    }

    /**
     * Envia uma mensagem de ajuda com os subcomandos disponíveis para a Caça ao Tesouro.
     * @param player O jogador que receberá a mensagem.
     */
    private void sendHelpMessage(Player player) {
        List<String> helpLines = plugin.getConfig().getStringList("messages.treasure-command.help");
        if (helpLines.isEmpty()) {
            // Fallback para mensagens hardcoded se a configuração estiver ausente.
            player.sendMessage(ChatColor.GOLD + "--- Caça ao Tesouro ---");
            player.sendMessage(ChatColor.AQUA + "/tesouro iniciar" + ChatColor.GRAY + " - Começa uma nova caça ao tesouro.");
            player.sendMessage(ChatColor.AQUA + "/tesouro pista" + ChatColor.GRAY + " - Recebe uma pista para o próximo local.");
            player.sendMessage(ChatColor.AQUA + "/tesouro cancelar" + ChatColor.GRAY + " - Abandona a caça ao tesouro atual.");
            player.sendMessage(ChatColor.GOLD + "-----------------------");
        } else {
            helpLines.forEach(line -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', line)));
        }
    }

    /**
     * Fornece sugestões de autocompletar para o comando `/tesouro`.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("iniciar", "pista", "cancelar")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}