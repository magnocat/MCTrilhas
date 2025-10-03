package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Implementa o subcomando de jogador `/scout version`.
 * <p>
 * Este comando exibe a versão atual do plugin para o jogador.
 */
@SuppressWarnings("deprecation") // Suprime avisos para ChatColor e getDescription, que são de uso comum.
public class VersionSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public VersionSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "version";
    }

    @Override
    public String getDescription() {
        return "Exibe a versão do plugin.";
    }

    @Override
    public String getSyntax() {
        return "/scout version";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.use";
    }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    /**
     * Executa a lógica para exibir a versão do plugin.
     *
     * @param sender A entidade que executou o comando.
     * @param args Argumentos do comando (não utilizados neste subcomando).
     */
    public void execute(CommandSender sender, String[] args) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.GOLD + "MCTrilhas " + ChatColor.AQUA + "v" + version);
    }
}