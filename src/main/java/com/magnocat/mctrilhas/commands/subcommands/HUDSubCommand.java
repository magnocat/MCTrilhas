package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout hud`.
 * <p>
 * Este comando ativa ou desativa a exibição da BossBar de estatísticas (HUD)
 * para o jogador que o executa.
 */
public class HUDSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public HUDSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "hud"; }

    @Override
    public String getDescription() { return "Ativa ou desativa o painel de estatísticas na tela (HUD)."; }

    @Override
    public String getSyntax() { return "/scout hud"; }

    @Override
    public String getPermission() { return "mctrilhas.hud"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-only-command", "&cEste comando só pode ser usado por jogadores.")));
            return;
        }
        Player player = (Player) sender;
        // CORREÇÃO: O nome do método para alternar o HUD é 'toggleHUD',
        // seguindo a convenção de nomenclatura para acrônimos.
        plugin.getHudManager().toggleHUD(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}