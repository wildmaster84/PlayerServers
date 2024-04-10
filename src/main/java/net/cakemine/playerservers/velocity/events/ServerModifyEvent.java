package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;;

public class ServerModifyEvent implements ResultedEvent<GenericResult>{
    PlayerServers pl;
    private UUID uuid;
    private GenericResult result = GenericResult.allowed();
    
    public ServerModifyEvent(PlayerServers pl, String s) {
        this.pl = pl;
        this.uuid = UUID.fromString(s);
    }
    
    public UUID getModifiedUUID() {
        return this.uuid;
    }

	@Override
	public GenericResult getResult() {
		return this.result;
	}

	@Override
	public void setResult(GenericResult result) {
		this.result = result;
		
	}
}
