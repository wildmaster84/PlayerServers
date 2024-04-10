package net.cakemine.playerservers.bungee.events;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import java.util.*;

public class ChangePropertiesEvent extends Event implements Cancellable
{
    PlayerServers pl;
    private boolean cancelled;
    private UUID uuid;
    private String setting;
    private String newValue;
    
    public ChangePropertiesEvent(PlayerServers pl, String s, String setting, String newValue) {
        this.cancelled = false;
        this.pl = pl;
        this.uuid = UUID.fromString(s);
        this.setting = setting;
        this.newValue = newValue;
    }
    
    public UUID getModifiedUUID() {
        return this.uuid;
    }
    
    public String getModifiedSetting() {
        return this.setting;
    }
    
    public String getNewValue() {
        return this.newValue;
    }
    
    public boolean isChanged() {
        return this.newValue != null;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
