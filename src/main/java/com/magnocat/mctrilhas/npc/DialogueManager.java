package com.magnocat.mctrilhas.npc;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia o carregamento, o cache e a apresentação dos diálogos.
 * <p>
 * Esta classe lê o arquivo `dialogues.yml`, armazena todos os diálogos em
 * memória para acesso rápido e fornecerá os métodos para iniciar uma
 * conversa com um jogador.
 */
public class DialogueManager {

    private final MCTrilhasPlugin plugin;
    private final Map<String, Dialogue> dialogueCache = new HashMap<>(); // Use o record Dialogue
    private final DialogueMenu dialogueMenu;

    public DialogueManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.dialogueMenu = new DialogueMenu(plugin);
        loadDialogues();
    }

    /**
     * Carrega todos os diálogos do arquivo dialogues.yml para o cache.
     */
    private void loadDialogues() {
        File dialoguesFile = new File(plugin.getDataFolder(), "dialogues.yml");
        if (!dialoguesFile.exists()) {
            plugin.saveResource("dialogues.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(dialoguesFile);

        // Limpa o cache para suportar recarregamentos do plugin.
        dialogueCache.clear();

        ConfigurationSection dialoguesSection = config.getConfigurationSection("dialogues");
        if (dialoguesSection == null) {
            plugin.logInfo("Nenhuma seção 'dialogues' encontrada em dialogues.yml.");
            return;
        }

        for (String dialogueId : dialoguesSection.getKeys(false)) {
            List<String> npcText = dialoguesSection.getStringList(dialogueId + ".npc-text");

            Map<Integer, DialogueChoice> choices = new HashMap<>();
            ConfigurationSection choicesSection = dialoguesSection.getConfigurationSection(dialogueId + ".choices");

            if (choicesSection != null) {
                for (String choiceKey : choicesSection.getKeys(false)) {
                    try {
                        int choiceNumber = Integer.parseInt(choiceKey);
                        String choiceText = choicesSection.getString(choiceKey + ".text");
                        String choiceAction = choicesSection.getString(choiceKey + ".action");

                        if (choiceText != null && choiceAction != null) {
                            choices.put(choiceNumber, new DialogueChoice(choiceText, choiceAction));
                        }
                    } catch (NumberFormatException e) {
                        plugin.logWarn("Chave de escolha inválida '" + choiceKey + "' no diálogo '" + dialogueId + "'. A chave deve ser um número.");
                    }
                }
            }

            Dialogue dialogue = new Dialogue(dialogueId, npcText, choices);
            dialogueCache.put(dialogueId.toLowerCase(), dialogue);
        }

        plugin.logInfo(dialogueCache.size() + " diálogos carregados.");
    }

    public Dialogue getDialogue(String id) {
        return dialogueCache.get(id.toLowerCase());
    }

    /**
     * Inicia uma conversa com um jogador, abrindo a GUI de diálogo.
     *
     * @param player O jogador.
     * @param dialogueId O ID do diálogo a ser exibido.
     */
    public void startDialogue(Player player, String dialogueId) {
        Dialogue dialogue = getDialogue(dialogueId);
        if (dialogue == null) {
            player.sendMessage(ChatColor.RED + "Este personagem ainda não tem nada a dizer.");
            plugin.logWarn("Tentativa de iniciar diálogo inexistente: '" + dialogueId + "'");
            return;
        }
        dialogueMenu.open(player, dialogue);
    }
}