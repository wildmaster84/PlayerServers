package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;
import com.velocitypowered.api.proxy.server.ServerInfo;

public class ServerRemoveEvent implements ResultedEvent<GenericResult> {
    PlayerServers pl;
    private GenericResult cancelled = GenericResult.allowed();
    private ServerInfo serverInfo;
    private UUID uuid;
    private int memx;
    private int mems;
    private int port;
    private String motd;
    
    public ServerRemoveEvent(PlayerServers pl, ServerInfo serverInfo, UUID uuid, int memx, int mems) {
        this.cancelled = ResultedEvent.GenericResult.allowed();
        this.pl = pl;
        this.serverInfo = serverInfo;
        this.uuid = uuid;
        this.memx = memx;
        this.mems = mems;
        this.port = serverInfo.getAddress().getPort();
        this.motd = "A Velocity Server";
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
    
    public int getXmx() {
        return this.memx;
    }
    
    public void setXmx(int n) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "memory", n + "M/" + this.pl.serverManager.serverMap.get(this.uuid.toString()).getSetting("memory").toString().split("\\/")[1]);
    }
    
    public int getXms() {
        return this.mems;
    }
    
    public void setXms(int n) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "memory", this.pl.serverManager.serverMap.get(this.uuid.toString()).getSetting("memory").toString().split("\\/")[0] + "/" + n + "M");
    }
    
    public int getPort() {
        return this.port;
    }
    
    public String getMotd() {
        return this.motd;
    }
    
    public void setMotd(String s) {
        this.pl.serverManager.setServerInfo(this.uuid.toString(), "motd", s);
    }

	@Override
	public GenericResult getResult() {
		// TODO Auto-generated method stub
		return this.cancelled;
	}

	@Override
	public void setResult(GenericResult result) {
		this.cancelled = result;
		
	}
}
