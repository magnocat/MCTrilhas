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

        PlayerData afilhadoData = plugin.getPlayerDataManager().getPlayerData(afilhado.getUniqueId());
        if (afilhadoData == null) {
            padrinho.sendMessage(ChatColor.RED + "Ocorreu um erro ao carregar os dados de " + afilhado.getName() + ".");
            return true;
        }

        // Nova validação: Verifica se o jogador já tem um padrinho.
        if (afilhadoData.getGodfatherUUID() != null) {
            padrinho.sendMessage(ChatColor.RED + afilhado.getName() + " já foi apadrinhado por outro jogador.");
            return true;
        }

        // Define o padrinho nos dados do afilhado para registrar a responsabilidade.
        afilhadoData.setGodfatherUUID(padrinho.getUniqueId());

        // Lógica condicional: Promove e recompensa apenas se for um visitante.
        if (afilhadoData.getRank() == Rank.VISITANTE) {
            // Promove o visitante para Filhote
            afilhadoData.setRank(Rank.FILHOTE);

            // Concede a insígnia de boas-vindas ao novo membro.
            plugin.getPlayerDataManager().grantBadgeAndReward(afilhado, "WELCOME");

            // Mensagens de confirmação para promoção
            String padrinhoMessage = "§aVocê apadrinhou §e" + afilhado.getName() + "§a! Ele agora é um Filhote e pode interagir com o mundo.";
            String afilhadoMessage = "§aVocê foi apadrinhado por §e" + padrinho.getName() + "§a e promovido para §fFilhote§a! Bem-vindo(a) oficialmente ao grupo!";
            String broadcastMessage = "§e" + padrinho.getName() + " §adeu as boas-vindas a §e" + afilhado.getName() + "§a, que agora faz parte da nossa família de Filhotes!";

            padrinho.sendMessage(padrinhoMessage);
            afilhado.sendMessage(afilhadoMessage);
            Bukkit.broadcastMessage(broadcastMessage);
        } else {
            // Mensagens de confirmação para jogadores já existentes
            padrinho.sendMessage("§aVocê agora é o padrinho de §e" + afilhado.getName() + "§a e será responsável por guiá-lo!");
            afilhado.sendMessage("§aVocê agora tem um padrinho! §e" + padrinho.getName() + "§a irá te ajudar em sua jornada.");
        }

        // Concede a insígnia de boas-vindas, independentemente do ranque,
        // para que jogadores antigos também possam recebê-la.
        // O método grantBadgeAndReward já impede a duplicação se o jogador já tiver.
        plugin.getPlayerDataManager().grantBadgeAndReward(afilhado, "WELCOME");

        // Salva os dados do afilhado após todas as modificações.
        plugin.getPlayerDataManager().savePlayerData(afilhadoData);

        return true;
    }
}