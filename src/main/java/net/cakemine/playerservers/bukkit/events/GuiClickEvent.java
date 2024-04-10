package net.cakemine.playerservers.bukkit.events;

import org.bukkit.event.*;
import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;

public class GuiClickEvent extends Event implements Cancellable
{
    private static HandlerList handlers;
    PlayerServers pl;
    private boolean cancelled;
    private Player player;
    private Inventory inventory;
    private String title;
    private ItemStack item;
    
    public GuiClickEvent(PlayerServers pl, Player player, Inventory inventory, String title, ItemStack item) {
        this.cancelled = false;
        this.pl = pl;
        this.player = player;
        this.inventory = inventory;
        this.title = title;
        this.item = item;
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public Inventory getGuiInventory() {
        return this.inventory;
    }
    
    public String getInventoryTitle() {
        return this.title;
    }
    
    public ItemStack getClickedItem() {
        return this.item;
    }
    
    public HandlerList getHandlers() {
        return GuiClickEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return GuiClickEvent.handlers;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    static {
        handlers = new HandlerList();
    }
}
