package net.cakemine.playerservers.bungee.events;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import java.util.*;

public class ServerModifyEvent extends Event
{
    PlayerServers pl;
    private UUID uuid;
    
    public ServerModifyEvent(PlayerServers pl, String s) {
        this.pl = pl;
        this.uuid = UUID.fromString(s);
    }
    
    public UUID getModifiedUUID() {
        return this.uuid;
    }
}
