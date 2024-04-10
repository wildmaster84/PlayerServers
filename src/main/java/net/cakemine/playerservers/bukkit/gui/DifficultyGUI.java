package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import java.util.*;
import org.bukkit.event.*;

public class DifficultyGUI extends CustomGUI
{
    PlayerServers pl;
    
    public DifficultyGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    private void resetSelected(Inventory inventory) {
        ItemStack selected = new ItemStack(this.getItem("peaceful"));
        ItemStack selected2 = new ItemStack(this.getItem("easy"));
        ItemStack selected3 = new ItemStack(this.getItem("normal"));
        ItemStack selected4 = new ItemStack(this.getItem("hard"));
        inventory.clear(11);
        inventory.clear(13);
        inventory.clear(14);
        inventory.clear(15);
        switch (Bukkit.getWorlds().iterator().next().getDifficulty().ordinal()) {
            case 0: {
                this.setSelected(selected);
                break;
            }
            case 1: {
                this.setSelected(selected2);
                break;
            }
            case 2: {
                this.setSelected(selected3);
                break;
            }
            case 3: {
                this.setSelected(selected4);
                break;
            }
        }
        inventory.setItem(11, selected);
        inventory.setItem(13, selected2);
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
            Bukkit.getPluginManager().callEvent(guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    this.close(player);
                }
                else {
                    if (this.getItem("peaceful").equals(currentItem)) {
                        Iterator<World> iterator = Bukkit.getWorlds().iterator();
                        while (iterator.hasNext()) {
                            iterator.next().setDifficulty(Difficulty.PEACEFUL);
                        }
                        this.pl.settingsManager.changeSetting("difficulty", "0");
                        this.pl.utils.sendMsg(player, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.PEACEFUL.name()));
                    }
                    else if (this.getItem("easy").equals(currentItem)) {
                        Iterator<World> iterator2 = Bukkit.getWorlds().iterator();
                        while (iterator2.hasNext()) {
                            iterator2.next().setDifficulty(Difficulty.EASY);
                        }
                        this.pl.settingsManager.changeSetting("difficulty", "1");
                        this.pl.utils.sendMsg(player, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.EASY.name()));
                    }
                    else if (this.getItem("normal").equals(currentItem)) {
                        Iterator<World> iterator3 = Bukkit.getWorlds().iterator();
                        while (iterator3.hasNext()) {
                            iterator3.next().setDifficulty(Difficulty.NORMAL);
                        }
                        this.pl.settingsManager.changeSetting("difficulty", "2");
                        this.pl.utils.sendMsg(player, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.NORMAL.name()));
                    }
                    else if (this.getItem("hard").equals(currentItem)) {
                        Iterator<World> iterator4 = Bukkit.getWorlds().iterator();
                        while (iterator4.hasNext()) {
                            iterator4.next().setDifficulty(Difficulty.HARD);
                        }
                        this.pl.settingsManager.changeSetting("difficulty", "3");
                        this.pl.utils.sendMsg(player, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.HARD.name()));
                    }
                    else {
                        if (this.getBackButton().equals(currentItem)) {
                            this.pl.gui.getGUI("settings").open(player, inventory);
                            return;
                        }
                        this.close(player);
                        return;
                    }
                    this.resetSelected(inventory);
                }
            }
        }
    }
}
