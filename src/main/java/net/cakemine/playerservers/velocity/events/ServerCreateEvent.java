package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;

import java.io.*;

public class ServerCreateEvent implements ResultedEvent<GenericResult>{
    PlayerServers pl;
    private UUID uuid;
    private String serverName;
    private int port;
    private int memx;
    private int mems;
    private String template;
    private GenericResult cancelled = GenericResult.allowed();
    
    public ServerCreateEvent(PlayerServers pl, UUID uuid, String serverName, int port, int memx, int mems, String template) {
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
    
    public int getPort() {
        return this.port;
    }
    
    public int getXmx() {
        return this.memx;
    }
    
    public int getXms() {
        return this.mems;
    }
    
    public String getTemplate() {
        return this.template;
    }
    
    public File getTemplateFolder() {
        return this.pl.templateManager.getTemplateFile(this.template);
    }
    
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public File getServerFolder() {
        return new File(this.pl.serversFolder + File.separator + this.uuid);
    }

	@Override
	public GenericResult getResult() {
		return this.cancelled;
	}

	@Override
	public void setResult(GenericResult result) {
	    this.cancelled = Objects.requireNonNull(result);
	}
}
