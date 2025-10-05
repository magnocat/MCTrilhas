// package com.magnocat.mctrilhas.land;
//
// import com.magnocat.mctrilhas.MCTrilhasPlugin;
// import com.magnocat.mctrilhas.commands.SubCommand;
// import org.bukkit.ChatColor;
// import org.bukkit.command.CommandSender;
// import org.bukkit.entity.Player;
//
// import java.util.List;
//
// public class SetHomeSubCommand implements SubCommand {
//
//     private final MCTrilhasPlugin plugin;
//
//     public SetHomeSubCommand(MCTrilhasPlugin plugin) {
//         this.plugin = plugin;
//     }
//
//     @Override
//     public String getName() { return "sethome"; }
//
//     @Override
//     public String getDescription() { return "Define o ponto de teletransporte do seu terreno."; }
//
//     @Override
//     public String getSyntax() { return "/terreno sethome"; }
//
//     @Override
//     public String getPermission() { return "mctrilhas.land.sethome"; }
//
//     @Override
//     public boolean isAdminCommand() { return false; }
//
//     @Override
//     public void execute(CommandSender sender, String[] args) {
//         if (!(sender instanceof Player)) {
//             sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
//             return;
//         }
//         // plugin.getLandManager().setHomePoint((Player) sender); // Funcionalidade desativada
//     }
//
//     @Override
//     public List<String> onTabComplete(CommandSender sender, String[] args) {
//         return List.of();
//     }
// }