package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import java.util.logging.*;
import java.util.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.event.*;

public class TemplatesGUI extends CustomGUI
{
    PlayerServers pl;
    
    public TemplatesGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }
    
    @Override
    public void open(Player player, Inventory reopenGUI, int n) {
    	this.pl.utils.debug("opened TemplatesGUI");
        int size = this.pl.templates.size();
        this.pl.utils.debug("templateCount = " + size + " | templates = " + this.pl.templates);
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
        if (size <= 1) {
            reopenGUI.setItem(4, this.getItem("none-found"));
            return;
        }
        if (b) {
            if (n3 != 0) {
                n5 = n3 - 1;
            }
            if (n5 > 0) {
                n6 = n5 * 44;
            }
            else {
                n6 = 0;
            }
        }
        if (b && n2 == 6 && size > 54) {
            if (n - 1 > 0) {
                reopenGUI.setItem(45, this.buildArrow(false, n - 1));
            }
            reopenGUI.setItem(53, this.buildArrow(true, n + 1));
        }
        int n7 = 0;
        for (Map.Entry<String, HashMap<String, String>> entry : this.pl.templates.entrySet()) {
            this.pl.utils.debug("Loop count: " + n7 + " | slotCount: " + n4);
            if (n7 >= n6) {
                String s = entry.getKey();
                this.pl.utils.debug("Processing template for GUI: " + s);
                String s2 = this.pl.templates.get(s).get("icon");
                String s3 = this.pl.templates.get(s).get("desc");
                this.pl.utils.debug("template settings: icon = " + s2 + " | desc = " + s3 + " | op =" + this.pl.templates.get(s).get("OP"));
                int n8 = 0;
                String upperCase = s2.toUpperCase();
                Material material;
                if (upperCase.matches("(?i)^(.*):\\d+$")) {
                    material = Material.matchMaterial(upperCase.split(":")[0]);
                    n8 = (upperCase.split(":")[1].matches("\\d+") ? ((short)Short.valueOf(upperCase.split(":")[1])) : 0);
                }
                else {
                    material = Material.matchMaterial(upperCase);
                }
                if (material == null) {
                    this.pl.utils.log(Level.WARNING, "Material \"" + upperCase + "\" not found! Defaulting to bedrock for \"" + s + "\" GUI item.");
                    material = Material.BEDROCK;
                }
                reopenGUI.setItem(n4, this.item(n7 + 1, material, n8, "&e&l&o" + s, s3));
                ++n4;
                if (b && n4 > 44) {
                    break;
                }
                continue;
            }
        }
    }
    
    @EventHandler
    @Override
    public void onClick(InventoryClickEvent inventoryClickEvent) {
    	this.pl.utils.debug("clicked TemplatesGUI");
        String stripColor = this.pl.utils.stripColor(inventoryClickEvent.getView().getTitle());
        Inventory inventory = inventoryClickEvent.getInventory();
        ItemStack currentItem = inventoryClickEvent.getCurrentItem();
        Player player = (Player)inventoryClickEvent.getWhoClicked();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, stripColor, currentItem);
        if (stripColor.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
        	this.pl.utils.debug("titles matched");
            Bukkit.getPluginManager().callEvent(guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                inventoryClickEvent.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                	this.pl.utils.debug("item was null or no metadata");
                    this.close(player);
                }
                else {
                	String displayName = currentItem.getItemMeta().getDisplayName();
                    if (!this.getFillItem().equals(currentItem)) {
                        this.pl.utils.debug("clicked displayName = " + ChatColor.stripColor(this.pl.utils.color(displayName)));
                        if (this.pl.templates.containsKey(ChatColor.stripColor(this.pl.utils.color(displayName)))) {
                        	this.pl.utils.debug("contains template");
                            this.pl.psCmd.psCreate(player.getUniqueId().toString(), displayName.replaceAll("(&|§)[aA-fFkK-oOrR1-9]", ""));
                        }
                        this.pl.utils.debug("finished");
                        this.close(player);
                    }
                }
            }
        }
    }
}
