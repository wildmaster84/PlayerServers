package net.cakemine.playerservers.bungee;

import net.md_5.bungee.api.*;
import java.util.regex.*;

import net.md_5.bungee.event.*;
import net.md_5.bungee.api.config.*;
import net.md_5.bungee.api.connection.*;
import java.util.*;
import java.util.concurrent.*;
import net.md_5.bungee.api.plugin.*;
import net.md_5.bungee.api.event.*;
import net.cakemine.playerservers.bungee.events.*;
import net.cakemine.playerservers.bungee.objects.StoredPlayer;

public class Listeners implements Listener
{
    PlayerServers pl;
    private HashMap<String, Long> creating;
    
    public Listeners(PlayerServers pl) {
        this.creating = new HashMap<String, Long>();
        this.pl = pl;
    }
    
    @EventHandler(priority = 64)
    public void onChat(ChatEvent chatEvent) {
        String message = chatEvent.getMessage();
        if (chatEvent.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer proxiedPlayer = (ProxiedPlayer)chatEvent.getSender();
            proxiedPlayer.getUniqueId().toString();
            if ((proxiedPlayer.getServer() == null || !this.pl.serverManager.isPlayerServer(proxiedPlayer.getServer().getInfo().getName())) && message.matches("(/(myserver|mys|mysrv)($|\\s(.+)))")) {
                this.pl.utils.sendMsg(proxiedPlayer, "&cYou can only use &o/myserver&c (&c&o/mys&c) while on &oa player server.");
                chatEvent.setCancelled(true);
            }
            if (proxiedPlayer.getServer() != null && this.pl.serverManager.isPlayerServer(proxiedPlayer.getServer().getInfo().getName()) && !this.pl.utils.hasPerm((CommandSender)proxiedPlayer, "playerservers.admin") && !this.pl.utils.hasPerm((CommandSender)proxiedPlayer, "playerservers.bypassblock")) {
                for (String s : this.pl.blockedCmds) {
                    Pattern pattern;
                    if (s.contains("(?i)")) {
                        pattern = Pattern.compile(s);
                    }
                    else {
                        pattern = Pattern.compile("(?i)" + s);
                    }
                    Matcher matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        chatEvent.setCancelled(true);
                        this.pl.utils.log("Blocked command from " + proxiedPlayer.getName() + " > " + message);
                        this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("blocked-cmd").replaceAll("(%blocked-command%)", matcher.group(0)));
                        break;
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onJoin(PostLoginEvent postLoginEvent) {
        ProxiedPlayer player = postLoginEvent.getPlayer();
        if (player != null) {
            String serverUUID = player.getUniqueId().toString();
            if (!this.pl.playerMap.containsKey(player.getUniqueId())) {
            	// Maybe save after loading?
                this.pl.loadPlayer(player.getUniqueId(), new StoredPlayer(player.getUniqueId()));
            }
            if (this.pl.serverManager.hasServer(player.getUniqueId().toString()) && !this.pl.serverManager.getServerInfo(serverUUID, "player-name").equals(player.getName())) {
                this.pl.utils.log(player.getName() + " has changed their name. Updating their server.");
                if (this.pl.utils.getSrvName(serverUUID).equals(this.pl.serverManager.getServerInfo(serverUUID, "player-name"))) {
                    this.pl.serverManager.setServerInfo(serverUUID, "server-name", player.getName());
                    this.pl.utils.log(player.getName() + "'s server name was the same as their old name, updating server name too.");
                }
                this.pl.serverManager.setServerInfo(serverUUID, "player-name", player.getName());
            }
        }
    }
    
    @EventHandler
    public void onKick(ServerKickEvent serverKickEvent) {
        ProxiedPlayer player = serverKickEvent.getPlayer();
        if (player != null) {
            Object info = null;
            if (player.getServer() != null) {
                info = player.getServer().getInfo();
            }
            ServerInfo kicked = serverKickEvent.getKickedFrom();
            if (this.pl.serverManager.isPlayerServer(kicked.getName())) {
                this.pl.utils.debug("Kicked from a player server!");
                serverKickEvent.setCancelled(true);
                serverKickEvent.setCancelServer(this.pl.proxy.getServerInfo(this.pl.fallbackSrv));
            }
            this.pl.utils.debug("currentSrv = " + String.valueOf(info) + " | kickedSrv = " + kicked);
            if ((info == null || info.equals(kicked)) && this.pl.usingHelper.containsKey(player)) {
                this.pl.utils.debug(serverKickEvent.getClass() + " fired, " + player.getName() + " removed from helper map.");
                this.pl.usingHelper.remove(player);
            }
        }
    }
    
    @EventHandler
    public void onStop(ServerStopEvent serverStopEvent) {
        if (this.pl.serverManager.isPlayerServer(serverStopEvent.getServerInfo().getName())) {
            for (ProxiedPlayer proxiedPlayer : serverStopEvent.getServerInfo().getPlayers()) {
                if (!proxiedPlayer.getServer().getInfo().getName().equals(this.pl.fallbackSrv)) {
                    this.pl.utils.movePlayer(proxiedPlayer, this.pl.fallbackSrv, 0);
                }
            }
        }
        Iterator<Map.Entry<String, Long>> iterator2 = this.creating.entrySet().iterator();
        while (iterator2.hasNext()) {
            if (System.currentTimeMillis() - iterator2.next().getValue() > 120000L) {
                iterator2.remove();
            }
        }
    }
    
    @EventHandler
    public void onServerLeave(ServerDisconnectEvent serverDisconnectEvent) {
        ProxiedPlayer player = serverDisconnectEvent.getPlayer();
        if (player != null) {
            Server server = player.getServer();
            UUID uniqueId = player.getUniqueId();
            this.pl.playerMap.get(player.getUniqueId()).save();
            if (server != null && this.pl.serverManager.isPlayerServer(server.getInfo().getName())) {
                ServerLeaveEvent serverLeaveEvent = (ServerLeaveEvent)this.pl.proxy.getPluginManager().callEvent((Event)new ServerLeaveEvent(this.pl, server.getInfo(), uniqueId));
            }
            if (this.pl.usingHelper.containsKey(player)) {
                this.pl.utils.debug(serverDisconnectEvent.getClass() + " fired, " + player.getName() + " removed from helper map.");
                this.pl.usingHelper.remove(player);
            }
        }
    }
    
    @EventHandler
    public void onServerSwitch(ServerSwitchEvent serverSwitchEvent) {
        ProxiedPlayer player = serverSwitchEvent.getPlayer();
        if (player != null && this.pl.usingHelper.containsKey(player)) {
            this.pl.utils.debug(serverSwitchEvent.getClass() + " fired, " + player.getName() + " removed from helper map.");
            this.pl.usingHelper.remove(player);
        }
    }
    
    @EventHandler
    public void onServerJoin(ServerConnectEvent serverConnectEvent) {
        ServerInfo target = serverConnectEvent.getTarget();
        UUID uniqueId = serverConnectEvent.getPlayer().getUniqueId();
        if (target != null && this.pl.serverManager.isPlayerServer(target.getName()) && ((ServerJoinEvent)this.pl.proxy.getPluginManager().callEvent((Event)new ServerJoinEvent(this.pl, target, uniqueId))).isCancelled()) {
            serverConnectEvent.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onServerConnected(ServerConnectedEvent serverConnectedEvent) {
        Server server = serverConnectedEvent.getServer();
        if (server != null && !this.pl.sender.syncedServers.contains(server)) {
            if (server.getInfo() != null) {
                this.pl.proxy.getScheduler().schedule((Plugin)this.pl, (Runnable)new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Listeners.this.pl.sender.reSync(server);
                        }
                        catch (NullPointerException ex) {
                            ex.printStackTrace();
                        }
                    }
                }, 250L, TimeUnit.MILLISECONDS);
            }
            else {
                this.pl.utils.debug("ServerConnectedEvent server object getInfo was null. " + server.toString());
            }
        }
    }
    
    @EventHandler
    public void onProxyLeave(PlayerDisconnectEvent playerDisconnectEvent) {
        ProxiedPlayer player = playerDisconnectEvent.getPlayer();
        if (player != null) {
            String string = player.getUniqueId().toString();
            if (this.pl.serverManager.startQueue.containsKey(string)) {
                this.pl.serverManager.startQueue.remove(string);
            }
        }
    }
    
    @EventHandler(priority = 64)
    public void onServerCreateStart(ServerCreateEvent serverCreateEvent) {
        if (!serverCreateEvent.isCancelled()) {
            String string = serverCreateEvent.getOwnerId().toString();
            if (!this.creating.containsKey(string)) {
                this.creating.put(string, System.currentTimeMillis());
            }
            else {
                this.creating.remove(string);
                this.creating.put(string, System.currentTimeMillis());
            }
        }
    }
    
    @EventHandler(priority = 64)
    public void onServerCreateFinish(ServerCreateFinishEvent serverCreateFinishEvent) {
        String string = serverCreateFinishEvent.getOwnerId().toString();
        if (this.creating.containsKey(string)) {
            this.creating.remove(string);
        }
    }
    
    @EventHandler(priority = -64)
    public void onServerModify(ServerModifyEvent serverModifyEvent) {
        String string = serverModifyEvent.getModifiedUUID().toString();
        if (!this.creating.containsKey(string)) {
            this.pl.serverManager.verifySettings(string);
        }
    }
}
