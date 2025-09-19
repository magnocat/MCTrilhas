package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

        player.sendMessage(Component.text("Uso incorreto. Tente /familia token", NamedTextColor.RED));
        return true;
    }

    private void handleTokenGeneration(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Component.text("Erro ao carregar seus dados. Tente relogar.", NamedTextColor.RED));
            return;
        }

        String token = playerData.getWebAccessToken();
        if (token == null || token.isEmpty()) {
            token = generateNewToken();
            playerData.setWebAccessToken(token);
            // O salvamento agora é feito de forma assíncrona ao deslogar, não precisa salvar aqui.
        }

        // Busca a URL base do config.yml, com um fallback para o IP local.
        String baseUrl = plugin.getConfig().getString("web-api.base-url");
        if (baseUrl == null || baseUrl.isEmpty() || baseUrl.contains("SEU_IP_EXTERNO")) {
             player.sendMessage(Component.text("O administrador do servidor ainda não configurou a URL do painel.", NamedTextColor.RED));
             player.sendMessage(Component.text("Por favor, peça para configurar a opção 'web-api.base-url' no config.yml.", NamedTextColor.GRAY));
             return;
        }

        // Garante que não haja barras extras
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String dashboardUrl = baseUrl + "/admin/player_dashboard.html?token=" + token;

        player.sendMessage(Component.text("=============================================", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Seu link de acesso ao Painel da Família:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("CLIQUE AQUI PARA ABRIR", NamedTextColor.YELLOW, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(dashboardUrl))
                .hoverEvent(HoverEvent.showText(Component.text("Clique para abrir o painel no seu navegador!", NamedTextColor.GRAY))));
        player.sendMessage(Component.text("Este link é único e secreto. Não compartilhe!", NamedTextColor.RED));
        player.sendMessage(Component.text("=============================================", NamedTextColor.GREEN));
    }

    private String generateNewToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[24]; // 24 bytes = 192 bits, resulta em 32 caracteres Base64
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}