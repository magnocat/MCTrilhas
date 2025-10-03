package com.magnocat.mctrilhas.commands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

/**
 * Define o contrato padrão para todos os subcomandos do plugin.
 * <p>
 * Esta interface garante que cada subcomando tenha uma estrutura consistente,
 * facilitando o gerenciamento, o registro e a execução pelo roteador de comandos principal.
 */
public interface SubCommand {

    /**
     * @return O nome do subcomando (ex: "addbadge", "progress").
     */
    String getName();

    /**
     * @return Uma breve descrição do que o subcomando faz.
     */
    String getDescription();

    /**
     * @return A sintaxe de uso correta do subcomando (ex: "/scout admin addbadge <jogador> <insignia>").
     */
    String getSyntax();

    /**
     * @return A permissão necessária para executar este subcomando.
     */
    String getPermission();

    /**
     * @return {@code true} se for um subcomando de administração (parte de /scout admin), {@code false} caso contrário.
     */
    boolean isAdminCommand();

    /**
     * Executa a lógica principal do subcomando.
     *
     * @param sender A entidade que executou o comando (jogador ou console).
     * @param args Os argumentos fornecidos após o nome do subcomando.
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Verifica se o módulo necessário para este comando está ativo.
     * A implementação padrão retorna {@code true}, assumindo que o módulo está sempre ativo.
     * Subcomandos que dependem de um módulo específico devem sobrescrever este método.
     *
     * @param plugin A instância principal do plugin.
     * @return {@code true} se o módulo estiver ativo, {@code false} caso contrário.
     */
    default boolean isModuleEnabled(MCTrilhasPlugin plugin) { return true; }

    /**
     * Fornece sugestões de autocompletar para os argumentos do subcomando.
     * A implementação padrão retorna uma lista vazia.
     */
    default List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}