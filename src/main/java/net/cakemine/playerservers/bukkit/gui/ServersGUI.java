package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import java.util.logging.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import java.util.*;
import org.bukkit.event.*;

public class ServersGUI extends CustomGUI
{
    PlayerServers pl;
    
    public ServersGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    public String getValue(String s, String s2) {
        if (this.pl.servers.containsKey(s) && this.pl.servers.get(s).containsKey(s2)) {
            return this.pl.servers.get(s).get(s2);
        }
        return "???";
    }
    
    public String doPlaceholders(OfflinePlayer offlinePlayer, String s) {
        String string = offlinePlayer.getUniqueId().toString();
        try {
            if (s.contains("%player-uuid%")) {
                s = s.replaceAll("%player-uuid%", string);
            }
            if (s.contains("%player%")) {
                s = s.replaceAll("%player%", this.getValue(string, "owner-name"));
            }
            if (s.contains("%current-players%")) {
                s = s.replaceAll("%current-players%", this.getValue(string, "current-players"));
            }
            if (s.contains("%max-players%")) {
                s = s.replaceAll("%max-players%", this.getValue(string, "max-players"));
            }
            if (s.contains("%motd%")) {
                s = s.replaceAll("%motd%", this.getValue(string, "motd"));
            }
            if (s.contains("%template-name%")) {
                s = s.replaceAll("%template-name%", this.getValue(string, "template-name"));
            }
            if (s.contains("%expire-date%")) {
                s = s.replaceAll("%expire-date%", this.getValue(string, "expire-date"));
            }
            if (s.contains("%time-left%")) {
                s = s.replaceAll("%time-left%", this.getValue(string, "time-left"));
            }
            if (s.contains("%whitelist%")) {
                s = s.replaceAll("%whitelist%", this.getValue(string, "white-listed").equalsIgnoreCase("true") ? "on" : "off");
            }
        }
        catch (NullPointerException ex) {
            this.pl.utils.log(Level.WARNING, "Tried to replace a placeholder in message, but the value was null! Please send this stack trace to the developer!");
            this.pl.utils.log(Level.WARNING, "input: " + s);
            ex.printStackTrace();
        }
        return s;
    }
    
    public ItemStack buildServer(OfflinePlayer offlinePlayer) {
        ItemStack itemStack = new ItemStack(this.getItem("server"));
        itemStack.setType(Material.LEGACY_SKULL_ITEM);
        itemStack.setDurability((short)3);
        SkullMeta itemMeta = (SkullMeta)itemStack.getItemMeta();
        this.pl.utils.debug("Trying to get '" + offlinePlayer.getUniqueId().toString() + "' from servers map: " + this.pl.servers);
        itemMeta.setDisplayName(this.pl.utils.color("&e&l&o" + this.getValue(offlinePlayer.getUniqueId().toString(), "server-name")));
        itemMeta.setOwner(offlinePlayer.getName());
        ArrayList<String> lore = new ArrayList<String>();
        Iterator iterator = itemMeta.getLore().iterator();
        while (iterator.hasNext()) {
            lore.add(this.pl.utils.color(this.doPlaceholders(offlinePlayer, (String) iterator.next())));
        }
        itemMeta.setLore((List)lore);
        itemStack.setItemMeta((ItemMeta)itemMeta);
        this.getListedPlayers().put(offlinePlayer.getName(), itemStack);
        return itemStack;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI, int n, HashMap servers) {
        this.pl.servers = (HashMap<String, HashMap<String, String>>)servers;
        int size = this.pl.servers.size();
        this.pl.utils.debug("serverCount = " + size + " | servers = " + this.pl.servers.values());
        int n2 = 1;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        boolean b = false;
        if (size <= 8) {
            n2 = 1;
        }
        else if (size <= 18) {
            n2 = 2;
        }
        else if (size <= 27) {
            n2 = 3;
        }
        else if (size <= 36) {
            n2 = 4;
        }
        else if (size <= 45) {
            n2 = 5;
        }
        else if (size > 46) {
            n2 = 6;
            n3 = n;
            b = true;
        }
        reopenGUI = this.reopenGUI(player, reopenGUI, n2, this.getTitle());
        this.fill(player, reopenGUI, 15);
        if (size < 1) {
            reopenGUI.setItem(4, this.getItem("none-online"));
            return;
        }
        int n7;
        if (b) {
            if (n3 != 0) {
                n5 = n3 - 1;
            }
            if (n5 > 0) {
                n6 = n5 * 44;
                n7 = n5 * 54;
            }
            else {
                n6 = 0;
                n7 = 44;
            }
        }
        else {
            n7 = 53;
        }
        if (b && n2 == 6 && size > 54) {
            if (n - 1 > 0) {
                reopenGUI.setItem(45, this.buildArrow(false, n - 1));
            }
            reopenGUI.setItem(53, this.buildArrow(true, n + 1));
        }
        this.pl.utils.debug("tList size = " + this.pl.servers.size());
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(this.pl.servers.keySet());
        for (int n8 = 0; n8 < n7 && n8 < this.pl.servers.size(); ++n8) {
            this.pl.utils.debug("Loop count: " + n8 + " | slotCount: " + n4);
            if (n8 >= n6) {
                reopenGUI.setItem(n4, this.buildServer(Bukkit.getOfflinePlayer(UUID.fromString(list.get(n8)))));
                ++n4;
                if (b && n4 > 44) {
                    break;
                }
            }
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
                    this.close(player);
                }
                else {
                    String stripColor2 = this.pl.utils.stripColor(currentItem.getItemMeta().getDisplayName());
                    if (!this.getFillItem().equals(currentItem)) {
                        for (Map.Entry<String, HashMap<String, String>> entry : this.pl.servers.entrySet()) {
                            if (this.pl.utils.stripColor(entry.getValue().get("server-name")).equals(stripColor2)) {
                                this.pl.psCmd.psJoin(player.getUniqueId().toString(), entry.getValue().get("owner-name"));
                            }
                        }
                        this.close(player);
                    }
                }
            }
        }
    }
}
