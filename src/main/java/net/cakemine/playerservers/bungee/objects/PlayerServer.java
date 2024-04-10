package net.cakemine.playerservers.bungee.objects;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import net.cakemine.playerservers.bungee.PlayerServers;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerServer {
	PlayerServers pl = PlayerServers.getApi().getInstance();
	HashMap<String, String> settings;
	HashMap<String, String> customSettings;
	public PlayerServer(String serverUUID) {
		settings = new HashMap<>();
		customSettings = new HashMap<>();
		loadServer(serverUUID);
		
	}
	
	private void loadServer(String serverUUID) {
        File file = new File(this.pl.getDataFolder() + File.separator + "servers");
        this.pl.utils.debug("serverDir = " + file.toString());
        if (!file.exists()) {
            file.mkdir();
        }
        if (this.pl.serverStore.get("servers") != null ) {
        	settings.put("server-name", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("server-name"));
        	settings.put("player-name", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("player-name"));
        	settings.put("expire-date", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("expire-date"));
        	settings.put("motd", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("motd"));
        	settings.put("memory", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("memory"));
        	settings.put("port", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("port"));
        	settings.put("white-list", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("white-list"));
        	settings.put("max-players", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("max-players"));
        	settings.put("server-ip", this.pl.serverStore.getSection("servers").getSection(serverUUID).getString("server-ip"));
        	if (this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("custom") != null) {
        		this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("custom").getKeys().forEach(custom -> {
        			customSettings.put(custom, this.pl.serverStore.getSection("servers").getSection(serverUUID).getSection("custom").getString(custom));
        		});
        	}
        }
        
        this.pl.utils.debug("player server added.");
        this.pl.utils.debug("Loaded " + customSettings.size() + " custom settings.");
    }
	
	
	public HashMap<String, String> fromHashMap() {
		return settings;
	}
	
	public void setCustomSetting(String setting, String value) {
		customSettings.put(setting, value);
	}
	public HashMap<String, String> getAllCustomSettings() {
		return customSettings;
	}
}
