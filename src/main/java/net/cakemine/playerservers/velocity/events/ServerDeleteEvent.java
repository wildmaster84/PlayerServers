package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;

import java.io.*;

public class ServerDeleteEvent implements ResultedEvent<GenericResult> {
    PlayerServers pl;
    private UUID uuid;
    private String serverName;
    private int port;
    private int memx;
    private int mems;
    private GenericResult cancelled = GenericResult.allowed();
    
    public ServerDeleteEvent(PlayerServers pl, UUID uuid, String serverName, int port, int memx, int mems) {
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
