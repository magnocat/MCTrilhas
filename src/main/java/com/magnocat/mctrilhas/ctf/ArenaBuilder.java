package com.magnocat.mctrilhas.ctf;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * Armazena os dados de uma arena de CTF durante o processo de criação.
 */
public class ArenaBuilder {

    private final String id;
    private String name;
    private final Map<String, Location> locations = new HashMap<>();

    public ArenaBuilder(String id) {
        this.id = id;
        this.name = id; // Nome padrão é o ID, pode ser alterado
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String key, Location location) {
        locations.put(key.toLowerCase(), location);
    }

    public Location getLocation(String key) {
        return locations.get(key.toLowerCase());
    }

    public Map<String, Location> getLocations() {
        return locations;
    }

    /**
     * Verifica se todos os pontos necessários da arena foram definidos.
     * @return true se a arena estiver completa, false caso contrário.
     */
    public boolean isComplete() {
        return locations.containsKey("lobby") &&
               locations.containsKey("red-spawn") && // Corrigido para usar hífen
               locations.containsKey("blue-spawn") && // Corrigido para usar hífen
               locations.containsKey("red-flag") &&   // Corrigido para usar hífen
               locations.containsKey("blue-flag");  // Corrigido para usar hífen
    }

    /**
     * Retorna uma string com o status da construção da arena.
     * @return Uma string formatada com o status.
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("§6--- Status da Arena '§e").append(id).append("§6' ---\n");
        status.append(check("Lobby", locations.containsKey("lobby"))); // Mantido
        status.append(check("Spawn Vermelho", locations.containsKey("red-spawn"))); // Corrigido
        status.append(check("Spawn Azul", locations.containsKey("blue-spawn")));   // Corrigido
        status.append(check("Bandeira Vermelha", locations.containsKey("red-flag"))); // Corrigido
        status.append(check("Bandeira Azul", locations.containsKey("blue-flag")));   // Corrigido
        if (isComplete()) {
            status.append("\n§aArena pronta para ser salva! Use /ctf admin save");
        } else {
            status.append("\n§eFaltam pontos a serem definidos.");
        }
        return status.toString();
    }

    private String check(String name, boolean isSet) {
        return "§7- " + name + ": " + (isSet ? "§a✔ Definido" : "§c✖ Não definido") + "\n";
    }
}