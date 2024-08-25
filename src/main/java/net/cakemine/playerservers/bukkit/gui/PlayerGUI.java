package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;

import java.util.Date;

import org.bukkit.*;
import org.bukkit.BanList.Type;
import org.bukkit.inventory.*;
import org.bukkit.event.*;

public class PlayerGUI extends CustomGUI
{
    PlayerServers pl;
    
    public PlayerGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI, Player player2) {
        reopenGUI = this.reopenInventory(player, reopenGUI, 3, this.getTitle());
        if (reopenGUI == null) {
            return;
        }
        this.fillInventory(player, reopenGUI, 0);
        this.addBackButtons(reopenGUI);
        reopenGUI.setItem(4, this.buildPlayer(player2));
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.kick")) {
            reopenGUI.setItem(12, this.getFormattedItem("kick", player2.getName()));
        }
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.ban")) {
            reopenGUI.setItem(13, this.getFormattedItem("ban", player2.getName()));
        }
        if (player.hasPermission("playerservers.myserver") || player.hasPermission("playerservers.myserver.whitelist")) {
            if (player2.isWhitelisted()) {
                reopenGUI.setItem(14, this.getFormattedItem("player-is-whitelisted", player2.getName()));
            }
            else {
                reopenGUI.setItem(14, this.getFormattedItem("player-not-whitelisted", player2.getName()));
            }
        }
    }
    
    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
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
                	player.closeInventory();
                }
                else {
                    String replaceAll = ChatColor.stripColor(currentItem.getItemMeta().getDisplayName()).replaceAll("(Kick|Confirm Ban|Ban|Add|Remove)|(:\\s)|((is|is\\snot)\\swhitelisted\\.)|(\\s)", "");
                    this.pl.utils.debug(replaceAll);
                    Player player2 = Bukkit.getPlayer(replaceAll);
                    if (this.getItem("kick").equals(currentItem)) {
                    	player.closeInventory();
                        this.pl.utils.sendMsg(player, this.pl.messages.get("kicked-player").replaceAll("%player%", player2.getName()));
                        this.pl.utils.sendMsg(player2, this.pl.messages.get("got-kicked").replaceAll("%player%", player.getName()));
                        player2.kickPlayer(null);
                    }
                    else if (this.getItem("ban").equals(currentItem)) {
                        inventory.setItem(13, this.getFormattedItem("ban-confirm", player2.getName()));
                    }
                    else if (this.getItem("ban-confirm").equals(currentItem)) {
                    	player.closeInventory();
                        this.pl.utils.sendMsg(player, this.pl.messages.get("banned-player").replaceAll("%player%", player2.getName()));
                        this.pl.utils.sendMsg(player2, this.pl.messages.get("got-banned").replaceAll("%player%", player.getName()).replaceAll("%reason%", "Unspecified Reason"));
                        player2.kickPlayer(this.pl.messages.get("ban-message").replaceAll("%player%", player.getName()).replaceAll("%reason%", "Unspecified Reason"));
                        BanList banList = Bukkit.getServer().getBanList(Type.NAME);
                        banList.addBan(player.getName(), "Unspecified reason", Date.from(null), null);
                    }
                    else if (this.getItem("player-is-whitelisted").equals(currentItem)) {
                        player2.setWhitelisted(false);
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-removed").replaceAll("%player%", player2.getName()));
                        inventory.setItem(14, this.getFormattedItem("player-not-whitelisted", player2.getName()));
                    }
                    else if (this.getItem("player-not-whitelisted").equals(currentItem)) {
                        player2.setWhitelisted(true);
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-added").replaceAll("%player%", player2.getName()));
                        inventory.setItem(14, this.getFormattedItem("player-is-whitelisted", player2.getName()));
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
}
