package net.cakemine.playerservers.velocity;

import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.concurrent.*;
import java.nio.file.attribute.*;
import java.nio.file.*;
import net.cakemine.playerservers.velocity.events.*;
import net.cakemine.playerservers.velocity.objects.PlayerServer;
import net.cakemine.playerservers.velocity.objects.PlayerServer.Status;

import java.util.*;

public class ServerManager
{
    PlayerServers pl;
    ProxyServer proxy;
    public LinkedHashMap<String, PlayerServer> serverMap;
    public List<String> playerServers;
    public int allocatedRam;
    public HashMap<String, Object> addedServers;
    public LinkedHashMap<String, String> startQueue;
    
    public ServerManager(PlayerServers pl) {
        this.proxy = pl.proxy;
        this.serverMap = new LinkedHashMap<String, PlayerServer>();
        this.playerServers = new LinkedList<String>();
        this.allocatedRam = 0;
        this.addedServers = new HashMap<String, Object>();
        this.startQueue = new LinkedHashMap<String, String>();
        this.pl = pl;
    }
    
    public void startupSrv(String serverUUID, CommandSource commandSource) {
    	if (pl.serverManager.serverMap.get(serverUUID.toString()).getStatus() == Status.STOPPED) {
        	pl.serverManager.serverMap.get(serverUUID.toString()).setStatus(Status.STARTING);

            String srvName = pl.utils.getSrvName(serverUUID);
            String serversFolder = serverUUID;
            String startMem = getMemorySetting(serverUUID, "default-Xmx").split("/")[0];
            String maxMem = getMemorySetting(serverUUID, "default-Xms").split("/")[1];

            ServerStartEvent serverStartEvent = new ServerStartEvent(pl, null, UUID.fromString(serverUUID),
                    pl.utils.memStringToInt(startMem), pl.utils.memStringToInt(maxMem));
            this.pl.eventManager.fire(serverStartEvent);

            if (serverStartEvent.getResult().isAllowed()) {
                startServer(serverUUID, srvName, serversFolder, startMem, maxMem, commandSource);
            } else {
            	pl.serverManager.serverMap.get(serverUUID.toString()).setStatus(Status.STOPPED);
            }
        } else if (pl.serverManager.serverMap.get(serverUUID.toString()).getStatus() == Status.INSTALLING) {
        	pl.utils.log("Server is still installing " + serverUUID);
        } else {
            pl.utils.log("Server already running for uuid " + serverUUID);
        }
    }

    private void startServer(String serverUUID, String srvName, String serversFolder, String startMem, String maxMem, CommandSource commandSource) {
    	if (!serversFolder.matches("^[a-zA-Z0-9_-]+$")) {
            pl.utils.log(Level.SEVERE, "Invalid server folder name: " + serversFolder);
            return;
        }
        ServerInfo serverInfo = createServerInfo(serverUUID, srvName);
        if (serverFilesExist(serverUUID)) {
            if (getJar(serverUUID) == null) {
                pl.utils.log(Level.SEVERE, "Failed to start server (" + serverUUID + "). JAR file not found.");
                pl.serverManager.serverMap.get(serverUUID.toString()).setStatus(Status.STOPPED);
            } else {
                executeServerStart(serverUUID, serversFolder, String.valueOf(serverInfo.getAddress().getPort()), pl.utils.getSrvMaxPlayers(serverUUID), startMem, maxMem, srvName, serverInfo, commandSource);
            }
        } else {
            notifyMissingServerFiles(serverUUID, commandSource);
        }
    }

    private ServerInfo createServerInfo(String serverUUID, String srvName) {
        return new ServerInfo(srvName, new InetSocketAddress(this.pl.utils.getSrvIp(serverUUID), this.pl.utils.getSrvPort(serverUUID)));
    }

    private void executeServerStart(String serverUUID, String serversFolder, String port, String maxPlayers, String startMem, String maxMem, String srvName, ServerInfo serverInfo, CommandSource commandSource) {
        String[] command = buildStartupCommand(serverUUID, serversFolder, port, maxPlayers, startMem, maxMem);
        proxy.getScheduler().buildTask(pl, () -> {
        	if (pl.wrapper.equalsIgnoreCase("screen") || pl.wrapper.equalsIgnoreCase("tmux")) {
        		try {
                	ProcessBuilder processBuilder = new ProcessBuilder(new String[0]);
                	processBuilder.command(command);
                	processBuilder.directory(new File(this.pl.serversFolder, serversFolder));
                	processBuilder.start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        	} else {
        		StringBuilder sb = new StringBuilder();
        		sb.append("+start ")
                .append(pl.utils.getSrvName(serverUUID)).append(" ")
                .append(new File(pl.resolveServersFolder()).getAbsolutePath() + File.separator + serversFolder.replace(" ", "\\/")).append(File.separator).append(" ")
                .append(port).append(" ")
                .append(maxPlayers).append(" ")
                .append(startMem).append(" ")
                .append(maxMem).append(" ").append(getJar(serverUUID));
                pl.ctrl.send(sb.toString());
        	}
            
        }).schedule();
        pl.utils.log("Started player server for uuid " + serverUUID);
        addVelocity(srvName, pl.utils.getSrvIp(serverUUID), pl.utils.getSrvPort(serverUUID), getServerInfo(serverUUID, "motd"), 3);
        this.proxy.getScheduler().buildTask(this.pl, () -> {
        	Executors.newSingleThreadExecutor().submit(() -> {
        	    Pattern pattern = Pattern.compile("Done \\(\\d+\\.\\d+s\\)! For help, type \"help\"");
        	    boolean isRunning = true;
        	    try {
        	        while (isRunning) {
        	            BufferedReader reader = pl.serverManager.serverMap.get(serverUUID.toString()).getServerLog(); // Get the current log reader
        	            String line;

        	            // Read lines if available
        	            while ((line = reader.readLine()) != null) {
        	                Matcher running = pattern.matcher(line);
        	                if (running.find()) {
        	                    pl.serverManager.serverMap.get(serverUUID.toString()).setStatus(Status.RUNNING);
        	                    Player owner = proxy.getPlayer(srvName).get();
        	                    if (owner != null) {
        	                        pl.utils.sendMsg(owner, "&eConnecting to '" + srvName + "', please wait..");
        	                        this.pl.utils.movePlayer(owner, srvName, this.pl.joinDelay);
        	                    }
        	                    reader.close();
        	                    isRunning = false;
        	                    break;
        	                }
        	            }
        	            
        	            if (isRunning) {
        	            	try {
            	                Thread.sleep(15000); // Wait 500 milliseconds before trying again
            	            } catch (InterruptedException e) {
            	                Thread.currentThread().interrupt();
            	                break;
            	            }
        	            }

        	        }
        	    } catch (IOException e) {
        	        e.printStackTrace();
        	        pl.serverManager.serverMap.get(serverUUID.toString()).setStatus(Status.STOPPED);
        	    }
        	});
        }).delay(15, TimeUnit.SECONDS).schedule();
    }

    private String[] buildStartupCommand(String serverUUID, String serversFolder, String port, String maxPlayers, String startMem, String maxMem) {
        String jarFile = getJar(serverUUID);
        String srvName = pl.utils.getSrvName(serverUUID);

        // Validate parameters
        if (!isValidParameter(srvName) || !isValidParameter(serversFolder) || !isValidParameter(port) || !isValidParameter(maxPlayers) || !isValidParameter(startMem) || !isValidParameter(maxMem) || !isValidParameter(jarFile)) {
            throw new IllegalArgumentException("Invalid parameter provided for server startup command.");
        }
        return new File(pl.configManager.getDataFolder().getAbsolutePath() + File.separator + "scripts" + File.separator + "start-screen.sh").exists() ?
        		new String[]{"sh", pl.configManager.getDataFolder().getAbsolutePath() + File.separator + "scripts" + File.separator + "start-screen.sh", serverUUID, srvName, serversFolder, startMem, maxMem, jarFile} :
        			new String[]{"screen", "-dmS", srvName, "java", "-Xmx" + startMem, "-Xms" + maxMem, "-jar", jarFile};
    }


    private void notifyMissingServerFiles(String serverUUID, CommandSource commandSource) {
        if (commandSource != null) {
            if (commandSource instanceof Player && ((Player) commandSource).getUniqueId().toString().equals(serverUUID)) {
                pl.utils.sendTitle((Player) commandSource, pl.utils.doPlaceholders(serverUUID, pl.msgMap.get("no-server-title")));
            } else {
                pl.utils.sendMsg(commandSource, pl.utils.doPlaceholders(serverUUID, pl.msgMap.get("other-no-server")));
            }
        }
        pl.utils.debug(pl.utils.getName(serverUUID) + " Server files don't exist on startup. Could be normal, ex: Server was deleted.");
        pl.serverManager.serverMap.get(serverUUID.toString()).setStatus(Status.STOPPED);
    }

    private String getMemorySetting(String serverUUID, String defaultKey) {
        String memorySetting = pl.templateManager.getTemplateSetting(pl.templateManager.getTemplateFile(getServerTemplateName(pl.utils.getSrvName(serverUUID))), defaultKey);
        if (getServerInfo(serverUUID, "memory") != null && !getServerInfo(serverUUID, "memory").isEmpty()) {
            memorySetting = getServerInfo(serverUUID, "memory");
        }
        return memorySetting;
    }
    
    public void stopSrv(String s) {
    	if (pl.serverManager.serverMap.get(s).getStatus() == Status.RUNNING) {
    		pl.serverManager.serverMap.get(s).setStatus(Status.STOPPING);
	        String srvName = this.pl.utils.getSrvName(s);
	        ServerInfo constructServerInfo = new ServerInfo(srvName, new InetSocketAddress(this.pl.utils.getSrvIp(s), this.pl.utils.getSrvPort(s)));
	        ServerStopEvent serverStopEvent = new ServerStopEvent(this.pl, constructServerInfo, UUID.fromString(s), PlayerServers.getApi().getServerXmx(srvName), PlayerServers.getApi().getServerXms(srvName));
	        this.pl.eventManager.fire(serverStopEvent);
	        if (serverStopEvent.getResult().isAllowed()) {	        	
	            Iterator iterator = this.proxy.getServer(constructServerInfo.getName()).get().getPlayersConnected().iterator();
	            while (iterator.hasNext()) {
	                this.pl.utils.movePlayer((Player) iterator.next(), this.pl.fallbackSrv, 0);
	            }
	            this.proxy.getScheduler()
	            .buildTask(this.pl, () -> {
	            	wrapperStop(s);
	            	pl.serverManager.serverMap.get(s).setStatus(Status.STOPPED);
                    pl.serverManager.removeVelocity(srvName);
	            })
	            .delay(1000L, TimeUnit.MILLISECONDS)
	            .schedule();
	        }
    	} else {
    		this.pl.utils.log("Server is not running for uuid " + s);
    	}
    }
    
    public void stopAll(CommandSource commandSender) {
        ArrayList<String> list = new ArrayList<String>();
        for (String s : this.playerServers) {
            String ownerId = this.pl.serverManager.getOwnerId(s);
            ServerInfo constructServerInfo = new ServerInfo(s, new InetSocketAddress(this.pl.utils.getSrvIp(ownerId), this.pl.utils.getSrvPort(ownerId)));
            ServerStopEvent serverStopEvent = new ServerStopEvent(this.pl, constructServerInfo, UUID.fromString(ownerId), PlayerServers.getApi().getServerXmx(s), PlayerServers.getApi().getServerXms(s));
            this.pl.eventManager.fire(serverStopEvent);
            if (serverStopEvent.getResult().isAllowed()) {
                Iterator iterator2 = this.proxy.getServer(constructServerInfo.getName()).get().getPlayersConnected().iterator();
                while (iterator2.hasNext()) {
                    this.pl.utils.movePlayer((Player) iterator2.next(), this.pl.fallbackSrv, 0);
                }
                this.wrapperStop(ownerId);
                pl.serverManager.serverMap.get(ownerId.toString()).setStatus(Status.STOPPED);
                if (this.serverExists(s)) {
                    ServerRemoveEvent serverRemoveEvent = new ServerRemoveEvent(this.pl, this.pl.proxy.getServer(s).get().getServerInfo(), UUID.fromString(ownerId), PlayerServers.getApi().getServerXmx(s), PlayerServers.getApi().getServerXms(s));
                    this.pl.eventManager.fire(serverRemoveEvent);
                    if (serverRemoveEvent.getResult().isAllowed()) {
                        this.pl.utils.log("&eRemoved server " + s + " from velocity servers.");
                        this.pl.proxy.getAllServers().remove(this.pl.proxy.getServer(s).get());
                        list.add(s);
                        if (this.addedServers != null && this.addedServers.containsKey(s)) {
                            this.addedServers.remove(s);
                            this.pl.online.put("servers", this.addedServers);
                            this.pl.configManager.saveConfig(this.pl.online, "online.yml");
                        }
                        this.countRam();
                    }
                }
                else {
                    this.pl.utils.log(Level.WARNING, "&cTried to remove server \"" + s + "\" but it doesn't exist!");
                }
                if (commandSender == null) {
                    continue;
                }
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(ownerId, this.pl.msgMap.get("server-stop-online-guest")));
            }
        }
        this.playerServers.removeAll(list);
    }
    
    public void wrapperStop(String s) {
        if (this.pl.wrapper.equalsIgnoreCase("screen")) {
            String[] array;
            if (new File(this.pl.configManager.getDataFolder().getAbsolutePath() + File.separator + "scripts" + File.separator + "stop-screen.sh").exists()) {
                array = new String[] { "sh", this.pl.configManager.getDataFolder().getAbsolutePath() + File.separator + "scripts" + File.separator + "stop-screen.sh", this.pl.utils.getSrvName(s) };
            }
            else {
                array = new String[] { "screen", "-S", this.pl.utils.getSrvName(s), "-p", "0", "-X", "stuff", "stop \\\r" };
            }
            String[] array2 = array;
            this.pl.utils.debug("Shutdown command to run: " + Arrays.toString(array2));
            this.proxy.getScheduler()
            .buildTask(this.pl, () -> {
            	ProcessBuilder command = new ProcessBuilder(new String[0]).command(array2);
                try {
                    command.start();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).schedule();
        }
        else if (!this.pl.wrapper.equalsIgnoreCase("tmux")) {
            if (this.pl.wrapper.equalsIgnoreCase("remote") || this.pl.wrapper.equalsIgnoreCase("default")) {
                this.pl.ctrl.send("+stop " + this.pl.utils.getSrvName(s));
            }
        }
    }
    
    public void killServer(String s) {
        if (this.pl.wrapper.equalsIgnoreCase("screen")) {
            String[] array = { "screen", "-S", this.pl.utils.getSrvName(s), "-X", "quit" };
            this.pl.utils.debug("Kill command to run: " + Arrays.toString(array));
            this.proxy.getScheduler()
            .buildTask(this.pl, () -> {
            	ProcessBuilder command = new ProcessBuilder(new String[0]).command(array);
                try {
                    command.start();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).schedule();
        }
        else if (!this.pl.wrapper.equalsIgnoreCase("tmux")) {
            if (this.pl.wrapper.equalsIgnoreCase("remote") || this.pl.wrapper.equalsIgnoreCase("default")) {
                this.pl.ctrl.send("+kill " + this.pl.utils.getSrvName(s));
            }
        }
        
        pl.serverManager.serverMap.get(s).setStatus(Status.STOPPED);
        
        this.removeVelocity(this.pl.utils.getSrvName(s));
    }
    
    public void addVelocity(String serverName, String address, Integer port, String motd, int timer) {
        ServerInfo constructServerInfo = new ServerInfo(serverName, new InetSocketAddress(address, port));
        ServerAddEvent serverAddEvent;
        
        
        if (this.isPlayerServer(serverName)) {
            serverAddEvent = new ServerAddEvent(this.pl, constructServerInfo, UUID.fromString(this.pl.utils.getServerUUID(serverName)), PlayerServers.getApi().getServerXmx(serverName), PlayerServers.getApi().getServerXms(serverName));
        }
        else {
            serverAddEvent = new ServerAddEvent(this.pl, constructServerInfo, UUID.fromString("00000000-0000-0000-0000-000000000000"), 0, 0);
        }
        this.pl.eventManager.fire(serverAddEvent);
        if (serverAddEvent.getResult().isAllowed()) {
        	this.proxy.getScheduler()
        	  .buildTask(this.pl, () -> {
        		  if (!serverExists(serverName)) {
                      pl.utils.log("&eAdded server " + serverName + " to velocity servers.");
                      RegisteredServer newServer = this.proxy.createRawRegisteredServer(constructServerInfo);
                      this.proxy.registerServer(newServer.getServerInfo());
                      playerServers.add(serverName);
                  }
                  else {
                      pl.utils.log(Level.WARNING, "&cTried to add server \"" + serverName + "\" but it already exists!");
                  }
        	  })
        	  .delay(timer, TimeUnit.SECONDS)
        	  .schedule();
            if (this.addedServers != null && (!this.addedServers.containsKey(serverName) || !((HashMap) this.addedServers.get(serverName)).get("port").equals(String.valueOf(port)) || !((HashMap) this.addedServers.get(serverName)).get("address").equals(address))) {
                HashMap<String, String> hashMap = new HashMap<String, String>();
                hashMap.put("address", address);
                hashMap.put("port", port.toString());
                hashMap.put("time", String.valueOf(System.currentTimeMillis()));
                hashMap.put("motd", motd);
                this.addedServers.put(serverName, hashMap);
                this.pl.online.put("servers", this.addedServers);
                this.pl.configManager.saveConfig(this.pl.online, "online.yml");
            }
            this.countRam();
        }
    }
    
    public boolean removeVelocity(String s) {
        if (!this.serverExists(s)) {
            this.pl.utils.log(Level.WARNING, "&cTried to remove server \"" + s + "\" but it doesn't exist!");
            return false;
        }
        ServerRemoveEvent serverRemoveEvent;
        if (this.isPlayerServer(s)) {
            serverRemoveEvent = new ServerRemoveEvent(this.pl, this.pl.proxy.getServer(s).get().getServerInfo(), UUID.fromString(this.pl.utils.getServerUUID(s)), PlayerServers.getApi().getServerXms(s), PlayerServers.getApi().getServerXms(s));
        }
        else {
            serverRemoveEvent = new ServerRemoveEvent(this.pl, this.pl.proxy.getServer(s).get().getServerInfo(), UUID.fromString("00000000-0000-0000-0000-000000000000"), 0, 0);
        }
        this.pl.eventManager.fire(serverRemoveEvent);
        if (serverRemoveEvent.getResult().isAllowed()) {
            this.pl.utils.log("&eRemoved server " + s + " from velocity servers.");
            this.pl.proxy.unregisterServer(this.proxy.getServer(s).get().getServerInfo());
            this.playerServers.remove(s);
            if (this.addedServers != null && this.addedServers.containsKey(s)) {
                this.addedServers.remove(s);
                this.pl.online.put("servers", this.addedServers);
                this.pl.configManager.saveConfig(this.pl.online, "online.yml");
            }
            this.countRam();
            return true;
        }
        return false;
    }
    
    public boolean createServer(Player Player, File file) {
        return this.createServer((CommandSource)Player, Player.getUsername(), Player.getUniqueId().toString(), file);
    }
    
    public boolean createServer(CommandSource commandSource, String s, String s2, File templateFile) {
    	Player commandSender = null;
    	if (commandSource instanceof Player) {
    		commandSender = (Player)commandSource;
    	}
        if (!this.pl.templateManager.templateDone()) {
            if (commandSource != null) {
                this.pl.utils.sendMsg(commandSource, "&c&lYou must setup a server jar before creating servers!||&e&oPut a server .jar file in the||&e&oBungee plugins/PlayerServers/templates/default folder.");
            }
            return false;
        }
        ServerCreateEvent serverCreateEvent = new ServerCreateEvent(this.pl, UUID.fromString(s2), s, this.pl.utils.getNextPort(), this.pl.utils.memStringToInt(this.pl.templateManager.getTemplateSetting(templateFile, "default-Xmx")), this.pl.utils.memStringToInt(this.pl.templateManager.getTemplateSetting(templateFile, "default-Xms")), this.pl.templateManager.getTemplateSetting(templateFile, "template-name"));
        this.pl.eventManager.fire(serverCreateEvent);
        if (!serverCreateEvent.getResult().isAllowed()) {
            return false;
        }
        if (this.pl.serverManager.hasServer(s2) && this.serverFilesExist(s2)) {
            this.pl.utils.log(Level.WARNING, "Tried to create a new server for " + this.pl.utils.getName(s2) + ", but this player already has a server!");
            if (commandSource != null) {
                this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("other-have-server")));
            }
            return false;
        }
        this.pl.utils.debug("templateFolder was: " + templateFile.getName() + " | getTemplate returns: " + serverCreateEvent.getTemplate());
        templateFile = this.pl.templateManager.getTemplateFile(serverCreateEvent.getTemplate());
        if (commandSource != null && !this.pl.utils.hasPerm(commandSource, "playerservers.templates.*") && !this.pl.utils.hasPerm(commandSource, "playerservers.templates.all") && this.pl.utils.hasPerm(commandSource, "playerservers.templates." + templateFile.getName()) && (this.pl.utils.hasPerm(commandSource, "playerservers.templates." + this.pl.templateManager.getTemplateSetting(templateFile, "template-name")) || this.pl.utils.hasPerm(commandSource, "playerservers.template.*") || this.pl.utils.hasPerm(commandSource, "playerservers.template.all")) && this.pl.utils.hasPerm(commandSource, "playerservers.template." + templateFile.getName()) && !this.pl.utils.hasPerm(commandSource, "playerservers.template." + this.pl.templateManager.getTemplateSetting(templateFile, "template-name"))) {
            this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("no-template-permissions")));
            return false;
        }
        if (!templateFile.exists()) {
            if (commandSource != null) {
                this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("create-missing-template")).replaceAll("%template-name%", serverCreateEvent.getTemplate()));
            }
            return false;
        }
        if (this.copyTemplate(s2, s, String.valueOf(this.pl.utils.getNextPort()), templateFile)) {
            if (commandSource != null) {
                this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("create-copying-files")).replaceAll("%template-name%", this.pl.templateManager.getTemplateSetting(templateFile, "template-name")));
            }
            
            this.setServerInfo(s2, "player-name", s);
            this.setServerInfo(s2, "server-name", s);
            this.setServerInfo(s2, "server-ip", this.pl.settingsManager.getSetting(s2, "server-ip"));
            this.setServerInfo(s2, "port", String.valueOf(this.pl.utils.getNextPort()));
            this.setServerInfo(s2, "max-players", this.pl.settingsManager.getSetting(s2, "max-players"));
            this.setServerInfo(s2, "motd", this.pl.settingsManager.getSetting(s2, "motd"));
            this.setServerInfo(s2, "white-list", this.pl.settingsManager.getSetting(s2, "white-list"));
            this.pl.utils.iteratePort();
            this.setServerInfo(s2, "memory", this.pl.templateManager.getTemplateSetting(templateFile, "default-Xms") + "/" + this.pl.templateManager.getTemplateSetting(templateFile, "default-Xmx"));
            if (this.pl.resetExpiry || (this.serverMap.containsKey(s2) && (this.serverMap.get(s2).getSetting("expire-date") == null || this.serverMap.get(s2).getSetting("expire-date").isEmpty()))) {
                this.setServerInfo(s2, "expire-date", "1989-04-20 16:20");
                this.pl.expiryTracker.addTime(s2, this.pl.templateManager.expireTime(templateFile), this.pl.templateManager.expireUnit(templateFile));
            }
            pl.serverManager.serverMap.get(s2).setStatus(Status.INSTALLING);
            this.pl.utils.log("Created server for player " + this.pl.utils.getName(s2));
            if (commandSource != null) {
                this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("create-finished")));
                if (this.pl.useExpiry && !this.pl.utils.hasPerm(commandSource, "playerservers.bypassexpire")) {
                    this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("expire-times")));
                }
                if (!this.pl.utils.getName(s2).equals(commandSender.getUsername())) {
                    this.pl.utils.log("Server created by " + commandSender.getUsername());
                }
            }
            this.pl.eventManager.fire(new ServerCreateFinishEvent(this.pl, UUID.fromString(s2), s, this.pl.utils.getNextPort(), this.pl.utils.memStringToInt(this.pl.templateManager.getTemplateSetting(templateFile, "default-Xmx")), this.pl.utils.memStringToInt(this.pl.templateManager.getTemplateSetting(templateFile, "default-Xms")), this.pl.templateManager.getTemplateSetting(templateFile, "template-name")));
            pl.serverManager.serverMap.get(s2).setStatus(Status.STOPPED);
            return true;
        }
        if (commandSource != null) {
            this.pl.utils.sendMsg(commandSource, this.pl.utils.doPlaceholders(s2, this.pl.msgMap.get("create-failed-copy")).replaceAll("%template-name%", this.pl.templateManager.getTemplateSetting(templateFile, "template-name")));
        }
        pl.serverManager.serverMap.get(s2).setStatus(Status.STOPPED);
        return false;
    }
    
    public void deleteServer(CommandSource commandSource, String s) {
        String srvName = this.pl.utils.getSrvName(s);
        ServerDeleteEvent serverDeleteEvent = new ServerDeleteEvent(this.pl, UUID.fromString(s), srvName, this.pl.utils.getSrvPort(s), PlayerServers.getApi().getServerXmx(srvName), PlayerServers.getApi().getServerXms(srvName));
        this.pl.eventManager.fire(serverDeleteEvent);
        if (serverDeleteEvent.getResult().isAllowed()) {
            if (this.serverFilesExist(s)) {
            	this.proxy.getScheduler()
                .buildTask(this.pl, () -> {
                	int n = 0;
                    int n2 = 1;
                    while (!pl.utils.isPortOpen(pl.utils.getSrvIp(s), pl.utils.getSrvPort(s))) {
                        if (n2 != 0) {
                            stopSrv(s);
                            n2 = 0;
                            pl.utils.log("Waiting for the server to shut down before deleting...");
                        }
                        if (n > 5) {
                            break;
                        }
                        ++n;
                        try {
                            Thread.sleep(5000L);
                        }
                        catch (InterruptedException ex) {
                            pl.utils.log(Level.WARNING, "RAWWWR you woke me from my slumber!");
                        }
                    }
                    if (commandSource != null) {
                        pl.utils.sendMsg(commandSource, pl.utils.doPlaceholders(s, pl.utils.doPlaceholders(s, pl.msgMap.get("start-delete"))));
                    }
                    boolean doDelete = doDelete(getServerFolder(s));
                    if (commandSource != null) {
                        if (!doDelete) {
                            pl.utils.sendMsg(commandSource, pl.utils.doPlaceholders(s, pl.msgMap.get("finish-delete-problem")));
                        }
                        else {
                        	serverMap.remove(s);
                            pl.utils.sendMsg(commandSource, pl.utils.doPlaceholders(s, pl.msgMap.get("finish-delete")));
                        }
                    }
                })
                .delay(2L, TimeUnit.SECONDS)
                .schedule();
            }
            else if (commandSource != null) {}
        }
    }
    
    public boolean doDelete(File file) {
        if (file.isDirectory()) {
            String[] list = file.list();
            for (int length = list.length, i = 0; i < length; ++i) {
                this.doDelete(new File(file, list[i]));
            }
            return file.delete();
        }
        return file.delete();
    }
    
    public String doMotdPlaceholders(String s, File file, String s2) {
        try {
            if (s2.contains("%owner-uuid%")) {
                s2 = s2.replaceAll("%owner-uuid%", s);
            }
            if (s2.contains("%owner-name%")) {
                s2 = s2.replaceAll("%owner-name%", this.pl.utils.getName(s));
            }
            if (s2.contains("%max-players%")) {
                s2 = s2.replaceAll("%max-players%", this.pl.settingsManager.getSetting(s, "max-players"));
            }
            if (s2.contains("%template-name")) {
                s2 = s2.replaceAll("%template-name%", this.pl.templateManager.getTemplateSetting(file, "template-name"));
            }
        }
        catch (Exception ex) {
            this.pl.utils.log(Level.WARNING, "Tried to replace a placeholder in message, but the value was null! Please send this stack trace to the developer!");
            this.pl.utils.log(Level.WARNING, "input: " + s2);
            ex.printStackTrace();
        }
        return s2;
    }
    
    public boolean copyTemplate(String serverUUID, String serverName, String serverPort, File file) {
        File serverFolder = this.getServerFolder(serverUUID);
        if (!file.exists()) {
            return false;
        }
        FutureTask futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                doCopy(file, serverFolder);
                pl.settingsManager.changeSetting(serverUUID, "server-name", serverName);
                pl.settingsManager.changeSetting(serverUUID, "server-port", serverPort);
                pl.settingsManager.changeSetting(serverUUID, "motd", doMotdPlaceholders(serverUUID, file, pl.settingsManager.getSetting(serverUUID, "motd")));
                return true;
            }
        });
        this.proxy.getScheduler()
        .buildTask(this.pl, () -> {
        	futureTask.run();
        })
        .delay(2L, TimeUnit.SECONDS)
        .schedule();
        try {
            return (boolean) futureTask.get(90L, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        catch (ExecutionException ex2) {
            ex2.printStackTrace();
        }
        catch (TimeoutException ex3) {
            ex3.printStackTrace();
        }
        return false;
    }
    
    public void doCopy(File file, File file2) {
        if (file.isDirectory()) {
            if (!file2.exists()) {
                file2.mkdir();
            }
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File file3 : listFiles) {
                    this.doCopy(file3, new File(file2, file3.getName()));
                }
            }
            else {
                this.pl.utils.log(Level.SEVERE, "#listFiles returned null on a folder (WTF?), skipping " + file.getAbsolutePath());
            }
        }
        else if (!this.pl.usingWindows && Files.isSymbolicLink(file.toPath())) {
            try {
                Files.createSymbolicLink(file2.toPath(), file.toPath().toAbsolutePath(), (FileAttribute<?>[])new FileAttribute[0]);
            }
            catch (FileAlreadyExistsException ex3) {
                this.pl.utils.debug("Linked template file already existed when trying to link to player's server.");
            }
            catch (IOException ex) {
                this.pl.utils.log(Level.SEVERE, "Failed to link template file (" + file.getPath() + "). Please send this stack trace to the developer.");
                ex.printStackTrace();
            }
            catch (UnsupportedOperationException ex4) {
                this.pl.utils.log(Level.WARNING, "Failed to create symbolic link for from template folder, creating a copy instead.");
                this.pl.utils.log(Level.WARNING, "Symbolic links may not be available on Windows, or certain file systems.");
                this.pl.utils.log(Level.WARNING, "Be sure to update ALL player server files when updating PlayerServers!");
                this.pl.utils.log(Level.WARNING, "You may want to create a script to update them all.");
                this.pl.utils.copyFile(file, file2);
            }
        }
        else if (!this.pl.usingWindows && file.getName().contains(".jar")) {
            try {
                Files.createSymbolicLink(file2.toPath(), file.toPath().toAbsolutePath(), (FileAttribute<?>[])new FileAttribute[0]);
            }
            catch (FileAlreadyExistsException ex5) {
                this.pl.utils.debug("Linked template .jar file already existed when trying to link to player's server.");
            }
            catch (IOException ex2) {
                this.pl.utils.log(Level.SEVERE, "Failed to link template file (" + file.getPath() + "). Please send this stack trace to the developer.");
                ex2.printStackTrace();
            }
            catch (UnsupportedOperationException ex6) {
                this.pl.utils.log(Level.WARNING, "Failed to create symbolic link for from template folder, creating a copy instead.");
                this.pl.utils.log(Level.WARNING, "Symbolic links may not be available on Windows, or certain file systems.");
                this.pl.utils.log(Level.WARNING, "Be sure to update ALL player server files when updating PlayerServers!");
                this.pl.utils.log(Level.WARNING, "You may want to create a script to update them all.");
                this.pl.utils.copyFile(file, file2);
            }
        }
        else {
            this.pl.utils.copyFile(file, file2);
        }
    }
    
    public String getJar(String s) {
        File serverFolder = this.getServerFolder(s);
        if (serverFolder.isDirectory()) {
            String jarName = null;
            for (String fileName : serverFolder.list()) {
                if (fileName.matches("(?i)(cauldron|kcauldron|forge)(.+)?(\\.jar)")) {
                    return fileName;
                }
                if (fileName.matches("(?i)(spigot|folia|server|paper|paperspigot|craftbukkit|minecraft-server|minecraft_server)(.+)?(\\.jar)")) {
                	jarName = fileName;
                }
            }
            return jarName;
        }
        return null;
    }
    
    public File getServerFolder(String s) {
        return new File(this.pl.serversFolder, s);
    }
    
    public String getServerInfo(String s, String s2) {
        if (this.serverMap.containsKey(s)) {
            return this.serverMap.get(s).getSetting(s2);
        }
        return null;
    }
    
    public void setServerInfo(String s, String s2, String s3) {
        String s4 = null;
        if (!this.serverMap.containsKey(s)) {
        	this.serverMap.put(s, new PlayerServer(UUID.fromString(s), this.pl));
            this.serverMap.get(s).setSetting(s2, s3);
        }
        else {
            this.serverMap.get(s).setSetting(s2, s3);
            s4 = this.serverMap.get(s).getSetting(s2);
        }
        this.serverMap.get(s).save();
        if (s4 == null || !s4.equals(s3)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("server-name");
            list.add("server-ip");
            list.add("max-players");
            list.add("motd");
            list.add("white-list");
            if (list.contains(s2.toLowerCase())) {
                this.pl.utils.debug("Updating server.properties " + s2 + " setting to: " + s3);
                this.pl.settingsManager.changeSetting(s, s2, s3);
            }
            ServerModifyEvent serverModifyEvent = new ServerModifyEvent(this.pl, s);
            this.pl.eventManager.fire(serverModifyEvent);
        }
    }
    
    public boolean hasServer(String s) {
        return this.serverMap.containsKey(s);
    }
    
    public boolean serverFilesExist(String s) {
        return this.getServerFolder(s).exists();
    }
    
    public boolean serverExists(String s) {
    	if (this.proxy.getServer(s) == null || this.proxy.getServer(s).isEmpty()) {
    		return false;
    	}
        return this.proxy.getAllServers().contains(this.proxy.getServer(s).get());
    }
    
    public boolean isPlayerServer(String s) {
        for (Map.Entry<String, PlayerServer> entry : this.pl.serverManager.serverMap.entrySet()) {
            if (entry.getValue().getSetting("server-name") != null && entry.getValue().getSetting("server-name").equals(s)) {
                return true;
            }
        }
        return false;
    }
    
    public String getOwnerId(String s) {
    	for (Map.Entry<String, PlayerServer> entry : this.serverMap.entrySet()) {
    		HashMap<String, String> hashMap = entry.getValue().getAllSettings();
    		if (hashMap != null && hashMap.get("server-name") != null && hashMap.get("server-name").equals(s)) {
                return entry.getKey();
            }
    		
    	}
    	return null;
    }
    
    public String getServerTemplateName(String s) {
        String ownerId = this.getOwnerId(s);
        if (ownerId != null && this.serverMap.containsKey(ownerId)) {
            File file = new File(this.pl.serversFolder + File.separator + ownerId + File.separator + "PlayerServers.yml");
            if (!file.exists()) {
                return null;
            }
            Map<String, Object> templateFile = (Map<String, Object>) this.pl.configManager.loadFile(file);
			String string = templateFile.get("template-name").toString();
			if (string.isEmpty()) {
			    return null;
			}
			return string;
        }
        return null;
    }
    
    public void countRam() {
        int allocatedRam = 0;
        for (String s : this.addedServers.keySet()) {
            if (this.isPlayerServer(s)) {
                allocatedRam += PlayerServers.getApi().getServerXmx(s);
            }
        }
        if (allocatedRam != this.allocatedRam) {
            this.allocatedRam = allocatedRam;
            this.pl.utils.debug("Allocated RAM changed! Total Allocated RAM: " + this.allocatedRam);
        }
    }
    
    public void verifySettings(String s) {
        if (this.pl.settingsManager.propExists(s)) {
            String setting;
            if (((setting = this.pl.settingsManager.getSetting(s, "server-ip")) != null && this.pl.serverManager.getServerInfo(s, "server-ip") == null) || (this.pl.serverManager.getServerInfo(s, "server-ip") != null && this.pl.serverManager.getServerInfo(s, "server-ip").equalsIgnoreCase("null")) || (this.pl.serverManager.getServerInfo(s, "server-ip") != null && !this.pl.serverManager.getServerInfo(s, "server-ip").equalsIgnoreCase(setting))) {
                if (setting == null || setting.equalsIgnoreCase("null")) {
                    this.pl.serverManager.setServerInfo(s, "server-ip", "127.0.0.1");
                    this.pl.utils.log(Level.WARNING, s + "'s server-ip servers.yml value did not match their server.prop server-ip, set to \"127.0.0.1\"");
                }
                else {
                    this.pl.serverManager.setServerInfo(s, "server-ip", setting);
                    this.pl.utils.log(Level.WARNING, s + "'s server-ip servers.yml value did not match their server.prop server-ip, set to \"" + setting + "\"");
                }
            }
            String setting2;
            if ((((setting2 = this.pl.settingsManager.getSetting(s, "motd")) != null && this.pl.serverManager.getServerInfo(s, "motd") == null) || (this.pl.serverManager.getServerInfo(s, "motd") != null && this.pl.serverManager.getServerInfo(s, "motd").equalsIgnoreCase("null")) || (this.pl.serverManager.getServerInfo(s, "motd") != null && !this.pl.serverManager.getServerInfo(s, "motd").equalsIgnoreCase(setting2))) && setting2 != null && !setting2.equalsIgnoreCase("null")) {
                this.pl.serverManager.setServerInfo(s, "motd", setting2);
                this.pl.utils.log(Level.WARNING, s + "'s motd servers.yml value did not match their server.prop motd, set to \"" + setting2 + "\"");
            }
            String setting3;
            if ((((setting3 = this.pl.settingsManager.getSetting(s, "white-list")) != null && this.pl.serverManager.getServerInfo(s, "white-list") == null) || (this.pl.serverManager.getServerInfo(s, "white-list") != null && this.pl.serverManager.getServerInfo(s, "white-list").equalsIgnoreCase("null")) || (this.pl.serverManager.getServerInfo(s, "white-list") != null && !this.pl.serverManager.getServerInfo(s, "white-list").equalsIgnoreCase(setting3))) && setting3 != null && !setting3.equalsIgnoreCase("null")) {
                this.pl.serverManager.setServerInfo(s, "white-list", setting3);
                this.pl.utils.log(Level.WARNING, s + "'s white-list servers.yml value did not match their server.prop motd, set to \"" + setting3 + "\"");
            }
            String setting4;
            if (((setting4 = this.pl.settingsManager.getSetting(s, "max-players")) != null && this.pl.serverManager.getServerInfo(s, "max-players") == null) || (this.pl.serverManager.getServerInfo(s, "max-players") != null && this.pl.serverManager.getServerInfo(s, "max-players").equalsIgnoreCase("null")) || (this.pl.serverManager.getServerInfo(s, "max-players") != null && !this.pl.serverManager.getServerInfo(s, "max-players").equalsIgnoreCase(setting4))) {
                if (setting4 == null || !setting4.matches("[0-9]+")) {
                    this.pl.utils.log(Level.WARNING, s + "'s max players wasn't a number! Defaulted to 2!");
                    this.pl.serverManager.setServerInfo(s, "max-players", "10");
                }
                else {
                    this.pl.serverManager.setServerInfo(s, "max-players", setting4);
                    this.pl.utils.log(Level.WARNING, s + "'s max-players servers.yml value did not match their server.prop max-players, set to \"" + setting4 + "\"");
                }
            }
            String setting5;
            if ((setting5 = this.pl.settingsManager.getSetting(s, "server-port")) == null || !setting5.equalsIgnoreCase(String.valueOf(this.pl.utils.getSrvPort(s)))) {
                if (this.pl.utils.getSrvPort(s) != 0) {
                    this.pl.settingsManager.changeSetting(s, "server-port", String.valueOf(this.pl.utils.getSrvPort(s)));
                    this.pl.utils.log(Level.WARNING, s + "'s server.properties server-port value did not match their saved port, set to \"" + String.valueOf(this.pl.utils.getSrvPort(s)) + "\"");
                }
                else {
                    this.pl.utils.log(Level.SEVERE, s + "'s server.properties server-port value did not match their server.yml saved port, but it could not be fixed automatically! Please verify this users server-port server.properties setting.");
                }
            }
            String setting6;
            if (((setting6 = this.pl.settingsManager.getSetting(s, "query.port")) == null || !setting6.equalsIgnoreCase(String.valueOf(this.pl.utils.getSrvPort(s)))) && this.pl.utils.getSrvPort(s) != 0) {
                this.pl.settingsManager.changeSetting(s, "query.port", String.valueOf(this.pl.utils.getSrvPort(s)));
                this.pl.utils.log(Level.WARNING, s + "'s server.properties query.port value did not match their saved port, set to \"" + String.valueOf(this.pl.utils.getSrvPort(s)) + "\"");
            }
            String setting7;
            if ((setting7 = this.pl.settingsManager.getSetting(s, "server-name")) == null || !setting7.equalsIgnoreCase(this.pl.utils.getSrvName(s))) {
                if (this.pl.utils.getSrvName(s) != null && !this.pl.utils.getSrvName(s).equalsIgnoreCase("null")) {
                    this.pl.settingsManager.changeSetting(s, "server-name", String.valueOf(this.pl.utils.getSrvName(s)));
                    this.pl.utils.log(Level.WARNING, s + "'s server.properties server-name value did not match their saved server name, set to \"" + String.valueOf(this.pl.utils.getSrvName(s)) + "\"");
                }
                else {
                    this.pl.utils.log(Level.SEVERE, s + "'s server.properties server-port value did not match their server.yml saved port, but it could not be fixed automatically! Please verify this users server-port server.properties setting.");
                }
            }
            String setting8;
            if ((setting8 = this.pl.settingsManager.getSetting(s, "online-mode")) == null || !setting8.equalsIgnoreCase("false")) {
                this.pl.settingsManager.changeSetting(s, "online-mode", "false");
                this.pl.utils.log(Level.WARNING, "Server's server.properties online-mode was not false, fixed!");
            }
        }
        else {
            this.pl.utils.debug("Didn't verify " + s + "'s server settings, because their server.properties doesn't exist (server deleted?)");
        }
    }
    private boolean isValidParameter(String param) {
        return param != null && param.matches("^[a-zA-Z0-9._-]+$");
    }
}
