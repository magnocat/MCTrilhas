package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ApadrinharCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    public ApadrinharCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /apadrinhar <jogador>");
            return true;
        }

        Player padrinho = (Player) sender;
        Player afilhado = Bukkit.getPlayer(args[0]);

        if (afilhado == null || !afilhado.isOnline()) {
            padrinho.sendMessage(ChatColor.RED + "O jogador '" + args[0] + "' não está online.");
            return true;
        }

        if (padrinho.equals(afilhado)) {
            padrinho.sendMessage(ChatColor.RED + "Você não pode apadrinhar a si mesmo.");
            return true;
        }

        // Validações de ranque
        Rank padrinhoRank = plugin.getPlayerDataManager().getRank(padrinho.getUniqueId());
        if (padrinhoRank == Rank.VISITANTE) {
            padrinho.sendMessage(ChatColor.RED + "Visitantes não podem apadrinhar outros jogadores.");
            return true;
        }

        Rank afilhadoRank = plugin.getPlayerDataManager().getRank(afilhado.getUniqueId());
        if (afilhadoRank != Rank.VISITANTE) {
            padrinho.sendMessage(ChatColor.RED + afilhado.getName() + " não é um visitante e não pode ser apadrinhado.");
            return true;
        }

        // Lógica de promoção
        PlayerData afilhadoData = plugin.getPlayerDataManager().getPlayerData(afilhado.getUniqueId());
        if (afilhadoData == null) {
            padrinho.sendMessage(ChatColor.RED + "Ocorreu um erro ao carregar os dados de " + afilhado.getName() + ".");
            return true;
        }

        // Promove o visitante para Filhote
        afilhadoData.setRank(Rank.FILHOTE);
        // Define o padrinho nos dados do afilhado para registrar a responsabilidade.
        afilhadoData.setGodfatherUUID(padrinho.getUniqueId());
        plugin.getPlayerDataManager().savePlayerData(afilhadoData); // Salva a alteração

        // Mensagens de confirmação
        String padrinhoMessage = "§aVocê apadrinhou §e" + afilhado.getName() + "§a! Ele agora é um Filhote e pode interagir com o mundo.";
        String afilhadoMessage = "§aVocê foi apadrinhado por §e" + padrinho.getName() + "§a e promovido para §fFilhote§a! Bem-vindo(a) oficialmente ao grupo!";
        String broadcastMessage = "§e" + padrinho.getName() + " §adeu as boas-vindas a §e" + afilhado.getName() + "§a, que agora faz parte da nossa família de Filhotes!";

        padrinho.sendMessage(padrinhoMessage);
        afilhado.sendMessage(afilhadoMessage);
        Bukkit.broadcastMessage(broadcastMessage);

        return true;
    }
}