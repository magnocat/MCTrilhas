package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.AddBadgeSubCommand;
import com.magnocat.mctrilhas.badges.RemoveBadgeSubCommand;
import com.magnocat.mctrilhas.badges.StatsSubCommand;
import com.magnocat.mctrilhas.duels.AdminDuelSubCommand;
import com.magnocat.mctrilhas.commands.subcommands.NpcAdminSubCommand;
import com.magnocat.mctrilhas.pet.AdminPetSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Implementa o comando roteador `/scout admin`.
 * <p>
 * Esta classe não executa uma ação própria, mas gerencia e delega a execução
 * para os subcomandos de administração registrados (ex: addbadge, reload).
 * Ela também lida com a mensagem de ajuda e o autocompletar para os subcomandos.
 */
public class AdminSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // Registra todos os subcomandos de administração disponíveis.
        registerSubCommand(new AddBadgeSubCommand(plugin));
        registerSubCommand(new RemoveBadgeSubCommand(plugin));
        registerSubCommand(new StatsSubCommand(plugin));
        registerSubCommand(new ReloadSubCommand(plugin));
        registerSubCommand(new AdminPetSubCommand(plugin));
        registerSubCommand(new AdminDuelSubCommand(plugin));
        registerSubCommand(new NpcAdminSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription() {
        return "Gerencia as funções de administração do plugin.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin <comando> [argumentos]";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    /**
     * Executa o roteador de subcomandos de administração.
     * Identifica o subcomando chamado e delega a execução para a classe correspondente.
     * Se nenhum subcomando for fornecido, exibe a mensagem de ajuda.
     *
     * @param sender A entidade que executou o comando.
     * @param args Os argumentos fornecidos. O primeiro argumento deve ser o nome do subcomando.
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Comando de administrador desconhecido. Use '/scout admin' para ver a lista de comandos.");
            return;
        }

        // Remove o nome do subcomando dos argumentos para passá-los adiante
        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);
    }

    /**
     * Envia uma mensagem de ajuda listando todos os subcomandos de administração disponíveis.
     * @param sender A entidade para a qual a mensagem será enviada.
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos de Administração MCTrilhas ---");
        subCommands.values().stream()
                .sorted(Comparator.comparing(SubCommand::getName))
                .forEach(subCmd -> sender.sendMessage(ChatColor.AQUA + subCmd.getSyntax() + ChatColor.GRAY + " - " + subCmd.getDescription()));
        sender.sendMessage(ChatColor.GOLD + "-----------------------------------------");
    }

    /**
     * Retorna uma visão não modificável dos subcomandos de administração.
     * @return Um mapa dos subcomandos de admin.
     */
    public Map<String, SubCommand> getAdminSubCommands() {
        return Collections.unmodifiableMap(subCommands);
    }

    /**
     * Fornece sugestões de autocompletar para o comando de administração.
     * Se o usuário estiver digitando o nome do subcomando, sugere os nomes disponíveis.
     * Caso contrário, delega o autocompletar para o subcomando específico.
     *
     * @param sender A entidade que está tentando autocompletar o comando.
     * @param args Os argumentos atuais digitados pelo remetente.
     * @return Uma lista de sugestões para o próximo argumento.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // /scout admin <sub-comando> [argumentos...]
        // args[0] é o nome do sub-comando (ex: "addbadge")

        // Se estiver completando o primeiro argumento (o nome do sub-comando de admin)
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(partialCommand))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Se estiver completando argumentos para um sub-comando de admin específico
        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                // Delega para o sub-comando específico (ex: AddBadgeSubCommand)
                return subCommand.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Collections.emptyList();
    }
}