package net.cakemine.playerservers.bungee;

import net.md_5.bungee.api.config.*;
import net.cakemine.playerservers.bungee.commands.*;
import java.util.logging.*;
import java.util.*;
import java.util.Map.Entry;

public class RepeatTasks implements Runnable
{
    private PlayerServers pl;
    private int ramCycles;
    private long purgeCycles;
    
    public RepeatTasks(PlayerServers pl) {
        this.purgeCycles = 600000L;
        this.pl = pl;
    }
    
    @Override
    public void run() {
        try {
            if (this.pl.ctrl != null && !this.pl.ctrl.getSocket().isClosed()) {
                this.pl.ctrl.send("+heartbeat " + System.currentTimeMillis());
            }
            ArrayList<String> list = new ArrayList<String>();
            for (Entry<String, ServerInfo> entry : this.pl.proxy.getServers().entrySet()) {
                String string = entry.getKey().toString();
                if (string != null && this.pl.serverManager.isPlayerServer(string)) {
                    String ownerId = this.pl.serverManager.getOwnerId(string);
                    int port = ((ServerInfo)entry.getValue()).getAddress().getPort();
                    long n = 90001L;
                    if (this.pl.serverManager.addedServers.containsKey(string)) {
                        n = System.currentTimeMillis() - Long.valueOf(this.pl.serverManager.addedServers.get(string).get("time"));
                    }
                    if (!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(ownerId), port) || ownerId == null || PlayerServerCMD.chill.contains(ownerId) || n <= 90000L) {
                        continue;
                    }
                    this.pl.running.remove(ownerId);
                    list.add(string);
                }
            }
            Iterator<String> iterator2 = list.iterator();
            while (iterator2.hasNext()) {
                this.pl.serverManager.removeBungee(iterator2.next());
            }
            if (this.ramCycles >= 2) {
                this.pl.serverManager.countRam();
                this.ramCycles = 0;
            }
            else {
                ++this.ramCycles;
            }
            if (this.pl.useExpiry && this.pl.autoPurge) {
                if (this.purgeCycles <= 0L) {
                    int n2 = 0;
                    for (String s : this.pl.serverManager.serverMap.keySet()) {
                        if (!this.pl.utils.hasPerm(s, "playerservers.bypassexpire") && !this.pl.utils.hasPerm(s, "playerservers.bypasspurge") && this.pl.expiryTracker.msLeft(s) < 1L && this.pl.autoPurgeTime > -1L && Math.abs(this.pl.expiryTracker.msLeft(s)) > this.pl.autoPurgeTime) {
                            this.pl.utils.debug("Auto purging server: " + s);
                            this.pl.serverManager.deleteServer(null, s);
                            ++n2;
                        }
                    }
                    if (n2 > 0) {
                        this.pl.utils.log("Automatically purged " + n2 + " player servers.");
                    }
                    this.purgeCycles = this.pl.autoPurgeInterval;
                }
                else {
                    this.purgeCycles -= 30000L;
                }
            }
        }
        catch (Exception ex) {
            this.pl.utils.log(Level.WARNING, "Error occurred during repeating task! Please send the following stack trace to the developer:");
            ex.printStackTrace();
        }
    }
}
