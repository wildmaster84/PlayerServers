package net.cakemine.playerservers.bukkit.events;

import org.bukkit.event.*;
import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;

public class CreatorJoinEvent extends Event implements Cancellable
{
    private static HandlerList handlers;
    PlayerServers pl;
    private boolean cancelled;
    private Player player;
    
    public CreatorJoinEvent(PlayerServers pl, Player player) {
        this.cancelled = false;
        this.pl = pl;
        this.player = player;
        this.pl.utils.debug("CreatorJoinEvent Fired.");
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public HandlerList getHandlers() {
        return CreatorJoinEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return CreatorJoinEvent.handlers;
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
