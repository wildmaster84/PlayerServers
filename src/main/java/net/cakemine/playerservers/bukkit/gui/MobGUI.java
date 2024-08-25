package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.event.*;

public class MobGUI extends CustomGUI
{
    PlayerServers pl;
    
    public MobGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI) {
        reopenGUI = this.reopenInventory(player, reopenGUI, 3, this.getTitle());
        if (reopenGUI == null) {
            return;
        }
        this.fillInventory(player, reopenGUI, 0);
        this.addBackButtons(reopenGUI);
        if (this.pl.settingsManager.getSetting("spawn-monsters").equalsIgnoreCase("true")) {
            reopenGUI.setItem(12, this.getItem("monster-spawns-on"));
        }
        else {
            reopenGUI.setItem(12, this.getItem("monster-spawns-off"));
        }
        if (this.pl.settingsManager.getSetting("spawn-animals").equalsIgnoreCase("true")) {
            reopenGUI.setItem(13, this.getItem("animal-spawns-on"));
        }
        else {
            reopenGUI.setItem(13, this.getItem("animal-spawns-off"));
        }
        if (this.pl.settingsManager.getSetting("spawn-npcs").equalsIgnoreCase("true")) {
            reopenGUI.setItem(14, this.getItem("npc-spawns-on"));
        }
        else {
            reopenGUI.setItem(14, this.getItem("npc-spawns-off"));
        }
    }
    
    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
        String stripColor = this.pl.utils.stripColor(inventoryClickEvent.getView().getTitle());
        Inventory inventory = inventoryClickEvent.getInventory();
        ItemStack currentItem = inventoryClickEvent.getCurrentItem();
        Player player = (Player)inventoryClickEvent.getWhoClicked();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, stripColor, currentItem);
        if (stripColor.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
            Bukkit.getPluginManager().callEvent((Event)guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    player.closeInventory();
                }
                else if (this.getItem("monster-spawns-on").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("spawn-monsters", "false");
                    inventory.setItem(12, this.getItem("monster-spawns-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("monster-spawns-off"));
                }
                else if (this.getItem("monster-spawns-off").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("spawn-monsters", "true");
                    inventory.setItem(12, this.getItem("monster-spawns-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("monster-spawns-on"));
                }
                else if (this.getItem("animal-spawns-on").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("spawn-animals", "false");
                    inventory.setItem(13, this.getItem("animal-spawns-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("animal-spawns-off"));
                }
                else if (this.getItem("animal-spawns-off").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("spawn-animals", "true");
                    inventory.setItem(13, this.getItem("animal-spawns-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("animal-spawns-on"));
                }
                else if (this.getItem("npc-spawns-on").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("spawn-npcs", "false");
                    inventory.setItem(14, this.getItem("npc-spawns-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("npc-spawns-off"));
                }
                else if (this.getItem("npc-spawns-off").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("spawn-npcs", "true");
                    inventory.setItem(14, this.getItem("npc-spawns-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("npc-spawns-on"));
                }
                else if (this.getBackButton().equals((Object)currentItem)) {
                    this.pl.gui.getGUI("settings").open(player, inventory);
                }
                else if (!this.getFillItem().equals((Object)currentItem)) {
                	player.closeInventory();
                }
            }
        }
    }
}
