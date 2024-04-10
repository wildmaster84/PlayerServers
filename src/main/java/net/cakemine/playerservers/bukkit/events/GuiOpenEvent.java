package net.cakemine.playerservers.bukkit.events;

import org.bukkit.event.*;
import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;

public class GuiOpenEvent extends Event implements Cancellable
{
    private static HandlerList handlers;
    PlayerServers pl;
    private boolean cancelled;
    private Player player;
    private Inventory inventory;
    private int rows;
    private String title;
    
    public GuiOpenEvent(PlayerServers pl, Player player, Inventory inventory, int rows, String title) {
        this.cancelled = false;
        this.pl = pl;
        this.player = player;
        this.inventory = inventory;
        this.rows = rows;
        this.title = title;
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public Inventory getInventory() {
        return this.inventory;
    }
    
    public ItemStack getSlot(int n) {
        return this.inventory.getItem(n);
    }
    
    public void setSlot(int n, ItemStack itemStack) {
        this.inventory.setItem(n, itemStack);
    }
    
    public void clearSlot(int n) {
        this.inventory.clear(n);
    }
    
    public void clearAll() {
        this.inventory.clear();
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    
    public int getRows() {
        return this.rows;
    }
    
    public void setRows(int rows) {
        this.rows = rows;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public HandlerList getHandlers() {
        return GuiOpenEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return GuiOpenEvent.handlers;
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
