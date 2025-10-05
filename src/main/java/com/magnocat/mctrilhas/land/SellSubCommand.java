package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class SellSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SellSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "vender"; }

    @Override
    public String getDescription() { return "Coloca seu terreno à venda para outro jogador."; }

    @Override
    public String getSyntax() { return "/terreno vender <jogador> <preço>"; }

    @Override
    public String getPermission() { return "mctrilhas.land.sell"; }

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
            return;
        }
        plugin.getLandManager().offerLandForSale((Player) sender, args[0], args[1]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}