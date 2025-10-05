package com.magnocat.mctrilhas.chat;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

/**
 * Gerencia as configurações e a lógica do sistema de chat por proximidade.
 */
public class ProximityChatManager {

    private final boolean enabled;
    private final int radius;
    private final String format;

    public ProximityChatManager(MCTrilhasPlugin plugin) {
        this.enabled = plugin.getConfig().getBoolean("proximity-chat.enabled", false);
        this.radius = plugin.getConfig().getInt("proximity-chat.radius", 50);
        this.format = plugin.getConfig().getString("proximity-chat.format", "&7[P] &r{player}: &f{message}");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRadius() {
        return radius;
    }

    public String getFormat() {
        return format;
    }
}