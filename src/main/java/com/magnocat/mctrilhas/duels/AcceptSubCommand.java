package com.magnocat.mctrilhas.duels;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

/**
 * Implementa o subcomando `/duelo aceitar <jogador>`.
 */
public class AcceptSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public AcceptSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "aceitar"; }

    @Override
    public String getDescription() { return "Aceita um desafio de duelo pendente."; }

    @Override
    public String getSyntax() { return "/duelo aceitar <jogador>"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.accept"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem aceitar duelos.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        Player target = (Player) sender;
        Player challenger = Bukkit.getPlayer(args[0]);

        if (challenger == null || !challenger.isOnline()) {
            target.sendMessage(ChatColor.RED + "O jogador '" + args[0] + "' não está online.");
            return;
        }

        plugin.getDuelManager().acceptChallenge(target, challenger);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Challenge challenge = plugin.getDuelManager().getChallengeFor(((Player) sender).getUniqueId()); // Busca o desafio pendente para o jogador
            // Se houver um desafio, sugere o nome do desafiante. Usa o accessor de record 'challenger()'.
            if (challenge != null && challenge.challenger().getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                return List.of(challenge.challenger().getName());
            }
        }
        return Collections.emptyList();
    }
}