package net.cakemine.playerservers.bungee.objects;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import net.cakemine.playerservers.bungee.PlayerServers;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerServer {
	PlayerServers pl = PlayerServers.getApi().getInstance();
	HashMap<String, String> customSettings;
	HashMap<String, String> settings;
	HashMap<String, HashMap<String, String>> map;
	UUID uuid;
	public PlayerServer(UUID serverUUID) {
		customSettings = new HashMap<>();
		settings = new HashMap<>();
		map = new HashMap<>();
		uuid = serverUUID;
		loadServer();
		
	}
	
	private void loadServer() {
		String serverUUID = uuid.toString();
        File file = new File(this.pl.getDataFolder() + File.separator + "servers");
        this.pl.utils.debug("serverDir = " + file.toString());
        if (!file.exists()) {
            file.mkdir();
        }
        if (this.pl.serverStore.get("servers") != null && this.pl.serverStore.getSection("servers").getSection(serverUUID) != null) {
        	if (this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("custom") != null) {
        		this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("custom").getKeys().forEach(custom -> {
        			customSettings.put(custom, this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("custom").getString(custom));
        		});
        	}
        	if (this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("settings") != null) {
        		this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("settings").getKeys().forEach(setting -> {
        			settings.put(setting, this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("settings").getString(setting));
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
}
