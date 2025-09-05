package com.magnocat.mctrilhas.commands.subcommands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Interface que define a estrutura de um subcomando.
 */
public interface SubCommand {

    String getName();

    String getDescription();

    String getSyntax();

    String getPermission();

    boolean isAdminCommand();

    void execute(CommandSender sender, String[] args);

    default List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}