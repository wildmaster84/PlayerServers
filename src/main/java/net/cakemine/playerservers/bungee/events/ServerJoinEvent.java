package net.cakemine.playerservers.bungee.events;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import net.md_5.bungee.api.config.*;
import java.util.*;

public class ServerJoinEvent extends Event implements Cancellable
{
    PlayerServers pl;
    private boolean cancelled;
    private ServerInfo serverInfo;
    private UUID uuid;
    private int port;
    private String motd;
    
    public ServerJoinEvent(PlayerServers pl, ServerInfo serverInfo, UUID uuid) {
        this.cancelled = false;
        this.pl = pl;
        this.serverInfo = serverInfo;
        this.uuid = uuid;
        this.port = serverInfo.getAddress().getPort();
        this.motd = serverInfo.getMotd();
    }
    
    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }
    
    public UUID getOwnerId() {
        return this.uuid;
    }
    
    public String getOwnerName() {
        return this.pl.utils.getName(this.uuid.toString());
    }
    
    public int getPort() {
        return this.port;
    }
    
    public String getMotd() {
        return this.motd;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
