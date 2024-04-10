package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.event.*;
import org.bukkit.*;

public class GamemodeGUI extends CustomGUI
{
    PlayerServers pl;
    
    public GamemodeGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    private void resetSelected(Inventory inventory) {
        ItemStack selected = new ItemStack(this.getItem("creative"));
        ItemStack selected2 = new ItemStack(this.getItem("survival"));
        ItemStack selected3 = new ItemStack(this.getItem("adventure"));
        ItemStack selected4 = new ItemStack(this.getItem("spectator"));
        inventory.clear(11);
        inventory.clear(12);
        inventory.clear(14);
        inventory.clear(15);
        switch (Bukkit.getDefaultGameMode()) {
            case SURVIVAL: {
                this.setSelected(selected2);
                break;
            }
            case CREATIVE: {
                this.setSelected(selected);
                break;
            }
            case ADVENTURE: {
                this.setSelected(selected3);
                break;
            }
            case SPECTATOR: {
                this.setSelected(selected4);
                break;
            }
        }
        inventory.setItem(11, selected2);
        inventory.setItem(12, selected);
        inventory.setItem(14, selected3);
        inventory.setItem(15, selected4);
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI) {
        reopenGUI = this.reopenGUI(player, reopenGUI, 3, this.getTitle());
        if (reopenGUI == null) {
            return;
        }
        this.fill(reopenGUI);
        this.addBackButtons(reopenGUI);
        this.resetSelected(reopenGUI);
        if (this.pl.settingsManager.getSetting("force-gamemode").equalsIgnoreCase("true")) {
            reopenGUI.setItem(4, this.getItem("force-gamemode-on"));
        }
        else {
            reopenGUI.setItem(4, this.getItem("force-gamemode-off"));
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
        if (stripColor.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
            Bukkit.getPluginManager().callEvent((Event)guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    player.closeInventory();
                }
                else if (this.getItem("survival").equals((Object)currentItem)) {
                    this.pl.settingsManager.setGamemode(0);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
                    this.resetSelected(inventory);
                }
                else if (this.getItem("creative").equals((Object)currentItem)) {
                    this.pl.settingsManager.setGamemode(1);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
                    this.resetSelected(inventory);
                }
                else if (this.getItem("adventure").equals((Object)currentItem)) {
                    this.pl.settingsManager.setGamemode(2);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
                    this.resetSelected(inventory);
                }
                else if (this.getItem("spectator").equals((Object)currentItem)) {
                    this.pl.settingsManager.setGamemode(3);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
                    this.resetSelected(inventory);
                }
                else if (this.getItem("force-gamemode-on").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("force-gamemode", "false");
                    inventory.setItem(4, this.getItem("force-gamemode-off"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("force-gamemode-off"));
                }
                else if (this.getItem("force-gamemode-off").equals((Object)currentItem)) {
                    this.pl.settingsManager.changeSetting("force-gamemode", "true");
                    inventory.setItem(4, this.getItem("force-gamemode-on"));
                    this.pl.utils.sendMsg(player, this.pl.messages.get("force-gamemode-on"));
                }
                else if (this.getBackButton().equals((Object)currentItem)) {
                    this.pl.gui.getGUI("settings").open(player, inventory);
                }
                else if (!this.getFillItem().equals((Object)currentItem)) {
                    this.close(player);
                }
            }
        }
    }
}
