package net.cakemine.playerservers.velocity.sync;

import net.cakemine.playerservers.velocity.*;
import net.kyori.adventure.pointer.Pointered;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class PluginSender
{
    PlayerServers pl;
    public List<RegisteredServer> syncedServers;
    public String guisSerialized;
    ObjectMapper mapper = new ObjectMapper();
    public PluginSender(PlayerServers pl) {
        this.syncedServers = new ArrayList<RegisteredServer>();
        this.pl = pl;
    }
    
    public void sendPluginMsg(RegisteredServer server, ByteArrayDataOutput byteArrayDataOutput) {
    	this.pl.proxy.getScheduler().buildTask(this.pl, () -> {
    		server.sendPluginMessage(MinecraftChannelIdentifier.from("playerservers:core"), Base64.getEncoder().encode(byteArrayDataOutput.toByteArray()));
    	}).delay(30L, TimeUnit.MILLISECONDS).schedule();
    }
    
    public void doSync(RegisteredServer server) {
        if (server.getPlayersConnected().size() > 0) {
           Player proxiedPlayer = server.getPlayersConnected().iterator().next();
            if (proxiedPlayer == null) {
                this.pl.utils.debug("Player was null when attempting doSync!");
                return;
            }
            String string = proxiedPlayer.getUniqueId().toString();
            this.sendStructuredMessage("reSync", string);
            String[] array2;
            String[] array = array2 = new String[] { "debug", "version", "messages", "prefix", "fallback", "useExpire", "expiredate", "daysleft", "blockedcmds", "alwaysops", "timeleft", "expirecheck", "templates", "psCustomCmd", "guis" };
            for (String s : array2) {
                if (this.pl.proxy.getPlayer(UUID.fromString(string)) == null) {
                    this.pl.utils.debug("Player returned null when sending server resync messages, aborting.");
                    return;
                }
                this.sendStructuredMessage(s, string);
            }
            this.confirmSync(server, array.length);
        }
    }
    
    public void confirmSync(RegisteredServer server, int n) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("finishSync");
        dataOutput.writeUTF(String.valueOf(n));
        this.sendPluginMsg(server, dataOutput);
    }
    
    public void reSync(RegisteredServer server) {
        if (server == null) {
            this.pl.utils.debug("Server was null when attempting a resync!");
            return;
        }
        if (this.syncedServers.size() > 0 && this.syncedServers.contains(server)) {
            this.syncedServers.remove(server);
        }
        if (server.getServerInfo() == null) {
            this.pl.utils.debug("Server info was null when attempting a resync!");
            return;
        }
        if (server.getPlayersConnected() == null) {
            this.pl.utils.debug("Server player collection was null when attempting a resync!");
            return;
        }
        if (server.getPlayersConnected().size() > 0) {
            this.doSync(server);
        }
    }
    
    public void reSyncAll() {
        this.syncedServers.clear();
        for (RegisteredServer server : this.pl.proxy.getAllServers()) {
            if (server.getPlayersConnected().size() > 0) {
                this.reSync(server);
            }
        }
    }
    
    public void controlGUI(Player proxiedPlayer) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        RegisteredServer server = proxiedPlayer.getCurrentServer().get().getServer();
        String uuid = proxiedPlayer.getUniqueId().toString();
        HashMap<String, Object> serverData = new HashMap<>();
        serverData.put("has-server", String.valueOf(this.pl.serverManager.hasServer(uuid)));
        serverData.put("use-expire", this.pl.utils.hasPerm(uuid, "playerservers.bypassexpire") ? "false" : String.valueOf(this.pl.useExpiry));
        serverData.put("time-left", String.valueOf(this.pl.expiryTracker.timeLeft(uuid)));
        serverData.put("files-exist", String.valueOf(this.pl.serverManager.serverFilesExist(uuid)));
        serverData.put("create-perm", String.valueOf(this.pl.utils.hasPerm(uuid, "playerservers.player") || this.pl.utils.hasPerm(uuid, "playerservers.ps.create")));
        serverData.put("delete-perm", String.valueOf(this.pl.utils.hasPerm(uuid, "playerservers.player") || this.pl.utils.hasPerm(uuid, "playerservers.ps.delete")));
                
        if (this.pl.serverManager.hasServer(uuid)) {
        	serverData.put("is-online", String.valueOf(!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(uuid), this.pl.utils.getSrvPort(uuid))));
            serverData.put("server-name", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("server-name")));
            serverData.put("expire-date", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("expire-date")));
            serverData.put("motd", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("motd")));
            serverData.put("memory", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("memory")));
            serverData.put("port", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("port")));
            serverData.put("player-name", String.valueOf( this.pl.serverManager.serverMap.get(uuid).getSetting("player-name")));
            serverData.put("white-list", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("white-list")));
            serverData.put("max-players", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("max-players")));
            serverData.put("server-ip", String.valueOf(this.pl.serverManager.serverMap.get(uuid).getSetting("server-ip")));
        } else {
        	serverData.put("is-online", String.valueOf(false));
            serverData.put("server-name", "none");
            serverData.put("expire-date", "1989-04-20 16:20");
            serverData.put("motd", "none");
            serverData.put("memory", "0M/0M");
            serverData.put("player-name", proxiedPlayer.getUsername());
            serverData.put("white-list", String.valueOf(false));
            serverData.put("max-players", "0");
            serverData.put("server-ip", "127.0.0.1");
        }
        
        
        try {
			String jsonString = mapper.writeValueAsString(serverData);
			dataOutput.writeUTF("controlGUI");
	        this.pl.utils.debug("Opening controlGUI with info: " + jsonString);
	        dataOutput.writeUTF(Base64.getEncoder().encodeToString(jsonString.getBytes()));
	        dataOutput.writeUTF(uuid);
	        this.sendPluginMsg(server, dataOutput);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void worldSelector(Player proxiedPlayer) {
    	this.pl.utils.debug("worldSelector Fired");
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        RegisteredServer server = proxiedPlayer.getCurrentServer().get().getServer();
        dataOutput.writeUTF("worldselect");
        dataOutput.writeUTF(proxiedPlayer.getUniqueId().toString());
        this.sendPluginMsg(server, dataOutput);
    }
    
    public void serverSelector(Player proxiedPlayer) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        RegisteredServer server = proxiedPlayer.getCurrentServer().get().getServer();
        dataOutput.writeUTF("serverselect");
        HashMap<String,  HashMap<String, String>> serverMap = new HashMap<>();
        serverMap.clear();
        for (String s : this.pl.serverManager.playerServers) {
        	HashMap<String, String> dataMap = new HashMap<>();
            String ownerId = this.pl.serverManager.getOwnerId(s);
            dataMap.put("owner-id", ownerId);
            dataMap.put("server-name", s);
            dataMap.put("owner-name", String.valueOf(this.pl.utils.getName(ownerId)));
            dataMap.put("template-name", String.valueOf(this.pl.serverManager.getServerTemplateName(s)));
            dataMap.put("motd", String.valueOf(this.pl.serverManager.getServerInfo(ownerId, "motd")));
            dataMap.put("current-players", String.valueOf(this.pl.proxy.getServer(s).get().getPlayersConnected().size()));
            dataMap.put("max-players", String.valueOf(this.pl.serverManager.getServerInfo(ownerId, "max-players")));
            dataMap.put("max-memory", String.valueOf(this.pl.serverManager.getServerInfo(ownerId, "memory")).split("\\/")[0]);
            dataMap.put("expire-date", String.valueOf(this.pl.serverManager.getServerInfo(ownerId, "expire-date")));
            dataMap.put("white-list", String.valueOf(this.pl.serverManager.getServerInfo(ownerId, "white-list")));
            dataMap.put("time-left", String.valueOf(this.pl.expiryTracker.timeLeft(ownerId)));
            serverMap.put(ownerId, dataMap);
        }
        
		try {
			String jsonString = mapper.writeValueAsString(serverMap);
			dataOutput.writeUTF(Base64.getEncoder().encodeToString(jsonString.getBytes()));
			dataOutput.writeUTF(proxiedPlayer.getUniqueId().toString());
	        this.sendPluginMsg(server, dataOutput);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void sendStructuredMessage(String s, String s2) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        if (s2 == null) {
            this.pl.utils.debug("Something went wrong sending a structured plugin message: UUID input was null.");
            return;
        }
        Player player = this.pl.proxy.getPlayer(UUID.fromString(s2)).get();
        if (player == null) {
            this.pl.utils.debug("Something went wrong sending a structured plugin message: player was null.");
            return;
        }
        RegisteredServer server = player.getCurrentServer().get().getServer();
        if (server == null) {
            this.pl.utils.debug("Something went wrong sending a structured plugin message: server was null.");
            return;
        }
        int n = 0;
        switch (s) {
            case "useExpire": {
                dataOutput.writeUTF("useExpire");
                if (this.pl.utils.hasPerm(this.pl.serverManager.getOwnerId(server.getServerInfo().getName()), "playerservers.bypassexpire")) {
                    dataOutput.writeUTF("false");
                    break;
                }
                dataOutput.writeUTF(String.valueOf(this.pl.useExpiry));
                break;
            }
            case "messages": {
                dataOutput.writeUTF("messages");
				try {
					String jsonString = mapper.writeValueAsString(this.pl.msgMap);
					dataOutput.writeUTF(Base64.getEncoder().encodeToString(jsonString.getBytes()));
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                break;
            }
            case "templates": {
                dataOutput.writeUTF("templates");
                HashMap<String, Object> templates = new HashMap<>();
                HashMap<String, Object> configSection = new HashMap<>();
                for (File file : this.pl.templateManager.templates.keySet()) {
                	Map<String, Object> configuration = this.pl.templateManager.templates.get(file);
                	configSection.put("name", (String)configuration.get("template-name"));
                	configSection.put("icon", (String)configuration.get("icon-material"));
                	configSection.put("desc", (String)configuration.get("description"));
                	configSection.put("OP", String.valueOf((boolean)configuration.get("creator-gets-op")));
                	templates.put(file.getName(), configSection);
                }
                
                this.pl.utils.debug("Synced template count = " + n);
				try {
					String jsonString = mapper.writeValueAsString(templates);
					this.pl.utils.debug("input = " + jsonString);
					dataOutput.writeUTF(Base64.getEncoder().encodeToString(jsonString.getBytes()));
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                break;
            }
            case "prefix": {
                dataOutput.writeUTF("prefix");
                dataOutput.writeUTF(this.pl.prefix);
                break;
            }
            case "fallback": {
                dataOutput.writeUTF("fallback");
                dataOutput.writeUTF(this.pl.fallbackSrv);
                break;
            }
            case "expiredate": {
                String expireDate = this.pl.expiryTracker.expireDate(this.pl.utils.getServerUUID(server.getServerInfo().getName()));
                dataOutput.writeUTF("expiredate");
                dataOutput.writeUTF(expireDate);
                break;
            }
            case "daysleft": {
                int daysLeft = this.pl.expiryTracker.daysLeft(this.pl.utils.getServerUUID(server.getServerInfo().getName()));
                dataOutput.writeUTF("daysleft");
                dataOutput.writeUTF(String.valueOf(daysLeft));
                break;
            }
            case "timeleft": {
                dataOutput.writeUTF("timeleft");
                dataOutput.writeUTF(this.pl.expiryTracker.timeLeft(this.pl.utils.getServerUUID(server.getServerInfo().getName())));
                break;
            }
            case "debug": {
                dataOutput.writeUTF("debug");
                if (this.pl.debug) {
                    dataOutput.writeUTF("1");
                    break;
                }
                dataOutput.writeUTF("0");
                break;
            }
            case "blockedcmds": {
                dataOutput.writeUTF("blockedcmds");
                StringBuilder sb3 = new StringBuilder();
                Iterator<String> iterator3 = this.pl.blockedCmds.iterator();
                while (iterator3.hasNext()) {
                    sb3.append(iterator3.next()).append("%%%");
                    ++n;
                }
                sb3.delete(sb3.length() - 3, sb3.length());
                this.pl.utils.debug("Synced Blocked Commands Count = " + n);
                dataOutput.writeUTF(sb3.toString());
                break;
            }
            case "alwaysops": {
                dataOutput.writeUTF("alwaysops");
                StringBuilder sb4 = new StringBuilder();
                Iterator<String> iterator4 = this.pl.alwaysOP.iterator();
                while (iterator4.hasNext()) {
                    sb4.append(iterator4.next()).append("%%%");
                    ++n;
                }
                sb4.delete(sb4.length() - 3, sb4.length());
                this.pl.utils.debug("Synced Always-OP List Count = " + n);
                dataOutput.writeUTF(sb4.toString());
                break;
            }
            case "psCustomCmd": {
                dataOutput.writeUTF("psCustomCmd");
                dataOutput.writeUTF(this.pl.psCommand);
                break;
            }
            case "expirecheck": {
                dataOutput.writeUTF("expirecheck");
                if (this.pl.utils.hasPerm(this.pl.serverManager.getOwnerId(server.getServerInfo().getName()), "playerservers.bypassexpire")) {
                    dataOutput.writeUTF("420");
                    break;
                }
                dataOutput.writeUTF(String.valueOf(this.pl.expiryTracker.msLeft(this.pl.serverManager.getOwnerId(server.getServerInfo().getName()))));
                break;
            }
            case "version": {
                dataOutput.writeUTF("version");
                dataOutput.writeUTF(this.pl.getDescription().getVersion().get());
                break;
            }
            case "guis": {
                dataOutput.writeUTF("guis");
                dataOutput.writeUTF(this.guisSerialized);
                break;
            }
            default: {
                return;
            }
        }
        this.pl.sender.sendPluginMsg(server, dataOutput);
    }
}
