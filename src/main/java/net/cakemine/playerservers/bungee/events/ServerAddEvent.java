package net.cakemine.playerservers.bungee.events;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import net.md_5.bungee.api.config.*;
import java.util.*;

public class ServerAddEvent extends Event implements Cancellable
{
    PlayerServers pl;
    private boolean cancelled;
    private ServerInfo serverInfo;
    private UUID uuid;
    private int memx;
    private int mems;
    private int port;
    private String motd;
    
    public ServerAddEvent(PlayerServers pl, ServerInfo serverInfo, UUID uuid, int memx, int mems) {
        this.cancelled = false;
        this.pl = pl;
        this.serverInfo = serverInfo;
        this.uuid = uuid;
        this.memx = memx;
        this.mems = mems;
        this.port = serverInfo.getAddress().getPort();
        this.motd = serverInfo.getMotd();
    }
    
    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }
    
    public String getServerName() {
        return this.serverInfo.getName();
    }
    
    public void setServerName(String s) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "server-name", s);
    }
    
    public UUID getOwnerId() {
        return this.uuid;
    }
    
    public String getOwnerName() {
        return this.pl.utils.getName(this.uuid.toString());
    }
    
    public int getXmx() {
        return this.memx;
    }
    
    public void setXmx(int n) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "memory", n + "M/" + this.pl.serverManager.serverMap.get(this.uuid.toString()).fromHashMap().get("memory").split("\\/")[1]);
    }
    
    public int getXms() {
        return this.mems;
    }
    
    public void setXms(int n) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "memory", this.pl.serverManager.serverMap.get(this.uuid.toString()).fromHashMap().get("memory").split("\\/")[0] + "/" + n + "M");
    }
    
    public int getPort() {
        return this.port;
    }
    
    public String getMotd() {
        return this.motd;
    }
    
    public void setMotd(String s) {
        this.pl.settingsManager.changeSetting(this.uuid.toString(), "motd", s);
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
