package net.cakemine.playerservers.bungee.events;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import java.util.*;
import java.io.*;

public class ServerDeleteEvent extends Event implements Cancellable
{
    PlayerServers pl;
    private boolean cancelled;
    private UUID uuid;
    private String serverName;
    private int port;
    private int memx;
    private int mems;
    
    public ServerDeleteEvent(PlayerServers pl, UUID uuid, String serverName, int port, int memx, int mems) {
        this.cancelled = false;
        this.pl = pl;
        this.uuid = uuid;
        this.serverName = serverName;
        this.port = port;
        this.memx = memx;
        this.mems = mems;
    }
    
    public UUID getOwnerId() {
        return this.uuid;
    }
    
    public String getOwnerName() {
        return this.pl.utils.getName(this.uuid.toString());
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public int getServerPort() {
        return this.port;
    }
    
    public int getXmx() {
        return this.memx;
    }
    
    public int getXms() {
        return this.mems;
    }
    
    public File getServerFolder() {
        return new File(this.pl.serversFolder + File.separator + this.uuid);
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
