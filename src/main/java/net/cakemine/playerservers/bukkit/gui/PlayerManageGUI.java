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

public class PlayerManageGUI extends CustomGUI {

    private final PlayerServers pl;

    public PlayerManageGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    @Override
    public void open(Player player, Inventory inventory, int page) {
        int size = Bukkit.getOnlinePlayers().size();
        int rows = calculateRows(size);
        boolean paginated = size > 45;
        int startIndex = (page - 1) * 44;

        inventory = this.reopenInventory(player, inventory, rows, this.getTitle());
        this.fillInventory(player, inventory, 14);

        if (size == 1) {
            inventory.setItem(4, this.getItem("nobody-online"));
            return;
        }

        if (paginated) {
            setupPaginationButtons(inventory, page, size);
        }

        List<Player> players = getPlayersExcept(player);
        populateInventoryWithPlayers(inventory, players, startIndex, paginated ? 44 : 53);
    }

    private int calculateRows(int size) {
        if (size <= 8) return 1;
        if (size <= 18) return 2;
        if (size <= 27) return 3;
        if (size <= 36) return 4;
        if (size <= 45) return 5;
        return 6;
    }

    private void setupPaginationButtons(Inventory inventory, int page, int size) {
        if (page > 1) {
            inventory.setItem(45, this.buildArrow(false, page - 1));
        }
        if (size > page * 44) {
            inventory.setItem(53, this.buildArrow(true, page + 1));
        }
    }

    private List<Player> getPlayersExcept(Player player) {
        List<Player> players = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                players.add(onlinePlayer);
            }
        }
        this.pl.utils.debug("Player list size: " + players.size());
        return players;
    }

    private void populateInventoryWithPlayers(Inventory inventory, List<Player> players, int startIndex, int maxItems) {
        int index = 0;
        for (int i = startIndex; i < players.size() && index < maxItems; i++, index++) {
            Player targetPlayer = players.get(i);
            inventory.setItem(index, this.buildPlayer(targetPlayer));
        }
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
    	InventoryView view = event.getView();
        String guiTitle = pl.utils.stripColor(pl.utils.color(view.getTitle()));
        String title = pl.utils.stripColor(pl.utils.color(getTitle()));
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, guiTitle, clickedItem);

        if (guiTitle.equalsIgnoreCase(title)) {
            Bukkit.getPluginManager().callEvent(guiClickEvent);

            if (!guiClickEvent.isCancelled()) {
                event.setCancelled(true);
                handleItemClick(player, inventory, clickedItem);
            }
        }
    }

    private void handleItemClick(Player player, Inventory inventory, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
            this.closeInventory(player);
            return;
        }

        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        if (this.getListedPlayers().containsKey(itemName)) {
            SkullMeta skullMeta = (SkullMeta) clickedItem.getItemMeta();
            Player targetPlayer = (Player) skullMeta.getOwningPlayer();
            this.pl.utils.debug("Opening player GUI for: " + targetPlayer.getName());
            this.pl.gui.getGUI("player").open(player, inventory, targetPlayer);
        } else if (isPaginationButton(clickedItem)) {
            handlePagination(player, inventory, clickedItem);
        } else if (this.getBackButton().equals(clickedItem)) {
            this.pl.gui.getGUI("settings").open(player, inventory);
        } else if (!this.getFillItem().equals(clickedItem)) {
            this.closeInventory(player);
        }
    }

    private boolean isPaginationButton(ItemStack item) {
        return this.getItem("page-forward").equals(item) || this.getItem("page-back").equals(item);
    }

    private void handlePagination(Player player, Inventory inventory, ItemStack clickedItem) {
        if (!clickedItem.getItemMeta().getLore().isEmpty()) {
            int newPage = Integer.parseInt(clickedItem.getItemMeta().getLore().get(0));
            this.open(player, inventory, newPage);
        }
    }
}
