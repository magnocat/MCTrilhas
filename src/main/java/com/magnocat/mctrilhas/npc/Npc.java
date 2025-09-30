package com.magnocat.mctrilhas.npc;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Representa os dados persistentes de um NPC.
 * <p>
 * Esta classe é um "record", uma forma concisa de criar uma classe imutável
 * para armazenar dados.
 *
 * @param id O identificador único e textual do NPC (ex: "chefe_escoteiro").
 * @param name O nome de exibição do NPC, com suporte a códigos de cor.
 * @param location A localização exata onde o NPC deve ser gerado.
 * @param skinTexture A textura da skin do NPC, em formato Base64.
 * @param skinSignature A assinatura da skin do NPC, em formato Base64.
 * @param startDialogueId O ID do diálogo inicial para este NPC.
 * @param entityId O UUID da entidade viva no mundo. Este valor é transitório e
 *                 muda a cada reinicialização do servidor.
 */
public record Npc(
        String id,
        String name,
        Location location,
        String skinTexture,
        String skinSignature,
        String startDialogueId,
        UUID entityId
) {}