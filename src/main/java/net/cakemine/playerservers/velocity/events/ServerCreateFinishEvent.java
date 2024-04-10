package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;

import java.io.*;

public class ServerCreateFinishEvent implements ResultedEvent<GenericResult> {
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
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "memory", n + "M/" + this.pl.serverManager.serverMap.get(this.uuid.toString()).get("memory").toString().split("\\/")[1]);
    }
    
    public int getXms() {
        return this.mems;
    }
    
    public void setXms(int n) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "memory", this.pl.serverManager.serverMap.get(this.uuid.toString()).get("memory").toString().split("\\/")[0] + "/" + n + "M");
    }
    
    public String getTemplate() {
        return this.template;
    }
    
    public File getServerFolder() {
        return new File(this.pl.serversFolder + File.separator + this.uuid);
    }

	@Override
	public GenericResult getResult() {
		// TODO Auto-generated method stub
		return GenericResult.allowed();
	}

	@Override
	public void setResult(GenericResult result) {
		// TODO Auto-generated method stub
		
	}
}
