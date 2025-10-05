package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetFlagSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SetFlagSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "flag"; }

    @Override
    public String getDescription() { return "Altera uma permissão (flag) do seu terreno."; }

    @Override
    public String getSyntax() { return "/terreno flag <nome> <allow|deny>"; }

    @Override
    public String getPermission() { return "mctrilhas.land.setflag"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            sender.sendMessage(ChatColor.YELLOW + "Flags disponíveis: pvp, chest-access");
            return;
        }
        plugin.getLandManager().setRegionFlag((Player) sender, args[0], args[1]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Stream.of("pvp", "chest-access").filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Stream.of("allow", "deny").filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}