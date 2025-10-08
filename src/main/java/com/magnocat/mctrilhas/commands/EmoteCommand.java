package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementa o comando /emote para jogadores e NPCs.
 */
public class EmoteCommand implements CommandExecutor, TabCompleter {

    private final MCTrilhasPlugin plugin;

    public EmoteCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: /emote <tipo> [jogador]");
            return true;
        }

        String emoteType = args[0].toLowerCase();
        Player target = null;

        if (args.length > 1) {
            // Se um alvo for especificado (usado pelo console para NPCs)
            if (!sender.hasPermission("mctrilhas.admin.emote.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para fazer outros jogadores usarem emotes.");
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[1] + "' não encontrado.");
                return true;
            }
        } else if (sender instanceof Player) {
            // Se nenhum alvo for especificado, o alvo é quem executou o comando
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador alvo: /emote <tipo> <jogador>");
            return true;
        }

        switch (emoteType) {
            case "acenar":
                target.swingMainHand();
                // Poderíamos adicionar uma mensagem ou partícula aqui no futuro.
                break;
            // Adicionar outros tipos de emote aqui no futuro
            default:
                sender.sendMessage(ChatColor.RED + "Emote '" + emoteType + "' desconhecido. Tente: acenar");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("acenar").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}