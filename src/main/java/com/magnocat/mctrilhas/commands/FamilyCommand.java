package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.Base64;

public class FamilyCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    public FamilyCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("token")) {
            handleTokenGeneration(player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Uso incorreto. Tente /familia token");
        return true;
    }

    private void handleTokenGeneration(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Erro ao carregar seus dados. Tente relogar.");
            return;
        }

        String token = playerData.getWebAccessToken();
        if (token == null || token.isEmpty()) {
            token = generateNewToken();
            playerData.setWebAccessToken(token);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId()); // Salva imediatamente
        }

        String dashboardUrl = "http://SEU_IP_EXTERNO:" + plugin.getConfig().getInt("web-api.port") + "/admin/player_dashboard.html?token=" + token;

        player.sendMessage(ChatColor.GREEN + "=============================================");
        player.sendMessage(ChatColor.AQUA + "Seu link de acesso ao Painel da Família:");
        player.sendMessage(ChatColor.YELLOW + "Este link é único e secreto. Não compartilhe com ninguém!");
        player.sendMessage(ChatColor.WHITE + dashboardUrl);
        player.sendMessage(ChatColor.GRAY + "Copie e cole este link no seu navegador para ver seu progresso.");
        player.sendMessage(ChatColor.GREEN + "=============================================");
    }

    private String generateNewToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[24]; // 24 bytes = 192 bits, resulta em 32 caracteres Base64
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}