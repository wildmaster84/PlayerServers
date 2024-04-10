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

public class WhitelistGUI extends CustomGUI
{
    PlayerServers pl;
    HashMap<String, String> whiteListMod;
    
    public WhitelistGUI(PlayerServers pl) {
        super(pl);
        this.whiteListMod = new HashMap<String, String>();
        this.pl = pl;
        HandlerList.unregisterAll(this);
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }
    
    public void setWaitInput(Player player, String s) {
        this.whiteListMod.put(player.getUniqueId().toString(), s);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.pl, new Runnable() {
            @Override
            public void run() {
                if (WhitelistGUI.this.whiteListMod.containsKey(player.getUniqueId().toString())) {
                    WhitelistGUI.this.pl.utils.sendMsg(player, WhitelistGUI.this.pl.messages.get("whitelist-add-timeout"));
                    WhitelistGUI.this.whiteListMod.remove(player.getUniqueId().toString());
                }
            }
        }, 600L);
    }
    
    private ItemStack buildWhitelistItem() {
        ItemStack item = this.getItem("current-whitelist");
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setLore(Arrays.asList(this.whitelistString().split("\\|\\|")));
        item.setItemMeta(itemMeta);
        return item;
    }
    
    public String whitelistString() {
        StringBuilder sb = new StringBuilder();
        int n = 1;
        int size = Bukkit.getWhitelistedPlayers().size();
        for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
            if (n >= 10) {
                sb.append("&eAnd &o").append(size).append("&e more...");
                break;
            }
            sb.append("&e&o").append(offlinePlayer.getName()).append(" &8: &7").append(offlinePlayer.getUniqueId().toString().substring(0, 7)).append("...||");
            ++n;
            --size;
        }
        if (sb.toString().endsWith("||")) {
            sb.delete(sb.length() - 2, sb.length());
        }
        String color;
        if (sb.length() < 1) {
            color = "Nobody!";
        }
        else {
            color = this.pl.utils.color(sb.toString());
        }
        return color;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI) {
        reopenGUI = this.reopenGUI(player, reopenGUI, 3, this.getTitle());
        if (reopenGUI == null) {
            return;
        }
        this.fill(player, reopenGUI, 0);
        this.addBackButtons(reopenGUI);
        if (Bukkit.hasWhitelist()) {
            reopenGUI.setItem(11, this.getItem("whitelist-on"));
        }
        else {
            reopenGUI.setItem(11, this.getItem("whitelist-off"));
        }
        reopenGUI.setItem(12, this.buildWhitelistItem());
        reopenGUI.setItem(13, this.getItem("add-player"));
        reopenGUI.setItem(14, this.getItem("remove-player"));
        reopenGUI.setItem(15, this.getItem("clear-whitelist"));
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
                else if (!this.getItem("current-whitelist").equals(currentItem)) {
                    if (this.getItem("whitelist-on").equals(currentItem)) {
                        Bukkit.setWhitelist(false);
                        inventory.setItem(11, this.getItem("whitelist-off"));
                    }
                    else if (this.getItem("whitelist-off").equals(currentItem)) {
                        Bukkit.setWhitelist(true);
                        inventory.setItem(11, this.getItem("whitelist-on"));
                    }
                    else if (this.getItem("add-player").equals(currentItem)) {
                        this.setWaitInput(player, "add");
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-modify-instructions"));
                        this.close(player);
                    }
                    else if (this.getItem("remove-player").equals(currentItem)) {
                        this.setWaitInput(player, "remove");
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-modify-instructions"));
                        this.close(player);
                    }
                    else if (this.getItem("clear-whitelist").equals(currentItem)) {
                        inventory.setItem(15, this.getItem("clear-confirm"));
                    }
                    else if (this.getItem("clear-confirm").equals(currentItem)) {
                        Iterator<OfflinePlayer> iterator = Bukkit.getWhitelistedPlayers().iterator();
                        while (iterator.hasNext()) {
                            iterator.next().setWhitelisted(false);
                        }
                        player.setWhitelisted(true);
                        inventory.setItem(15, this.getItem("clear-whitelist"));
                        inventory.setItem(12, this.buildWhitelistItem());
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-cleared"));
                    }
                    else if (!this.getFillItem().equals(currentItem)) {
                        if (this.getBackButton().equals(currentItem)) {
                            this.pl.gui.getGUI("settings").open(player, inventory);
                        }
                        else {
                            this.close(player);
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        Player player = asyncPlayerChatEvent.getPlayer();
        String string = asyncPlayerChatEvent.getPlayer().getUniqueId().toString();
        String message = asyncPlayerChatEvent.getMessage();
        if (this.whiteListMod.containsKey(string)) {
            asyncPlayerChatEvent.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) {
                this.whiteListMod.remove(string);
                this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-add-cancelled"));
                return;
            }
            if (this.whiteListMod.get(string).equalsIgnoreCase("add")) {
                if (message.length() > 16) {
                    Bukkit.getOfflinePlayer(UUID.fromString(message)).setWhitelisted(true);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-added").replaceAll("%player%", message));
                }
                else {
                    Bukkit.getOfflinePlayer(message).setWhitelisted(true);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-added").replaceAll("%player%", message));
                }
                this.whiteListMod.remove(string);
            }
            else if (this.whiteListMod.get(string).equalsIgnoreCase("remove")) {
                if (message.length() > 16) {
                    Bukkit.getOfflinePlayer(UUID.fromString(message)).setWhitelisted(false);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-removed").replaceAll("%player%", message));
                }
                else {
                    Bukkit.getOfflinePlayer(message).setWhitelisted(false);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-removed").replaceAll("%player%", message));
                }
                this.whiteListMod.remove(string);
            }
        }
    }
}
