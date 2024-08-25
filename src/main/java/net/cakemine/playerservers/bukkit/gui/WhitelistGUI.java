package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.event.player.*;
import java.util.*;
import org.bukkit.event.*;

public class WhitelistGUI extends CustomGUI {
    private final PlayerServers pl;
    private final HashMap<String, String> whiteListMod;

    public WhitelistGUI(PlayerServers pl) {
        super(pl);
        this.whiteListMod = new HashMap<>();
        this.pl = pl;
        HandlerList.unregisterAll(this);
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    public void setWaitInput(Player player, String action) {
        this.whiteListMod.put(player.getUniqueId().toString(), action);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.pl, () -> {
            if (whiteListMod.containsKey(player.getUniqueId().toString())) {
                pl.utils.sendMsg(player, pl.messages.get("whitelist-add-timeout"));
                whiteListMod.remove(player.getUniqueId().toString());
            }
        }, 600L);
    }

    private ItemStack buildWhitelistItem() {
        ItemStack item = this.getItem("current-whitelist");
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setLore(Arrays.asList(this.whitelistString().split("\\|\\|")));
            item.setItemMeta(itemMeta);
        }
        return item;
    }

    public String whitelistString() {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        int size = Bukkit.getWhitelistedPlayers().size();
        for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
            if (count >= 10) {
                sb.append("&eAnd &o").append(size).append("&e more...");
                break;
            }
            sb.append("&e&o").append(offlinePlayer.getName()).append(" &8: &7")
                .append(offlinePlayer.getUniqueId().toString(), 0, 7).append("...||");
            count++;
            size--;
        }
        if (sb.toString().endsWith("||")) {
            sb.delete(sb.length() - 2, sb.length());
        }
        return sb.length() < 1 ? "Nobody!" : this.pl.utils.color(sb.toString());
    }

    @Override
    public void open(Player player, Inventory inventory) {
        inventory = this.reopenInventory(player, inventory, 3, this.getTitle());
        if (inventory == null) {
            return;
        }
        this.fillInventory(inventory);
        this.addBackButtons(inventory);

        if (Bukkit.hasWhitelist()) {
            inventory.setItem(11, this.getItem("whitelist-on"));
        } else {
            inventory.setItem(11, this.getItem("whitelist-off"));
        }

        inventory.setItem(12, this.buildWhitelistItem());
        inventory.setItem(13, this.getItem("add-player"));
        inventory.setItem(14, this.getItem("remove-player"));
        inventory.setItem(15, this.getItem("clear-whitelist"));
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        String inventoryTitle = this.pl.utils.stripColor(event.getView().getTitle());
        Inventory inventory = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        if (inventoryTitle.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
            GuiClickEvent guiClickEvent = new GuiClickEvent(pl, player, inventory, inventoryTitle, currentItem);
            Bukkit.getPluginManager().callEvent(guiClickEvent);

            if (!guiClickEvent.isCancelled()) {
                event.setCancelled(true);

                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    closeInventory(player);
                } else if (!this.getItem("current-whitelist").equals(currentItem)) {
                    handleWhitelistActions(player, inventory, currentItem);
                }
            }
        }
    }

    private void handleWhitelistActions(Player player, Inventory inventory, ItemStack currentItem) {
        if (this.getItem("whitelist-on").equals(currentItem)) {
            Bukkit.setWhitelist(false);
            inventory.setItem(11, this.getItem("whitelist-off"));
        } else if (this.getItem("whitelist-off").equals(currentItem)) {
            Bukkit.setWhitelist(true);
            inventory.setItem(11, this.getItem("whitelist-on"));
        } else if (this.getItem("add-player").equals(currentItem)) {
            this.setWaitInput(player, "add");
            this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-modify-instructions"));
            closeInventory(player);
        } else if (this.getItem("remove-player").equals(currentItem)) {
            this.setWaitInput(player, "remove");
            this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-modify-instructions"));
            closeInventory(player);
        } else if (this.getItem("clear-whitelist").equals(currentItem)) {
            inventory.setItem(15, this.getItem("clear-confirm"));
        } else if (this.getItem("clear-confirm").equals(currentItem)) {
            clearWhitelist(player, inventory);
        } else if (!this.getFillItem().equals(currentItem)) {
            if (this.getBackButton().equals(currentItem)) {
                this.pl.gui.getGUI("settings").open(player, inventory);
            } else {
                closeInventory(player);
            }
        }
    }

    private void clearWhitelist(Player player, Inventory inventory) {
        for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
            offlinePlayer.setWhitelisted(false);
        }
        player.setWhitelisted(true);
        inventory.setItem(15, this.getItem("clear-whitelist"));
        inventory.setItem(12, this.buildWhitelistItem());
        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-cleared"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        String message = event.getMessage();

        if (this.whiteListMod.containsKey(playerUUID)) {
            event.setCancelled(true);

            if (message.equalsIgnoreCase("cancel")) {
                this.whiteListMod.remove(playerUUID);
                this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-add-cancelled"));
            } else {
                modifyWhitelist(player, message);
            }
        }
    }

    private void modifyWhitelist(Player player, String message) {
        String action = this.whiteListMod.get(player.getUniqueId().toString());

        if (action.equalsIgnoreCase("add")) {
            if (message.length() > 16) {
                Bukkit.getOfflinePlayer(UUID.fromString(message)).setWhitelisted(true);
            } else {
                Bukkit.getOfflinePlayer(message).setWhitelisted(true);
            }
            this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-added").replace("%player%", message));
        } else if (action.equalsIgnoreCase("remove")) {
            if (message.length() > 16) {
                Bukkit.getOfflinePlayer(UUID.fromString(message)).setWhitelisted(false);
            } else {
                Bukkit.getOfflinePlayer(message).setWhitelisted(false);
            }
            this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-removed").replace("%player%", message));
        }

        this.whiteListMod.remove(player.getUniqueId().toString());
    }
}
