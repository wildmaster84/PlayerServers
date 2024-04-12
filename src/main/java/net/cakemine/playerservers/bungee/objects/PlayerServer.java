package net.cakemine.playerservers.bungee.objects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import net.cakemine.playerservers.bungee.PlayerServers;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class PlayerServer {
	PlayerServers pl = PlayerServers.getApi().getInstance();
	public Configuration serverStore;
	
	HashMap<String, String> customSettings;
	HashMap<String, String> settings;
	HashMap<String, HashMap<String, String>> map;
	UUID uuid;
	File file;
	public PlayerServer(UUID serverUUID) {
		customSettings = new HashMap<>();
		settings = new HashMap<>();
		map = new HashMap<>();
		uuid = serverUUID;
		file = new File(this.pl.getDataFolder() + File.separator + "data" + File.separator + "servers" + File.separator + uuid.toString() + ".yml");
		try {
			this.pl.utils.debug("serverDir = " + file.toString());
	        if (!file.exists()) {
	        	Files.createFile(file.toPath());
	        }
			serverStore = this.pl.cfg.load(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadServer();
		
	}
	
	private void loadServer() {
		String serverUUID = uuid.toString();
        if (serverStore.get(serverUUID) != null) {
        	if (serverStore.getSection(serverUUID).getSection("custom") != null) {
        		serverStore.getSection(serverUUID).getSection("custom").getKeys().forEach(custom -> {
        			customSettings.put(custom, serverStore.getSection(serverUUID).getSection("custom").getString(custom));
        		});
        	}
        	if (serverStore.getSection(serverUUID).getSection("settings") != null) {
        		serverStore.getSection(serverUUID).getSection("settings").getKeys().forEach(setting -> {
        			settings.put(setting, serverStore.getSection(serverUUID).getSection("settings").getString(setting));
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
	
	public String getServerTemplateName() {
		return settings.get("template-name");
	}
	
	public ProxiedPlayer getPlayer(UUID playerUuid) {
		for (ProxiedPlayer player : pl.proxy.getServerInfo(getName()).getPlayers()) {
			if (player.getUniqueId() == playerUuid) return player;
		}
		return null;
	}
	
	public void save() {
		serverStore.set(uuid.toString(), map);
		try {
			this.pl.cfg.save(serverStore, file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
