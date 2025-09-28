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
            String token = plugin.getPlayerDataManager().regenerateWebAccessToken(player.getUniqueId());

            if (token == null) {
                player.sendMessage(ChatColor.RED + "Ocorreu um erro ao gerar seu token de acesso. Tente novamente.");
                return true;
            }

            String baseUrl = plugin.getConfig().getString("web-api.base-url", "http://localhost:" + plugin.getConfig().getInt("web-api.port", 22222));
            String fullUrl = baseUrl + "/admin/pdash.html?token=" + token;

            player.sendMessage(ChatColor.GOLD + "--- Seu Link para o Portal da Família ---");

            // Mensagem clicável para clientes Java
            TextComponent linkMessage = new TextComponent("[CLIQUE AQUI PARA ABRIR O PORTAL]");
            linkMessage.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkMessage.setBold(true);
            linkMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, fullUrl));
            linkMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Abrir seu painel de progresso no navegador").color(net.md_5.bungee.api.ChatColor.GRAY).create()));
            player.spigot().sendMessage(linkMessage);

            // Mensagem de texto plano para clientes Bedrock (Geyser)
            player.sendMessage(ChatColor.GRAY + "Se o link acima não funcionar, copie e cole este no seu navegador:");
            player.sendMessage(ChatColor.WHITE + fullUrl);

            player.sendMessage(ChatColor.RED + "Atenção: Não compartilhe este link com ninguém!");
            player.sendMessage(ChatColor.GOLD + "------------------------------------------");

            return true;
        }

        player.sendMessage(ChatColor.RED + "Uso: /familia token");
        return true;
    }
}