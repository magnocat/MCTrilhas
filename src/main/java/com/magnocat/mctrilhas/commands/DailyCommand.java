package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerDataManager;
// Usando a classe de fábrica de itens do projeto
import com.magnocat.mctrilhas.utils.ItemFactory; 
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

    private final MCTrilhasPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;

    public DailyCommand(MCTrilhasPlugin plugin) {
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
            sendMessage(player, dailyRewardSection, "messages.disabled", "&cO sistema de recompensa diária está desativado.");
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
            long remainingMillis = (lastClaim + cooldownMillis) - System.currentTimeMillis();
            String timeRemaining = formatTime(remainingMillis);
            sendMessage(player, dailyRewardSection, "messages.cooldown", "&cVocê já coletou sua recompensa. Tente novamente em &e{time}&c.", "{time}", timeRemaining);
        }

        return true;
    }

    private void handleRewardClaim(Player player, ConfigurationSection dailyRewardSection) {
        int totemAmount = dailyRewardSection.getInt("reward-totems", 0);
        ItemStack rewardItem = ItemFactory.createFromConfig(dailyRewardSection.getConfigurationSection("reward-item-data"));

        // Verifica se há espaço no inventário ANTES de dar qualquer recompensa
        if (rewardItem != null && player.getInventory().firstEmpty() == -1) {
            sendMessage(player, dailyRewardSection, "messages.inventory-full", "&cSeu inventário está cheio! Libere espaço para coletar sua recompensa.");
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
        sendMessage(player, dailyRewardSection, "messages.claim-success", "&aVocê coletou sua recompensa diária! Volte amanhã para mais.");
        
        if (totemAmount > 0) {
            sendMessage(player, dailyRewardSection, "messages.totem-reward", "&e+ {amount} Totens!", "{amount}", String.valueOf(totemAmount));
        }
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
    private void sendMessage(Player player, ConfigurationSection section, String path, String defaultValue, String... replacements) {
        String message = defaultValue;
        if (section != null) {
            message = section.getString(path, defaultValue);
        }

        // Aplica os placeholders (ex: "{time}", "12h 30m")
        if (replacements != null && replacements.length > 0) {
            if (replacements.length % 2 != 0) {
                plugin.logWarn("Número ímpar de argumentos de substituição para sendMessage. As substituições serão ignoradas.");
            } else {
                for (int i = 0; i < replacements.length; i += 2) {
                    message = message.replace(replacements[i], replacements[i + 1]);
                }
            }
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}