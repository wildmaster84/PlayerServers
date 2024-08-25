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

public class ControlGUI extends CustomGUI {
    private final PlayerServers pl;

    public ControlGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    @Override
    public void open(Player player, Inventory inventory, int rows, HashMap<String, String> serverMap) {
        inventory = reopenInventory(player, inventory, 3, getTitle());
        if (inventory == null) {
            return;
        }

        fillInventory(inventory);
        pl.utils.debug("serverMap: " + serverMap.toString());

        boolean hasServer = Boolean.parseBoolean(serverMap.getOrDefault("has-server", "false"));
        boolean useExpire = Boolean.parseBoolean(serverMap.getOrDefault("use-expire", "false"));

        if (hasServer && useExpire) {
            inventory.setItem(4, buildExpireTracker(serverMap));
        }

        if (hasServer && Boolean.parseBoolean(serverMap.getOrDefault("files-exist", "false"))) {
            if (Boolean.parseBoolean(serverMap.getOrDefault("is-online", "false"))) {
                inventory.setItem(13, getItem("stop-server"));

                if (!pl.utils.getOwnerId().equals(player.getUniqueId().toString())) {
                    inventory.setItem(12, getItem("join-server"));
                } else {
                    inventory.setItem(12, getItem("leave-server"));
                }
            } else {
                inventory.setItem(13, getItem("start-server"));
            }
            inventory.setItem(14, getItem("delete-server"));
        } else {
            inventory.setItem(13, getItem("create-server"));
        }
        player.openInventory(inventory);
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        String inventoryTitle = pl.utils.stripColor(event.getView().getTitle());
        Inventory inventory = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        String playerUUID = player.getUniqueId().toString();

        if (inventoryTitle.equalsIgnoreCase(pl.utils.stripColor(getTitle()))) {
            GuiClickEvent guiClickEvent = new GuiClickEvent(pl, player, inventory, inventoryTitle, currentItem);
            Bukkit.getPluginManager().callEvent(guiClickEvent);

            if (!guiClickEvent.isCancelled()) {
                event.setCancelled(true);

                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    closeInventory(player);
                } else if (getItem("create-server").equals(currentItem)) {
                    pl.psCmd.psCreate(playerUUID, "");
                    closeInventory(player);
                } else if (getItem("delete-server").equals(currentItem)) {
                    inventory.setItem(14, getItem("delete-confirm"));
                } else if (getItem("delete-confirm").equals(currentItem)) {
                    pl.psCmd.psDelete(playerUUID, "confirm");
                    closeInventory(player);
                } else if (getItem("start-server").equals(currentItem) || getItem("join-server").equals(currentItem)) {
                    pl.psCmd.psHome(playerUUID);
                    closeInventory(player);
                } else if (getItem("stop-server").equals(currentItem)) {
                    pl.psCmd.psStop(playerUUID);
                    closeInventory(player);
                } else if (getItem("leave-server").equals(currentItem)) {
                    pl.psCmd.psLeave(playerUUID);
                    closeInventory(player);
                } else if (getFillItem().equals(currentItem) || getItem("expire-tracker").getType().equals(currentItem.getType())) {
                    inventory.setItem(14, getItem("delete-server"));
                } else {
                    closeInventory(player);
                }
            }
        }
    }

    private String doPlaceholders(Map<String, String> serverMap, String text) {
        try {
            if (text.contains("%expire-date%")) {
                text = text.replace("%expire-date%", serverMap.get("expire-date"));
            }
            if (text.contains("%time-left%")) {
                text = text.replace("%time-left%", serverMap.get("time-left"));
            }
        } catch (NullPointerException ex) {
            pl.utils.log(Level.WARNING, "Failed to replace placeholders in message: " + text + " Exception: " + ex);
        }
        return text;
    }

    private ItemStack buildExpireTracker(Map<String, String> serverMap) {
        ItemStack item = getItem("expire-tracker");
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(doPlaceholders(serverMap, meta.getDisplayName()));

            List<String> lore = new ArrayList<>();
            for (String line : meta.getLore()) {
                lore.add(pl.utils.color(doPlaceholders(serverMap, line)));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }
}
