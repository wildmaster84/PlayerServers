package net.cakemine.playerservers.bungee.events;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import java.util.*;
import java.io.*;

public class ServerCreateFinishEvent extends Event
{
    PlayerServers pl;
    private UUID uuid;
    private String serverName;
    private int port;
    private int memx;
    private int mems;
    private String template;
    
    public ServerCreateFinishEvent(PlayerServers pl, UUID uuid, String serverName, int port, int memx, int mems, String template) {
        this.pl = pl;
        this.uuid = uuid;
        this.serverName = serverName;
        this.port = port;
        this.memx = memx;
        this.mems = mems;
        this.template = template;
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
    
    public void setServerName(String s) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "server-name", s);
    }
    
    public int getPort() {
        return this.port;
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
    
    public String getTemplate() {
        return this.template;
    }
    
    public File getServerFolder() {
        return new File(this.pl.serversFolder + File.separator + this.uuid);
    }
}
