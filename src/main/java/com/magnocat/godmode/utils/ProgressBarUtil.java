package com.magnocat.godmode.utils;

import org.bukkit.ChatColor;

/**
 * A utility class for creating text-based progress bars.
 */
@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public final class ProgressBarUtil {

    private ProgressBarUtil() {
        // Utility class, not meant to be instantiated.
    }

    /**
     * Builds a string representation of a progress bar.
     * @param current The current progress value.
     * @param max The maximum progress value.
     * @return A formatted string representing the progress bar.
     */
    public static String buildProgressBar(int current, int max) {
        if (max <= 0) max = 1; // Avoid division by zero
        double percentage = Math.min(1.0, (double) current / max);

        int totalBars = 10;
        int progressBars = (int) (totalBars * percentage);

        return ChatColor.GREEN + "■".repeat(progressBars) + ChatColor.GRAY + "■".repeat(totalBars - progressBars);
    }
}