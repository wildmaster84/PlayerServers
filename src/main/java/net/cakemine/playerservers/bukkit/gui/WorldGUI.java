package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.event.*;

public class WorldGUI extends CustomGUI
{
    PlayerServers pl;
    
    public WorldGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI) {
        reopenGUI = this.reopenInventory(player, reopenGUI, 3, this.getTitle());
        if (reopenGUI == null) {
            return;
        }
        this.fillInventory(reopenGUI);
        this.addBackButtons(reopenGUI);
        if (Bukkit.getAllowNether()) {
            reopenGUI.setItem(11, this.getItem("allow-nether-on"));
        }
        else {
            reopenGUI.setItem(11, this.getItem("allow-nether-off"));
        }
        if (Bukkit.getAllowFlight()) {
            reopenGUI.setItem(12, this.getItem("allow-flight-on"));
        }
        else {
            reopenGUI.setItem(12, this.getItem("allow-flight-off"));
        }
        if (Bukkit.getGenerateStructures()) {
            reopenGUI.setItem(13, this.getItem("generate-structures-on"));
        }
        else {
            reopenGUI.setItem(13, this.getItem("generate-structures-off"));
        }
        reopenGUI.setItem(14, this.getItem("mob-settings"));
    }
    
    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
    	InventoryView view = inventoryClickEvent.getView();
        String stripColor = this.pl.utils.stripColor(view.getTitle());
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
                else if (this.getItem("allow-nether-on").equals(currentItem)) {
                    this.pl.settingsManager.changeSetting("allow-nether", "false");
                    inventory.setItem(11, this.getItem("allow-nether-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("allow-nether-off"));
                }
                else if (this.getItem("allow-nether-off").equals(currentItem)) {
                    this.pl.settingsManager.changeSetting("allow-nether", "true");
                    inventory.setItem(11, this.getItem("allow-nether-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("allow-nether-on"));
                }
                else if (this.getItem("allow-flight-on").equals(currentItem)) {
                    this.pl.settingsManager.changeSetting("allow-flight", "false");
                    inventory.setItem(12, this.getItem("allow-flight-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("allow-flight-off"));
                }
                else if (this.getItem("allow-flight-off").equals(currentItem)) {
                    this.pl.settingsManager.changeSetting("allow-flight", "true");
                    inventory.setItem(12, this.getItem("allow-flight-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("allow-flight-on"));
                }
                else if (this.getItem("generate-structures-on").equals(currentItem)) {
                    this.pl.settingsManager.changeSetting("generate-structures", "false");
                    inventory.setItem(13, this.getItem("generate-structures-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("generate-structures-off"));
                }
                else if (this.getItem("generate-structures-off").equals(currentItem)) {
                    this.pl.settingsManager.changeSetting("generate-structures", "true");
                    inventory.setItem(13, this.getItem("generate-structures-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("generate-structures.on"));
                }
                else if (this.getItem("mob-settings").equals(currentItem)) {
                    this.pl.gui.getGUI("mob-settings").open(player, inventory);
                }
                else if (this.getBackButton().equals(currentItem)) {
                    this.pl.gui.getGUI("settings").open(player, inventory);
                }
                else if (!this.getFillItem().equals(currentItem)) {
                	player.closeInventory();
                }
            }
        }
    }
}
