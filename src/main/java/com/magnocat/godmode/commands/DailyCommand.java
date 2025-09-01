package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.data.PlayerDataManager;
// Usando a classe de fábrica de itens do projeto
import com.magnocat.godmode.utils.ItemFactory; 
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class DailyCommand implements CommandExecutor {

    private final GodModePlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;

    public DailyCommand(GodModePlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.economy = plugin.getEconomy(); // Assumindo que você tenha um método getEconomy() no plugin principal
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // Mensagem para console, não precisa de config
            sender.sendMessage("Este comando so pode ser executado por um jogador.");
            return true;
        }

        Player player = (Player) sender;
        ConfigurationSection dailyRewardSection = plugin.getConfig().getConfigurationSection("daily-reward");

        // Validações iniciais
        if (dailyRewardSection == null || !dailyRewardSection.getBoolean("enabled", false)) {
            sendMessage(player, dailyRewardSection, "messages.disabled", "&cO sistema de recompensa diaria esta desativado.");
            return true;
        }

        if (economy == null) {
            plugin.getLogger().severe("Vault (Economy) não foi encontrado! A recompensa diária não pode ser concedida.");
            sendMessage(player, dailyRewardSection, "messages.economy-error", "&cOcorreu um erro ao processar sua recompensa. Contate um administrador.");
            return true;
        }

        // Lógica de Cooldown
        long lastClaim = playerDataManager.getLastDailyRewardTime(player.getUniqueId());
        long cooldownHours = dailyRewardSection.getInt("cooldown-hours", 24);
        long cooldownMillis = TimeUnit.HOURS.toMillis(cooldownHours);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastClaim >= cooldownMillis) {
            handleRewardClaim(player, dailyRewardSection);
        } else {
            handleCooldownMessage(player, dailyRewardSection, lastClaim, cooldownMillis);
        }

        return true;
    }

    private void handleRewardClaim(Player player, ConfigurationSection dailyRewardSection) {
        int totemAmount = dailyRewardSection.getInt("reward-totems", 0);
        ItemStack rewardItem = ItemFactory.createFromConfig(dailyRewardSection.getConfigurationSection("reward-item-data"));

        // Verifica se há espaço no inventário ANTES de dar qualquer recompensa
        if (rewardItem != null && player.getInventory().firstEmpty() == -1) {
            sendMessage(player, dailyRewardSection, "messages.inventory-full", "&cSeu inventario esta cheio! Libere espaco para coletar sua recompensa diaria.");
            return;
        }

        // Entrega as recompensas
        if (totemAmount > 0) {
            economy.depositPlayer(player, totemAmount);
        }
        if (rewardItem != null) {
            player.getInventory().addItem(rewardItem);
        }

        // Atualiza o tempo da última coleta
        playerDataManager.setLastDailyRewardTime(player.getUniqueId(), System.currentTimeMillis());

        // Envia mensagens de sucesso
        String successMessage = dailyRewardSection.getString("messages.claim-success", "&aVoce coletou sua recompensa diaria!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage.replace("{amount}", String.valueOf(totemAmount))));
        
        if (totemAmount > 0) {
            String totemMessage = plugin.getConfig().getString("totem-reward-message", "&e+ {amount} Totens!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', totemMessage.replace("{amount}", String.valueOf(totemAmount))));
        }
    }

    private void handleCooldownMessage(Player player, ConfigurationSection dailyRewardSection, long lastClaim, long cooldownMillis) {
        long remainingMillis = (lastClaim + cooldownMillis) - System.currentTimeMillis();
        String timeRemaining = formatTime(remainingMillis);

        String cooldownMessage = dailyRewardSection.getString("messages.cooldown", "&cVoce ja coletou sua recompensa. Tente novamente em &e{time}&c.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMessage.replace("{time}", timeRemaining)));
    }

    /**
     * Formata milissegundos em uma string legível (ex: "12h 30m 15s").
     */
    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    /**
     * Helper para enviar mensagens configuráveis para o jogador.
     */
    private void sendMessage(Player player, ConfigurationSection section, String path, String defaultValue) {
        String message = defaultValue;
        if (section != null) {
            message = section.getString(path, defaultValue);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}