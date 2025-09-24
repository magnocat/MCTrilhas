package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo setpos <1|2>`.
 */
public class SetArenaPosSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SetArenaPosSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "setpos"; }

    @Override
    public String getDescription() { return "Define um ponto de spawn para a arena em criação."; }

    @Override
    public String getSyntax() { return "/duelo setpos <1|2>"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.admin"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        Player admin = (Player) sender;
        try {
            int pos = Integer.parseInt(args[0]);
            plugin.getDuelManager().setArenaPosition(admin, pos);
        } catch (NumberFormatException e) {
            admin.sendMessage(ChatColor.RED + "Posição inválida. Use 1 ou 2.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("1", "2");
        return Collections.emptyList();
    }
}