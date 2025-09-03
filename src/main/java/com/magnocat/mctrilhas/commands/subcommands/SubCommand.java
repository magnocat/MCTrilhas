package com.magnocat.mctrilhas.commands.subcommands;

import org.bukkit.command.CommandSender;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

public abstract class SubCommand {

    protected final MCTrilhasPlugin plugin;

    public SubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    // Informações do comando para a mensagem de ajuda
    public abstract String getName();
    public abstract String getDescription();
    public abstract String getSyntax();
    public abstract String getPermission();
    public abstract boolean isAdminCommand();

    /**
     * Executa a lógica do subcomando.
     *
     * @param sender Quem enviou o comando.
     * @param args   Argumentos do comando, sem o nome do subcomando.
     */
    public abstract void execute(CommandSender sender, String[] args);
}