package net.cakemine.playerservers.bungee.objects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

import net.cakemine.playerservers.bungee.PlayerServers;
import net.md_5.bungee.config.Configuration;

public class StoredPlayer {
	PlayerServers pl;
	public Configuration playerStore;

	final UUID uuid;
	File file;
	HashMap<String, String> customSettings;
	HashMap<String, String> permissions;
	HashMap<String, String> playerInfo;
	HashMap<String, HashMap<String, String>> map;
	
	public StoredPlayer(UUID playerUUID, PlayerServers pl) {
		this.pl = pl;
		customSettings = new HashMap<>();
		permissions = new HashMap<>();
		playerInfo = new HashMap<>();
		map = new HashMap<>();
		uuid = playerUUID;
		file = new File(this.pl.getDataFolder() + File.separator + "data" + File.separator + "players" + File.separator + uuid.toString() + ".yml");
		try {
			this.pl.utils.debug("playerDir = " + file.toString());
	        if (!file.exists()) {
	        	Files.createFile(file.toPath());
	        }
			playerStore = this.pl.cfg.load(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadPlayer();
	}
	
	
	public void loadPlayer() {
		String playerUUID = uuid.toString();
        if (playerStore.get(playerUUID) != null) {
        	if (playerStore.getSection(playerUUID).getSection("custom") != null) {
        		playerStore.getSection(playerUUID).getSection("custom").getKeys().forEach(custom -> {
        			customSettings.put(custom, playerStore.getSection(playerUUID).getSection("custom").getString(custom));
        		});
        	}
        	if (playerStore.getSection(playerUUID).getSection("permissions") != null) {
        		playerStore.getSection(playerUUID).getSection("permissions").getKeys().forEach(perms -> {
        			permissions.put(perms, playerStore.getSection(playerUUID).getSection("permissions").getString(perms));
        		});
        	}
        	if (playerStore.getSection(playerUUID).getSection("info") != null) {
        		playerStore.getSection(playerUUID).getSection("info").getKeys().forEach(info -> {
        			playerInfo.put(info, playerStore.getSection(playerUUID).getSection("info").getString(info));
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
		// Ehh.. should be cached..
		return this.pl.proxy.getPlayer(uuid).getName();
	}
	
	public HashMap<String, String> getPermissions(){
		return permissions;
	}
	
	public HashMap<String, String> getPlayerInfo(){
		return playerInfo;
	}
	
	public void save() {
		playerStore.set(uuid.toString(), map);
		try {
			this.pl.cfg.save(playerStore, file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
