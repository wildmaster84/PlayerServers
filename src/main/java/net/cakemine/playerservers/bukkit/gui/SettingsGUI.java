package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import java.util.logging.*;
import org.bukkit.inventory.meta.*;
import java.util.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class SettingsGUI extends CustomGUI {
    private final PlayerServers pl;

    public SettingsGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    public String doPlaceholders(String s) {
        try {
            if (s.contains("%expire-date%")) {
                s = s.replace("%expire-date%", this.pl.expireDate);
            }
            if (s.contains("%time-left%")) {
                s = s.replace("%time-left%", this.pl.utils.timeLeft());
            }
        } catch (NullPointerException ex) {
            this.pl.utils.log(Level.WARNING, "Tried to replace a placeholder in message, but the value was null! Please send this stack trace to the developer!");
            this.pl.utils.log(Level.WARNING, "input: " + s);
            ex.printStackTrace();
        }
        return s;
    }

    public ItemStack buildExpireTracker() {
        ItemStack item = this.getItem("expire-tracker");
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(this.doPlaceholders(itemMeta.getDisplayName()));

        List<String> lore = itemMeta.getLore();
        List<String> updatedLore = new ArrayList<>();
        for (String line : lore) {
            updatedLore.add(this.pl.utils.color(this.doPlaceholders(line)));
        }
        itemMeta.setLore(updatedLore);
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    public void open(Player player, Inventory reopenGUI) {
        reopenGUI = this.reopenInventory(player, reopenGUI, 4, this.getTitle());
        if (reopenGUI == null) {
            return;
        }

        this.fillInventory(reopenGUI);  // Using the new fillInventory method

        if (this.pl.useExpire) {
            reopenGUI.setItem(4, this.buildExpireTracker());
        }

        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.gamemode")) {
            reopenGUI.setItem(12, this.getItem("gamemode"));
        }
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.difficulty")) {
            reopenGUI.setItem(13, this.getItem("difficulty"));
        }
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.pvp")) {
            if (Bukkit.getWorlds().iterator().next().getPVP()) {
                reopenGUI.setItem(14, this.getItem("pvp-on"));
            } else {
                reopenGUI.setItem(14, this.getItem("pvp-off"));
            }
        }
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.whitelist")) {
            reopenGUI.setItem(21, this.getItem("whitelist"));
        }
        if (player.hasPermission("playerservers.myserver") ||
            player.hasPermission("playerservers.myserver.whitelist") ||
            player.hasPermission("playerservers.myserver.kick") ||
            player.hasPermission("playerservers.myserver.ban")) {
            reopenGUI.setItem(22, this.getItem("player-manager"));
        }
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.worldsettings")) {
            reopenGUI.setItem(23, this.getItem("world-settings"));
        }
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
    	String guiTitle = pl.utils.stripColor(pl.utils.color(inventoryClickEvent.getView().getTitle()));
        String title = pl.utils.stripColor(pl.utils.color(getTitle()));
        Inventory inventory = inventoryClickEvent.getInventory();
        ItemStack currentItem = inventoryClickEvent.getCurrentItem();
        Player player = (Player) inventoryClickEvent.getWhoClicked();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, guiTitle, currentItem);

        if (guiTitle.equalsIgnoreCase(this.pl.utils.stripColor(title))) {
            Bukkit.getPluginManager().callEvent(guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);

                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                	player.closeInventory();
                } else if (this.getItem("gamemode").equals(currentItem)) {
                    this.pl.gui.getGUI("gamemode").open(player, inventory);
                } else if (this.getItem("difficulty").equals(currentItem)) {
                    this.pl.gui.getGUI("difficulty").open(player, inventory);
                } else if (this.getItem("pvp-on").equals(currentItem)) {
                    this.pl.settingsManager.setPvP(false);
                    inventory.setItem(14, this.getItem("pvp-off"));
                    this.pl.utils.broadcast(this.pl.messages.get("pvp-disabled"));
                } else if (this.getItem("pvp-off").equals(currentItem)) {
                    this.pl.settingsManager.setPvP(true);
                    inventory.setItem(14, this.getItem("pvp-on"));
                    this.pl.utils.broadcast(this.pl.messages.get("pvp-enabled"));
                } else if (this.getItem("whitelist").equals(currentItem)) {
                    this.pl.gui.getGUI("whitelist").open(player, inventory);
                } else if (this.getItem("player-manager").equals(currentItem)) {
                    this.pl.gui.getGUI("player-manager").open(player, inventory, 1);
                } else if (this.getItem("world-settings").equals(currentItem)) {
                    this.pl.gui.getGUI("world-settings").open(player, inventory);
                } else if (!this.getFillItem().equals(currentItem)) {
                    if (!this.buildExpireTracker().equals(currentItem)) {
                    	player.closeInventory();
                    }
                }
            }
        }
    }
}
