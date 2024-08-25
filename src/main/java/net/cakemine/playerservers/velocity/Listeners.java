package net.cakemine.playerservers.velocity;

import java.util.regex.*;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.*;
import java.util.concurrent.*;
import net.cakemine.playerservers.velocity.events.*;
import net.cakemine.playerservers.velocity.objects.StoredPlayer;

public class Listeners {
    PlayerServers pl;
    private HashMap<String, Long> creating;
    
    public Listeners(PlayerServers pl) {
        this.creating = new HashMap<String, Long>();
        this.pl = pl;
    }
    
    @Subscribe
    public void onChat(PlayerChatEvent chatEvent) {
        String message = chatEvent.getMessage();
        Player proxiedPlayer = chatEvent.getPlayer();
        proxiedPlayer.getUniqueId().toString();
        if ((proxiedPlayer == null || !this.pl.serverManager.isPlayerServer(proxiedPlayer.getCurrentServer().get().getServerInfo().getName()) && message.matches("(/(myserver|mys|mysrv)($|\\s(.+)))"))) {
            this.pl.utils.sendMsg(proxiedPlayer, "&cYou can only use &o/myserver&c (&c&o/mys&c) while on &oa player server.");
            chatEvent.setResult(PlayerChatEvent.ChatResult.denied());
        }
        if (proxiedPlayer != null && this.pl.serverManager.isPlayerServer(proxiedPlayer.getCurrentServer().get().getServerInfo().getName()) && !this.pl.utils.hasPerm(proxiedPlayer, "playerservers.admin") && !this.pl.utils.hasPerm(proxiedPlayer, "playerservers.bypassblock")) {
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
                	chatEvent.setResult(PlayerChatEvent.ChatResult.denied());
                    this.pl.utils.log("Blocked command from " + proxiedPlayer.getUsername() + " > " + message);
                    this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("blocked-cmd").replaceAll("(%blocked-command%)", matcher.group(0)));
                    break;
                }
            }
        }
    }
    
    @Subscribe
    public void onJoin(PostLoginEvent postLoginEvent) {
        Player player = postLoginEvent.getPlayer();
        if (player != null) {
            String string = player.getUniqueId().toString();
            if (!this.pl.playerMap.containsKey(player.getUniqueId())) {
                this.pl.loadPlayer(player.getUniqueId(), new StoredPlayer(player.getUniqueId(), this.pl));
            }
            if (this.pl.serverManager.hasServer(player.getUniqueId().toString()) && !this.pl.serverManager.getServerInfo(string, "player-name").equals(player.getUsername())) {
                this.pl.utils.log(player.getUsername() + " has changed their name. Updating their server.");
                if (this.pl.utils.getSrvName(string).equals(this.pl.serverManager.getServerInfo(string, "player-name"))) {
                    this.pl.serverManager.setServerInfo(string, "server-name", player.getUsername());
                    this.pl.utils.log(player.getUsername() + "'s server name was the same as their old name, updating server name too.");
                }
                this.pl.serverManager.setServerInfo(string, "player-name", player.getUsername());
            }
        }
    }
    
    @Subscribe
    public void onKick(KickedFromServerEvent serverKickEvent) {
        Player player = serverKickEvent.getPlayer();
        if (player != null) {
            ServerInfo info = null;
            if (player.getCurrentServer() != null) {
                info = player.getCurrentServer().get().getServerInfo();
            }
            ServerInfo kicked = serverKickEvent.getServer().getServerInfo();
            if (this.pl.serverManager.isPlayerServer(info.getName())) {
                this.pl.utils.debug("Kicked from a player server!");
                serverKickEvent.setResult(KickedFromServerEvent.RedirectPlayer.create(this.pl.proxy.getServer(this.pl.fallbackSrv).get(), serverKickEvent.getServerKickReason().get()));
            }
            this.pl.utils.debug("currentSrv = " + String.valueOf(info) + " | kickedSrv = " + kicked);
            if ((info == null || info.equals(kicked)) && this.pl.usingHelper.containsKey(player)) {
                this.pl.utils.debug(serverKickEvent.getClass() + " fired, " + player.getUsername() + " removed from helper map.");
                this.pl.usingHelper.remove(player);
            }
        }
    }
    
    @Subscribe
    public void onStop(ServerStopEvent serverStopEvent) {
        if (this.pl.serverManager.isPlayerServer(serverStopEvent.getServerInfo().getName())) {
            for (Player proxiedPlayer : this.pl.proxy.getServer(serverStopEvent.getServerInfo().getName()).get().getPlayersConnected()) {
                if (!proxiedPlayer.getCurrentServer().get().getServerInfo().getName().equals(this.pl.fallbackSrv)) {
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
    
    @Subscribe
    public void onServerLeave(DisconnectEvent serverDisconnectEvent) {
        Player player = serverDisconnectEvent.getPlayer();
        if (player != null && !player.getCurrentServer().isEmpty()) {
            ServerConnection server = player.getCurrentServer().get();
            UUID uniqueId = player.getUniqueId();
            this.pl.playerMap.get(player.getUniqueId()).save();
            if (server != null && this.pl.serverManager.isPlayerServer(server.getServerInfo().getName())) {
                ServerLeaveEvent serverLeaveEvent = new ServerLeaveEvent(this.pl, server.getServerInfo(), uniqueId);
                this.pl.eventManager.fire(serverLeaveEvent);
            }
            if (this.pl.usingHelper.containsKey(player)) {
                this.pl.utils.debug(serverDisconnectEvent.getClass() + " fired, " + player.getUsername() + " removed from helper map.");
                this.pl.usingHelper.remove(player);
            }
        }
    }
    
    @Subscribe
    public void onServerSwitch(ServerConnectedEvent serverSwitchEvent) {
        Player player = serverSwitchEvent.getPlayer();        
        if (!serverSwitchEvent.getPreviousServer().isEmpty() && player != null && this.pl.usingHelper.containsKey(player)) {
            this.pl.utils.debug(serverSwitchEvent.getClass() + " fired, " + player.getUsername() + " removed from helper map.");
            this.pl.usingHelper.remove(player);
        }
    }
    
    @Subscribe
    public void onServerJoin(ServerConnectedEvent  serverConnectEvent) {
        ServerInfo target = serverConnectEvent.getServer().getServerInfo();
        UUID uniqueId = serverConnectEvent.getPlayer().getUniqueId();
        ServerJoinEvent serverJoinEvent = new ServerJoinEvent(this.pl, target, uniqueId);
        this.pl.eventManager.fire(serverJoinEvent);
        if (target != null && this.pl.serverManager.isPlayerServer(target.getName()) && !serverJoinEvent.getResult().isAllowed()) {
        	serverConnectEvent.getPlayer().createConnectionRequest(serverConnectEvent.getPreviousServer().get()).connect();
        }
    }
    
    @Subscribe
    public void onServerConnected(ServerConnectedEvent serverConnectedEvent) {
        RegisteredServer server = serverConnectedEvent.getServer();
        if (server != null && !this.pl.sender.syncedServers.contains(server)) {
            if (server.getServerInfo() != null) {
            	this.pl.proxy.getScheduler()
            	  .buildTask(this.pl, () -> {
            		  try {
                          Listeners.this.pl.sender.reSync(server);
                      }
                      catch (NullPointerException ex) {
                          ex.printStackTrace();
                      }
            	  })
            	  .delay(250L, TimeUnit.MILLISECONDS)
            	  .schedule();
            }
            else {
                this.pl.utils.debug("ServerConnectedEvent server object getInfo was null. " + server.toString());
            }
        }
    }
    
    @Subscribe
    public void onProxyLeave(DisconnectEvent playerDisconnectEvent) {
        Player player = playerDisconnectEvent.getPlayer();
        if (player != null) {
            String string = player.getUniqueId().toString();
            if (this.pl.serverManager.startQueue.containsKey(string)) {
                this.pl.serverManager.startQueue.remove(string);
            }
        }
    }
    
    @Subscribe
    public void onServerCreateStart(ServerCreateEvent serverCreateEvent) {
        if (serverCreateEvent.getResult().isAllowed()) {
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
    
    @Subscribe
    public void onServerCreateFinish(ServerCreateFinishEvent serverCreateFinishEvent) {
        String string = serverCreateFinishEvent.getOwnerId().toString();
        if (this.creating.containsKey(string)) {
            this.creating.remove(string);
        }
    }
    
    @Subscribe
    public void onServerModify(ServerModifyEvent serverModifyEvent) {
        String string = serverModifyEvent.getModifiedUUID().toString();
        if (!this.creating.containsKey(string)) {
            this.pl.serverManager.verifySettings(string);
        }
    }
}
