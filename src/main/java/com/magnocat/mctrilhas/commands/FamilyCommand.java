package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementa o comando `/familia token`.
 * <p>
 * Este comando gera um link de acesso único e seguro para o "Portal da Família",
 * permitindo que o jogador (ou seus responsáveis) acompanhe seu progresso em uma página web.
 * A cada uso, um novo token é gerado, invalidando o anterior por segurança.
 */
public class FamilyCommand implements CommandExecutor {
    private final MCTrilhasPlugin plugin;

    /**
     * Construtor do comando da família.
     * @param plugin A instância principal do plugin.
     */
    public FamilyCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executa a lógica do comando `/familia`.
     *
     * @param sender A entidade que executou o comando.
     * @param command O comando que foi executado.
     * @param label O alias do comando que foi usado.
     * @param args Argumentos do comando. Espera-se "token".
     * @return {@code true} se o comando foi tratado com sucesso.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-only-command", "&cEste comando só pode ser usado por jogadores.")));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("token")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.generating", "&eGerando seu link de acesso seguro para o Portal da Família...")));

            // Chama o método centralizado que gera um novo token e invalida o antigo.
            String token = plugin.getPlayerDataManager().regenerateWebAccessToken(player.getUniqueId());

            if (token == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.error", "&cOcorreu um erro ao gerar seu token. Por favor, contate um administrador.")));
                return true;
            }

            String baseUrl = plugin.getConfig().getString("web-api.base-url");
            // Validação da URL base para ajudar na configuração do servidor.
            if (baseUrl == null || baseUrl.isEmpty() || baseUrl.contains("SEU_IP_EXTERNO")) {
                player.sendMessage(ChatColor.RED + "O administrador do servidor ainda não configurou a URL do painel.");
                player.sendMessage(ChatColor.GRAY + "Por favor, peça para configurar a opção 'web-api.base-url' no config.yml.");
                return true;
            }

            // Garante que não haja barras extras
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String fullUrl = baseUrl + "/admin/pdash.html?token=" + token;

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.success", "&aSeu link exclusivo foi gerado! &c(Não compartilhe com ninguém)")));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.warning", "&7Este link dá acesso ao seu painel de progresso. Se você suspeitar que alguém o acessou, use este comando novamente para gerar um novo link e invalidar o antigo.")));

            String clickText = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.click-text", "&e[CLIQUE AQUI PARA ABRIR O PAINEL]"));
            String hoverText = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.hover-text", "&aClique para abrir seu painel de progresso!"));

            TextComponent message = new TextComponent(clickText);
            message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, fullUrl));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
            player.spigot().sendMessage(message);
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.family-command.invalid-usage", "&cComando inválido. Use: /familia token")));
        }
        return true;
    }
}