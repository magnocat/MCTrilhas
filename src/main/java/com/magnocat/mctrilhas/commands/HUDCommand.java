package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementa o comando `/hud`.
 * <p>
 * Este comando permite que um jogador ative ou desative a exibição
 * de informações na tela (Heads-Up Display), como o progresso de insígnias.
 */
public class HUDCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    /**
     * Construtor do comando HUD.
     * @param plugin A instância principal do plugin.
     */
    public HUDCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executa a lógica do comando `/hud`.
     *
     * @param sender A entidade que executou o comando.
     * @param command O comando que foi executado.
     * @param label O alias do comando que foi usado.
     * @param args Argumentos do comando (não utilizados neste comando).
     * @return {@code true} se o comando foi tratado com sucesso.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            String consoleMessage = plugin.getConfig().getString("messages.player-only-command", "&cEste comando só pode ser usado por jogadores.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', consoleMessage));
            return true;
        }

        Player player = (Player) sender;
        plugin.getHudManager().toggleHUD(player);
        return true;
    }
}