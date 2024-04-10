package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.event.*;
import java.util.logging.*;
import org.bukkit.inventory.meta.*;
import java.util.*;

public class ControlGUI extends CustomGUI
{
    PlayerServers pl;
    
    public ControlGUI(PlayerServers pl) {
        this.pl = pl;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI, int n, HashMap hashMap) {
        reopenGUI = this.reopenGUI(player, reopenGUI, 3, this.getTitle());
        if (reopenGUI == null) {
            return;
        }
        this.fill(reopenGUI);
        this.pl.utils.debug("serverMap: " + hashMap.toString());
        boolean equalsIgnoreCase = hashMap.get("has-server").equals("true");
        if (equalsIgnoreCase && hashMap.get("use-expire").equals("true")) {
            reopenGUI.setItem(4, this.buildExpireTracker(hashMap));
        }
        if (equalsIgnoreCase && hashMap.get("files-exist").equals("true")) {
            if (hashMap.get("is-online").equals("true")) {
                reopenGUI.setItem(13, this.getItem("stop-server"));
                if (!this.pl.utils.getOwnerId().equals(player.getUniqueId().toString())) {
                    reopenGUI.setItem(12, this.getItem("join-server"));
                }
                else {
                    reopenGUI.setItem(12, this.getItem("leave-server"));
                }
            }
            else {
                reopenGUI.setItem(13, this.getItem("start-server"));
            }
            reopenGUI.setItem(14, this.getItem("delete-server"));
        }
        else {
            reopenGUI.setItem(13, this.getItem("create-server"));
        }
    }
    
    @EventHandler
    @Override
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        String stripColor = this.pl.utils.stripColor(inventoryClickEvent.getView().getTitle());
        Inventory inventory = inventoryClickEvent.getInventory();
        ItemStack currentItem = inventoryClickEvent.getCurrentItem();
        Player player = (Player)inventoryClickEvent.getWhoClicked();
        String string = player.getUniqueId().toString();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, stripColor, currentItem);
        if (stripColor.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
            Bukkit.getPluginManager().callEvent(guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    this.close(player);
                }
                else if (this.getItem("create-server").equals(currentItem)) {
                    this.pl.psCmd.psCreate(string, "");
                    this.close(player);
                }
                else if (this.getItem("delete-server").equals(currentItem)) {
                    inventory.setItem(14, this.getItem("delete-confirm"));
                }
                else if (this.getItem("delete-confirm").equals(currentItem)) {
                    this.pl.psCmd.psDelete(string, "confirm");
                    this.close(player);
                }
                else if (this.getItem("start-server").equals(currentItem) || this.getItem("join-server").equals(currentItem)) {
                    this.pl.psCmd.psHome(string);
                    this.close(player);
                }
                else if (this.getItem("stop-server").equals(currentItem)) {
                    this.pl.psCmd.psStop(string);
                    this.close(player);
                }
                else if (this.getItem("leave-server").equals(currentItem)) {
                    this.pl.psCmd.psLeave(string);
                    this.close(player);
                }
                else if (this.getFillItem().equals(currentItem) || this.getItem("expire-tracker").getType().equals(currentItem.getType())) {
                    inventory.setItem(14, this.getItem("delete-server"));
                }
                else {
                    this.close(player);
                }
            }
        }
    }
    
    public String doPlaceholders(HashMap<String, String> hashMap, String s) {
        try {
            if (s.contains("%expire-date%")) {
                s = s.replaceAll("%expire-date%", hashMap.get("expire-date"));
            }
            if (s.contains("%time-left%")) {
                s = s.replaceAll("%time-left%", hashMap.get("time-left"));
            }
        }
        catch (NullPointerException ex) {
            this.pl.utils.log(Level.WARNING, "Tried to replace a placeholder in message, but the value was null! Please send this stack trace to the developer!");
            this.pl.utils.log(Level.WARNING, "input: " + s);
            ex.printStackTrace();
        }
        return s;
    }
    
    public ItemStack buildExpireTracker(HashMap<String, String> hashMap) {
        ItemStack item = this.getItem("expire-tracker");
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(this.doPlaceholders(hashMap, itemMeta.getDisplayName()));
        ArrayList<String> lore = new ArrayList<String>();
        Iterator<String> iterator = itemMeta.getLore().iterator();
        while (iterator.hasNext()) {
            lore.add(this.pl.utils.color(this.doPlaceholders(hashMap, iterator.next())));
        }
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }
}
