package com.magnocat.mctrilhas.npc;

import java.util.List;
import java.util.Map;

/**
 * Representa uma única tela de diálogo com o texto do NPC e as escolhas do jogador.
 * <p>
 * Esta classe é um "record", uma forma concisa de criar uma classe imutável
 * para armazenar dados.
 *
 * @param id O identificador único deste diálogo.
 * @param npcText As linhas de texto que o NPC irá dizer.
 * @param choices Um mapa das escolhas disponíveis para o jogador.
 */
public record Dialogue(
        String id,
        List<String> npcText,
        Map<Integer, DialogueChoice> choices
) {}