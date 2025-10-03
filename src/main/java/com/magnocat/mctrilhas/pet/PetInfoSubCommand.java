package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout pet info`.
 */
public class PetInfoSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetInfoSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "info"; }

    @Override
    public String getDescription() { return "Mostra as estatísticas do seu pet."; }

    @Override
    public String getSyntax() { return "/scout pet info"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.info"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem ver as informações de pets.");
            return;
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        PetData petData = playerData != null ? playerData.getPetData() : null;

        if (petData == null || !petData.isOwned()) {
            player.sendMessage(ChatColor.RED + "Você ainda não tem um pet. Use /scout pet loja para adquirir um.");
            return;
        }

        int currentXp = (int) petData.getExperience();
        int nextLevelXp = petData.getExperienceToNextLevel();

        sender.sendMessage(ChatColor.GOLD + "--- Estatísticas de " + petData.getName() + " ---");
        sender.sendMessage(ChatColor.AQUA + "Tipo: " + ChatColor.WHITE + petData.getType());
        sender.sendMessage(ChatColor.AQUA + "Nível: " + ChatColor.WHITE + petData.getLevel());
        sender.sendMessage(ChatColor.AQUA + "Felicidade: " + ChatColor.WHITE + String.format("%.0f%%", petData.getHappiness()));

        if (petData.getLevel() < PetData.MAX_LEVEL) {
            sender.sendMessage(ChatColor.AQUA + "XP: " + ChatColor.WHITE + currentXp + " / " + nextLevelXp);
        } else {
            sender.sendMessage(ChatColor.AQUA + "XP: " + ChatColor.GREEN + "Nível Máximo!");
        }
        sender.sendMessage(ChatColor.GOLD + "---------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}