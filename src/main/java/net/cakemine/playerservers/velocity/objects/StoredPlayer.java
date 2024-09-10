package net.cakemine.playerservers.velocity.objects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

import net.cakemine.playerservers.velocity.PlayerServers;

public class StoredPlayer {
	PlayerServers pl;
	HashMap<String, HashMap<String, HashMap<String, String>>> playerStore;

	final UUID uuid;
	File file;
	HashMap<String, String> customSettings;
	HashMap<String, String> permissions;
	HashMap<String, String> playerInfo;
	HashMap<String, HashMap<String, String>> map;
	
	public StoredPlayer(UUID playerUUID, PlayerServers pl) {
		this.pl = pl;
		playerStore = new HashMap<>();
		customSettings = new HashMap<>();
		permissions = new HashMap<>();
		playerInfo = new HashMap<>();
		map = new HashMap<>();
		uuid = playerUUID;
		file = new File(this.pl.getDataFolder() + File.separator + "data" + File.separator + "players" + File.separator + uuid.toString() + ".yml");
		try {
			if (!file.exists()) {
				Files.createFile(file.toPath());
				Files.write(file.toPath(), String.format("%s: {}", uuid.toString()).getBytes());
	        }
			InputStream inputStream2 = new FileInputStream(this.pl.getDataFolder().getPath() + File.separator + "data" + File.separator + "players" + File.separator + uuid.toString() + ".yml");
			this.pl.utils.debug("playerDir = " + file.toPath());
			playerStore = this.pl.yaml.load(new InputStreamReader(inputStream2, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void loadPlayer() {
		String playerUUID = uuid.toString();
        if (playerStore.get(playerUUID) != null) {
        	if (playerStore.get(playerUUID).get("custom") != null) {
        		playerStore.get(playerUUID).get("custom").keySet().forEach(custom -> {
        			customSettings.put(custom, playerStore.get(playerUUID).get("custom").get(custom));
        		});
        	}
        	if (playerStore.get(playerUUID).get("permissions") != null) {
        		playerStore.get(playerUUID).get("permissions").keySet().forEach(perms -> {
        			permissions.put(perms, playerStore.get(playerUUID).get("permissions").get(perms));
        		});
        	}
        	if (playerStore.get(playerUUID).get("info") != null) {
        		playerStore.get(playerUUID).get("info").keySet().forEach(info -> {
        			playerInfo.put(info, playerStore.get(playerUUID).get("info").get(info));
        		});
        	}
        	
        	map.put("info", playerInfo);
        	map.put("permissions", permissions);
        	map.put("custom", customSettings);

        }
	}
	
	public void setServerMap(HashMap<String, HashMap<String, String>> serverMap) {
		map.clear();
		map = serverMap;
	}
	
	public void setCustomSetting(String setting, String value) {
		customSettings.put(setting, value);
		map.put("custom", customSettings);
	}
	
	public void setPermission(String setting, String value) {
		permissions.put(setting, value);
		map.put("permissions", permissions);
	}
	
	public void setPlayerinfo(String setting, String value) {
		playerInfo.put(setting, value);
		map.put("info", playerInfo);
	}
	
	public boolean hasPermission(String perms) {
		return (permissions.get(perms) == "true") ? true : false;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public String getUsername() {
		return (playerInfo.get("username") == null ? null : playerInfo.get("username"));
	}
	
	public HashMap<String, String> getPermissions(){
		return permissions;
	}
	
	public HashMap<String, String> getPlayerInfo(){
		return playerInfo;
	}
	
	public void save() {
		playerStore.put(uuid.toString(), map);
		this.pl.saveConfig(playerStore, "data" + File.separator + "players" + File.separator + uuid.toString() + ".yml");
	}
}
