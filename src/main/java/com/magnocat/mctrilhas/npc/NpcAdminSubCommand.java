package com.magnocat.mctrilhas.npc;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementa o subcomando de admin `/scout admin npc ...`.
 * <p>
 * Este comando atua como um roteador para todas as ações de gerenciamento de
 * NPCs, como criar, deletar e listar.
 */
public class NpcAdminSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public NpcAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "npc";
    }

    @Override
    public String getDescription() {
        return "Gerencia os NPCs do servidor.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin npc <create|delete|list> [args]";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.admin.npc"; // Permissão específica para gerenciar NPCs
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public boolean isModuleEnabled(MCTrilhasPlugin plugin) {
        return plugin.getNpcManager() != null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        // O comando precisa de pelo menos uma ação (ex: create, delete)
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        String action = args[0].toLowerCase();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (action) {
                case "create":
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Uso: /scout admin npc create <id> <nome_do_npc>");
                        return;
                    }
                    String id = args[1];
                    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    plugin.getNpcManager().createNpc(player, id, name);
                    break;

                case "delete":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + "Uso: /scout admin npc delete <id>");
                        return;
                    }
                    plugin.getNpcManager().deleteNpc(player, args[1]);
                    break;

                case "list":
                    Collection<Npc> allNpcs = plugin.getNpcManager().getAllNpcs();
                    if (allNpcs.isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "Nenhum NPC foi criado ainda.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GOLD + "--- Lista de NPCs Criados ---");
                    for (Npc npc : allNpcs) {
                        Location loc = npc.location();
                        String locationStr = String.format("%s (%.0f, %.0f, %.0f)", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
                        sender.sendMessage(ChatColor.AQUA + "ID: " + ChatColor.WHITE + npc.id() + ChatColor.GRAY + " | " + ChatColor.AQUA + "Nome: " + npc.name() + ChatColor.GRAY + " | " + ChatColor.AQUA + "Local: " + ChatColor.WHITE + locationStr);
                    }
                    break;

                default:
                    sender.sendMessage(ChatColor.RED + "Subcomando de NPC desconhecido. Use: create, delete, list");
                    break;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}