package com.magnocat.godmode.commands.subcommands;

import org.bukkit.command.CommandSender;

import com.magnocat.godmode.GodModePlugin;

public abstract class SubCommand {

    protected final GodModePlugin plugin;

    public SubCommand(GodModePlugin plugin) {
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