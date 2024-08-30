package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.event.*;
import java.util.*;

public class DifficultyGUI extends CustomGUI {
    private final PlayerServers pl;

    public DifficultyGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    private void resetSelected(Inventory inventory) {
        ItemStack peaceful = getItem("peaceful");
        ItemStack easy = getItem("easy");
        ItemStack normal = getItem("normal");
        ItemStack hard = getItem("hard");

        inventory.clear(11);
        inventory.clear(13);
        inventory.clear(14);
        inventory.clear(15);

        // Get the current difficulty and set the selected item
        switch (Bukkit.getWorlds().iterator().next().getDifficulty()) {
            case PEACEFUL -> setSelected(peaceful);
            case EASY -> setSelected(easy);
            case NORMAL -> setSelected(normal);
            case HARD -> setSelected(hard);
        }

        // Set the items in the inventory
        inventory.setItem(11, peaceful);
        inventory.setItem(13, easy);
        inventory.setItem(14, normal);
        inventory.setItem(15, hard);
    }

    @Override
    public void open(Player player, Inventory inventory) {
        inventory = reopenInventory(player, inventory, 3, getTitle());
        if (inventory == null) {
            return;
        }

        fillInventory(inventory);
        addBackButtons(inventory);
        resetSelected(inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
    	InventoryView view = event.getView();
        String inventoryTitle = pl.utils.stripColor(view.getTitle());
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

                handleDifficultyChange(player, inventory, currentItem);
            }
        }
    }

    private void handleDifficultyChange(Player player, Inventory inventory, ItemStack currentItem) {
        // Peaceful difficulty
        if (getItem("peaceful").equals(currentItem)) {
            setWorldDifficulty(Difficulty.PEACEFUL, player, "0");
        }
        // Easy difficulty
        else if (getItem("easy").equals(currentItem)) {
            setWorldDifficulty(Difficulty.EASY, player, "1");
        }
        // Normal difficulty
        else if (getItem("normal").equals(currentItem)) {
            setWorldDifficulty(Difficulty.NORMAL, player, "2");
        }
        // Hard difficulty
        else if (getItem("hard").equals(currentItem)) {
            setWorldDifficulty(Difficulty.HARD, player, "3");
        }
        // Back button
        else if (getBackButton().equals(currentItem)) {
            pl.gui.getGUI("settings").open(player, inventory);
        } else {
            closeInventory(player);
        }

        // Reset the selected difficulty after the change
        resetSelected(inventory);
    }

    private void setWorldDifficulty(Difficulty difficulty, Player player, String difficultyValue) {
        // Set the difficulty for all worlds
        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(difficulty);
        }

        // Update the difficulty setting and send a message to the player
        pl.settingsManager.changeSetting("difficulty", difficultyValue);
        pl.utils.sendMsg(player, pl.messages.get("difficulty-changed").replace("%difficulty%", difficulty.name()));
    }
}
