package net.cakemine.playerservers.velocity.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;

import net.cakemine.playerservers.utils.ServerLogReader;
import net.cakemine.playerservers.velocity.PlayerServers;

public class PlayerServer {
	PlayerServers pl;
    Status status;
	
	public enum Status {
        INSTALLING,
        RUNNING,
        STOPPED,
        STARTING,
        STOPPING
    }
	HashMap<String, HashMap<String, HashMap<String, String>>> serverStore;
	HashMap<String, String> customSettings;
	HashMap<String, String> settings;
	HashMap<String, HashMap<String, String>> map;
	File file;
	final UUID uuid;
	public PlayerServer(UUID serverUUID, PlayerServers pl) {
		this.pl = pl;
		customSettings = new HashMap<>();
		settings = new HashMap<>();
		map = new HashMap<>();
		uuid = serverUUID;
		serverStore = new HashMap<>();
		file = new File(this.pl.getDataFolder() + File.separator + "data" + File.separator + "servers" + File.separator + uuid.toString() + ".yml");
		try {
			if (!file.exists()) {
				Files.createFile(file.toPath());
				Files.write(file.toPath(), String.format("%s: {}", uuid.toString()).getBytes());
	        }
			InputStream inputStream2 = new FileInputStream(this.pl.getDataFolder().getPath() + File.separator + "data" + File.separator + "servers" + File.separator + uuid.toString() + ".yml");
			this.pl.utils.debug("serverDir = " + file.toPath());
			serverStore = this.pl.yaml.load(new InputStreamReader(inputStream2, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadServer();
		
	}
	
	private void loadServer() {
		String serverUUID = uuid.toString();
        if (serverStore.get(serverUUID) != null) {
        	if (serverStore.get(serverUUID).get("custom") != null) {
        		serverStore.get(serverUUID).get("custom").keySet().forEach(custom -> {
        			customSettings.put(custom, serverStore.get(serverUUID).get("custom").get(custom));
        		});
        	}
        	if (serverStore.get(serverUUID).get("settings") != null) {
        		serverStore.get(serverUUID).get("settings").keySet().forEach(perms -> {
        			settings.put(perms, serverStore.get(serverUUID).get("settings").get(perms));
        		});
        	}
        	map.put("settings", settings);
        	map.put("custom", customSettings);
        	
        }
        
        this.pl.utils.debug("player server added.");
        this.pl.utils.debug("Loaded " + customSettings.size() + " custom settings.");
    }
	
	
	public HashMap<String, String> getAllSettings() {
		return settings;
	}
	public HashMap<String, String> getAllCustomSettings() {
		return customSettings;
	}
	
	public HashMap<String, HashMap<String, String>> toHashMap() {
		return map;
	}
	
	public void setServerMap(HashMap<String, HashMap<String, String>> serverMap) {
		map.clear();
		map = serverMap;
	}
	
	public void sendCommand(String command) {
        pl.getApi().getWrapperController().send("+command " + getName() + " " + command);
    }
	
	
	public void setCustomSetting(String setting, String value) {
		customSettings.put(setting, value);
		map.put("custom", customSettings);
	}
	
	public String getCustomSetting(String setting) {
		return customSettings.get(setting);
	}
	
	public void setSetting(String setting, String value) {
		settings.put(setting, value);
		map.put("settings", settings);
	}
	
	public String getSetting(String setting) {
		return settings.get(setting);
	}
	
	public int getPort() {
		return Integer.parseInt(settings.get("port"));
	}
	
	public int getMaxPlayers() {
		return Integer.parseInt(settings.get("max-players"));
	}
	
	public String getName() {
		return settings.get("server-name");
	}
	
	public String getUUID() {
		return uuid.toString();
	}
	
	public String getMotd() {
		return settings.get("server-motd");
	}
	
	public String getRam() {
		return settings.get("memory");
	}
	
	public String getServerTemplateName() {
		return settings.get("template-name");
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status stats) {
		status = stats;
	}
	
	public Player getPlayer(UUID playerUuid) {
		for (Player player : pl.proxy.getServer(getName()).get().getPlayersConnected()) {
			if (player.getUniqueId() == playerUuid) return player;
		}
		return null;
	}
	
	public BufferedReader getServerLog() {
	    File serversDir = new File(pl.getDataFolder(), "servers");
	    File serverDir = new File(serversDir, uuid.toString());
	    File logsDir = new File(serverDir, "logs");
	    File log = new File(logsDir, "latest.log");

	    if (!serversDir.exists() || !logsDir.exists() || !log.exists()) {
	        return null; // Only return null if the file doesn't exist
	    }

	    ServerLogReader serverLogReader = new ServerLogReader(log);
	    return serverLogReader.getLogReader();
	}
	
	public void save() {
		serverStore.put(uuid.toString(), map);
		this.pl.saveConfig(serverStore, "data" + File.separator + "servers" + File.separator + uuid.toString() + ".yml");
	}
}
