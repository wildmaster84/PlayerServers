package net.cakemine.playerservers.velocity.events;

import net.cakemine.playerservers.velocity.*;
import java.util.*;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;;

public class ChangePropertiesEvent implements ResultedEvent<GenericResult> {
    PlayerServers pl;
    private GenericResult cancelled = GenericResult.allowed();
    private UUID uuid;
    private String setting;
    private String newValue;
    
    public ChangePropertiesEvent(PlayerServers pl, String s, String setting, String newValue) {
        this.pl = pl;
        this.uuid = UUID.fromString(s);
        this.setting = setting;
        this.newValue = newValue;
    }
    
    public UUID getModifiedUUID() {
        return this.uuid;
    }
    
    public String getModifiedSetting() {
        return this.setting;
    }
    
    public String getNewValue() {
        return this.newValue;
    }
    
    public boolean isChanged() {
        return this.newValue != null;
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
