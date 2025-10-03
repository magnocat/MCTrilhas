package com.magnocat.mctrilhas.hud;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout hud`.
 * <p>
 * Este comando permite que um jogador ative ou desative a exibição de
 * informações na tela (Heads-Up Display) via BossBar.
 */
public class HUDSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public HUDSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "hud";
    }

    @Override
    public String getDescription() {
        return "Ativa ou desativa a barra de informações na tela.";
    }

    @Override
    public String getSyntax() {
        return "/scout hud";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.hud";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        Player player = (Player) sender;

        // Obtém os dados do jogador para salvar a preferência.
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados. Tente novamente.");
            return;
        }

        // Alterna o estado da HUD e salva a nova preferência.
        boolean isNowEnabled = !playerData.isHudEnabled();
        playerData.setHudEnabled(isNowEnabled);
        plugin.getHudManager().setHudVisibility(player, isNowEnabled);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
