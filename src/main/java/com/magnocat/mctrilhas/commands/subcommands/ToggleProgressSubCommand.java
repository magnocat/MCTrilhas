package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando de jogador `/scout toggleprogress`.
 * <p>
 * Este comando permite que um jogador ative ou desative o recebimento
 * de mensagens no chat sobre o progresso de suas insígnias.
 */
public class ToggleProgressSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ToggleProgressSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "toggleprogress";
    }

    @Override
    public String getDescription() {
        return "Ativa ou desativa as mensagens de progresso de insígnias.";
    }

    @Override
    public String getSyntax() {
        return "/scout toggleprogress";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.use";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }

    /**
     * Executa a lógica para alternar a exibição de mensagens de progresso.
     *
     * @param sender A entidade que executou o comando (deve ser um jogador).
     * @param args Argumentos do comando (não utilizados neste subcomando).
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return;
        }

        Player player = (Player) sender;
        boolean isDisabled = plugin.getPlayerDataManager().toggleProgressMessages(player.getUniqueId());

        if (isDisabled) {
            player.sendMessage(ChatColor.YELLOW + "Mensagens de progresso de insígnias foram " + ChatColor.RED + "desativadas.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Mensagens de progresso de insígnias foram " + ChatColor.GREEN + "ativadas.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}