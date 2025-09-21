package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerDataManager;
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

/**
 * Implementa o comando `/daily`.
 * <p>
 * Este comando permite que os jogadores reivindiquem uma recompensa diária,
 * que pode consistir em Totens (moeda) e/ou um item customizado, sujeito a um cooldown configurável.
 */
@SuppressWarnings("deprecation")
public class DailyCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;

    /**
     * Construtor do comando de recompensa diária.
     *
     * @param plugin A instância principal do plugin, usada para acessar configurações e gerenciadores.
     */
    public DailyCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.economy = plugin.getEconomy(); // Assumindo que você tenha um método getEconomy() no plugin principal
    }

    /**
     * Executa a lógica do comando `/daily` quando um jogador o utiliza.
     * Verifica se o sistema está ativo, se a economia está funcionando e se o jogador
     * já cumpriu o tempo de espera (cooldown) para coletar a recompensa novamente.
     *
     * @param sender A entidade que executou o comando.
     * @param command O comando que foi executado.
     * @param label O alias do comando que foi usado.
     * @param args Argumentos do comando (não utilizados neste comando).
     * @return {@code true} se o comando foi tratado com sucesso.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String consoleMessage = plugin.getConfig().getString("messages.console-only-command", "&cEste comando só pode ser executado por um jogador.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', consoleMessage));
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

    /**
     * Lida com a lógica de reivindicação da recompensa.
     * Verifica o espaço no inventário, deposita a moeda, entrega o item
     * e atualiza o timestamp da última coleta do jogador.
     * @param player O jogador que está reivindicando a recompensa.
     * @param dailyRewardSection A seção de configuração da recompensa diária.
     */
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
     *
     * @param millis A quantidade de milissegundos a ser formatada.
     * @return Uma string representando o tempo formatado.
     */
    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    /**
     * Envia uma mensagem configurável para o jogador, buscando o texto no `config.yml`.
     * Permite a substituição de placeholders na mensagem.
     *
     * @param player O jogador que receberá a mensagem.
     * @param section A seção de configuração onde a mensagem está localizada.
     * @param path O caminho da mensagem dentro da seção.
     * @param defaultValue A mensagem padrão a ser usada se o caminho não for encontrado.
     * @param replacements Um varargs de pares de strings (placeholder, valor) para substituição.
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