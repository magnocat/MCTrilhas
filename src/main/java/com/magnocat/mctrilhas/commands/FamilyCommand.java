package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FamilyCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    public FamilyCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("token")) {
            player.sendMessage("§eGerando seu link de acesso seguro para o Portal da Família...");

            // Chama o método centralizado que gera um novo token e invalida o antigo.
            String token = plugin.getPlayerDataManager().regenerateWebAccessToken(player.getUniqueId());

            if (token == null) {
                player.sendMessage("§cOcorreu um erro ao gerar seu token. Por favor, contate um administrador.");
                return true;
            }

            String baseUrl = plugin.getConfig().getString("web-api.base-url", "http://localhost:22222");
            // Garante que não haja barras extras
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String fullUrl = baseUrl + "/admin/pdash.html?token=" + token;

            player.sendMessage("§aSeu link exclusivo foi gerado! §c(Não compartilhe com ninguém)");
            player.sendMessage("§7Este link dá acesso ao seu painel de progresso. Se você suspeitar que alguém o acessou, use este comando novamente para gerar um novo link e invalidar o antigo.");

            TextComponent message = new TextComponent("§e[CLIQUE AQUI PARA ABRIR O PAINEL]");
            message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, fullUrl));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para abrir seu painel de progresso!").create()));
            player.spigot().sendMessage(message);
        } else {
            player.sendMessage("§cComando inválido. Use: /familia token");
        }
        return true;
    }
}