package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.event.*;
import org.bukkit.*;

public class GamemodeGUI extends CustomGUI {
    private final PlayerServers pl;

    public GamemodeGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    private void resetSelected(Inventory inventory) {
        ItemStack survival = getItem("survival");
        ItemStack creative = getItem("creative");
        ItemStack adventure = getItem("adventure");
        ItemStack spectator = getItem("spectator");

        inventory.clear(11);
        inventory.clear(12);
        inventory.clear(14);
        inventory.clear(15);

        // Get the current game mode and set the selected item
        switch (Bukkit.getDefaultGameMode()) {
            case SURVIVAL -> setSelected(survival);
            case CREATIVE -> setSelected(creative);
            case ADVENTURE -> setSelected(adventure);
            case SPECTATOR -> setSelected(spectator);
        }

        // Set the items in the inventory
        inventory.setItem(11, survival);
        inventory.setItem(12, creative);
        inventory.setItem(14, adventure);
        inventory.setItem(15, spectator);
    }

    @Override
    public void open(Player player, Inventory inventory) {
        inventory = reopenInventory(player, inventory, 3, getTitle());
        if (inventory == null) {
            return;
        }

        fillInventory(inventory);  // Fill the inventory with items
        addBackButtons(inventory);  // Add back buttons using CustomGUI
        resetSelected(inventory);  // Set selected game mode items

        // Handle the force-gamemode setting
        if (pl.settingsManager.getSetting("force-gamemode").equalsIgnoreCase("true")) {
            inventory.setItem(4, getItem("force-gamemode-on"));
        } else {
            inventory.setItem(4, getItem("force-gamemode-off"));
        }

        player.openInventory(inventory);  // Open the inventory for the player
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        String inventoryTitle = pl.utils.stripColor(event.getView().getTitle());
        Inventory inventory = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        if (inventoryTitle.equalsIgnoreCase(pl.utils.stripColor(getTitle()))) {
            GuiClickEvent guiClickEvent = new GuiClickEvent(pl, player, inventory, inventoryTitle, currentItem);
            Bukkit.getPluginManager().callEvent(guiClickEvent);

            if (!guiClickEvent.isCancelled()) {
                event.setCancelled(true);

                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    closeInventory(player);
                    return;
                }

                handleGamemodeSelection(player, inventory, currentItem);
            }
        }
    }

    private void handleGamemodeSelection(Player player, Inventory inventory, ItemStack currentItem) {
        // Survival mode
        if (getItem("survival").equals(currentItem)) {
            changeGamemode(player, inventory, 0, "SURVIVAL");
        }
        // Creative mode
        else if (getItem("creative").equals(currentItem)) {
            changeGamemode(player, inventory, 1, "CREATIVE");
        }
        // Adventure mode
        else if (getItem("adventure").equals(currentItem)) {
            changeGamemode(player, inventory, 2, "ADVENTURE");
        }
        // Spectator mode
        else if (getItem("spectator").equals(currentItem)) {
            changeGamemode(player, inventory, 3, "SPECTATOR");
        }
        // Force gamemode toggle
        else if (getItem("force-gamemode-on").equals(currentItem)) {
            toggleForceGamemode(player, inventory, false);
        } else if (getItem("force-gamemode-off").equals(currentItem)) {
            toggleForceGamemode(player, inventory, true);
        }
        // Back button
        else if (getBackButton().equals(currentItem)) {
            pl.gui.getGUI("settings").open(player, inventory);
        } else if (!getFillItem().equals(currentItem)) {
            closeInventory(player);
        }
    }

    private void changeGamemode(Player player, Inventory inventory, int gamemodeValue, String gamemodeName) {
        pl.settingsManager.setGamemode(gamemodeValue);
        pl.utils.sendMsg(player, pl.messages.get("gamemode-changed").replaceAll("%gamemode%", gamemodeName));
        player.getServer().getOnlinePlayers().forEach(user -> {
        	user.setGameMode(GameMode.getByValue(gamemodeValue));
        });
        resetSelected(inventory);
    }

    private void toggleForceGamemode(Player player, Inventory inventory, boolean enable) {
        String settingValue = enable ? "true" : "false";
        String messageKey = enable ? "force-gamemode-on" : "force-gamemode-off";
        String itemKey = enable ? "force-gamemode-on" : "force-gamemode-off";

        pl.settingsManager.changeSetting("force-gamemode", settingValue);
        inventory.setItem(4, getItem(itemKey));
        pl.utils.sendMsg(player, pl.messages.get(messageKey));
    }
}
