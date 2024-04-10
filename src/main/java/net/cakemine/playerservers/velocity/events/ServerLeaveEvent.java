package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;
import com.velocitypowered.api.proxy.server.ServerInfo;

public class ServerLeaveEvent implements ResultedEvent<GenericResult> {
    PlayerServers pl;
    private GenericResult cancelled = GenericResult.allowed();
    private ServerInfo serverInfo;
    private UUID uuid;
    private int port;
    private String motd;
    
    public ServerLeaveEvent(PlayerServers pl, ServerInfo serverInfo, UUID uuid) {
        this.pl = pl;
        this.serverInfo = serverInfo;
        this.uuid = uuid;
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
    
    public int getPort() {
        return this.port;
    }
    
    public String getMotd() {
        return this.motd;
    }

	@Override
	public GenericResult getResult() {
		return this.cancelled;
	}

	@Override
	public void setResult(GenericResult result) {
		
	}
}
