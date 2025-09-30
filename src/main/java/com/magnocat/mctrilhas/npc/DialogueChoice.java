package com.magnocat.mctrilhas.npc;

/**
 * Representa uma única opção de escolha em um diálogo.
 * <p>
 * Esta classe é um "record", uma forma concisa de criar uma classe imutável
 * para armazenar dados.
 *
 * @param text O texto da escolha que será exibido ao jogador.
 * @param action A ação que será executada se esta escolha for selecionada (ex: "dialogue:outro_id").
 */
public record DialogueChoice(
        String text,
        String action
) {}