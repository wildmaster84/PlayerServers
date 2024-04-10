package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import java.util.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.*;
import org.bukkit.event.*;

public class PlayerManageGUI extends CustomGUI
{
    PlayerServers pl;
    
    public PlayerManageGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI, int n) {
        int size = Bukkit.getOnlinePlayers().size();
        int n2 = 1;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        boolean b = false;
        if (size <= 8) {
            n2 = 1;
        }
        else if (size <= 18) {
            n2 = 2;
        }
        else if (size <= 27) {
            n2 = 3;
        }
        else if (size <= 36) {
            n2 = 4;
        }
        else if (size <= 45) {
            n2 = 5;
        }
        else if (size > 46) {
            n2 = 6;
            n3 = n;
            b = true;
        }
        reopenGUI = this.reopenGUI(player, reopenGUI, n2, this.getTitle());

        this.fill(player, reopenGUI, 14);
        if (size == 1) {
            reopenGUI.setItem(4, this.getItem("nobody-online"));
            return;
        }
        int n7;
        if (b) {
            if (n3 != 0) {
                n5 = n3 - 1;
            }
            if (n5 > 0) {
                n6 = n5 * 44;
                n7 = n5 * 54;
            }
            else {
                n6 = 0;
                n7 = 44;
            }
        }
        else {
            n7 = 53;
        }
        if (b && n2 == 6 && size > 54) {
            if (n - 1 > 0) {
                reopenGUI.setItem(45, this.buildArrow(false, n - 1));
            }
            reopenGUI.setItem(53, this.buildArrow(true, n + 1));
        }
        Iterator<Player> iterator = (Iterator<Player>) Bukkit.getOnlinePlayers().iterator();
        ArrayList<Player> list = new ArrayList<Player>();
        while (iterator.hasNext()) {
            Player player2 = iterator.next();
            if (!player2.equals(player)) {
                list.add(player2);
                this.pl.utils.debug("added player " + player2 + " to pList");
            }
        }
        this.pl.utils.debug("pList size = " + list.size());
        for (int n8 = 0; n8 < n7 && n8 < list.size(); ++n8) {
            this.pl.utils.debug("Loop count: " + n8 + " | slotCount: " + n4);
            if (n8 >= n6) {
                Player player3 = list.get(n8);
                this.pl.utils.debug("Processing player: " + player3);
                reopenGUI.setItem(n4, this.buildPlayer(player3));
                ++n4;
                if (b && n4 > 44) {
                    break;
                }
            }
        }
    }
    
    @EventHandler
    @Override
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        String stripColor = this.pl.utils.stripColor(inventoryClickEvent.getView().getTitle());
        Inventory inventory = inventoryClickEvent.getInventory();
        ItemStack currentItem = inventoryClickEvent.getCurrentItem();
        Player player = (Player)inventoryClickEvent.getWhoClicked();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, stripColor, currentItem);
        this.pl.utils.debug("inventory clicked: " + stripColor + " looking for: " + this.pl.utils.stripColor(this.getTitle()));
        if (stripColor.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
        	this.pl.utils.debug("clicked player manager");
            Bukkit.getPluginManager().callEvent(guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    player.closeInventory();
                }

                else if (this.getListedPlayers().containsKey(ChatColor.stripColor(currentItem.getItemMeta().getDisplayName()).replaceAll("(Kick|Confirm Ban|Ban|Add|Remove)|(:\\s)|((is|is\\snot)\\swhitelisted\\.)|(\\s)|(«)|(»)", ""))) {
                    SkullMeta skullMeta = (SkullMeta)currentItem.getItemMeta();
                    Player player2 = (Player) skullMeta.getOwningPlayer();
                    this.pl.utils.debug("opening player gui: " + player2.getName());
                    this.pl.gui.getGUI("player").open(player, inventory, player2);
                }
                else if (this.pl.utils.stripColor(this.getItem("page-forward").getItemMeta().getDisplayName()).equals(this.pl.utils.stripColor(currentItem.getItemMeta().getDisplayName())) || this.pl.utils.stripColor(this.getItem("page-back").getItemMeta().getDisplayName()).equals(this.pl.utils.stripColor(currentItem.getItemMeta().getDisplayName()))) {
                    if (!currentItem.getItemMeta().getLore().isEmpty()) {
                        this.open(player, inventory, Integer.valueOf(currentItem.getItemMeta().getLore().iterator().next()));
                    }
                }
                else if (this.getBackButton().equals(currentItem)) {
                    this.pl.gui.getGUI("settings").open(player, inventory);
                }
                else if (!this.getFillItem().equals(currentItem)) {
                	this.pl.utils.debug("didnt match item");
                    this.close(player);
                }
            }
        }
    }
}
