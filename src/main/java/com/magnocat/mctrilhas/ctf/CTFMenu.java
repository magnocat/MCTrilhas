package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.utils.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Cria a interface gráfica (GUI) para interagir com o sistema de CTF.
 */
public class CTFMenu {

    public static final String MENU_TITLE = ChatColor.DARK_BLUE + "Capture a Bandeira";

    /**
     * Abre o menu principal do CTF para um jogador.
     * @param player O jogador para quem o menu será aberto.
     */
    public static void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);

        // Item para entrar na fila
        ItemStack joinItem = ItemFactory.createSimple(
                Material.GREEN_WOOL,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Entrar na Fila",
                List.of(ChatColor.GRAY + "Clique para procurar uma partida.")
        );

        // Item para sair da fila
        ItemStack leaveItem = ItemFactory.createSimple(
                Material.RED_WOOL,
                ChatColor.RED + "" + ChatColor.BOLD + "Sair da Fila",
                List.of(ChatColor.GRAY + "Clique para sair da fila ou da partida atual.")
        );

        menu.setItem(11, joinItem);
        menu.setItem(15, leaveItem);

        player.openInventory(menu);
    }
}