package net.cakemine.playerservers.velocity.events;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;


public class PSReloadEvent implements ResultedEvent<GenericResult> {
    private GenericResult cancelled = GenericResult.allowed();
    
    public PSReloadEvent() {
    }
	@Override
	public GenericResult getResult() {
		// TODO Auto-generated method stub
		return this.cancelled;
	}

	@Override
	public void setResult(GenericResult result) {
		
	}
}
