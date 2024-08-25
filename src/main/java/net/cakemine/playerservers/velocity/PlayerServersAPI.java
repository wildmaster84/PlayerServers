package net.cakemine.playerservers.velocity;

import net.cakemine.playerservers.velocity.ServerManager;
import net.cakemine.playerservers.velocity.events.*;
import net.cakemine.playerservers.velocity.objects.PlayerServer;
import net.cakemine.playerservers.velocity.objects.StoredPlayer;
import net.cakemine.playerservers.velocity.wrapper.Controller;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;

import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * This class provides an API for interacting with the PlayerServers plugin.
 */
public class PlayerServersAPI {
    PlayerServers pl;
    
    PlayerServersAPI(PlayerServers pl) {
        this.pl = pl;
    }
    
    public boolean getDebugMode() {
        return this.pl.debug;
    }
    
    public void debugLog(String message) {
        this.pl.utils.debug(message);
    }
    
    public void debugLog(Object plugin, String s) {
        this.pl.utils.debug(s);
    }
    
    public void log(String s) {
        this.pl.utils.log(s);
    }
    
    public void log(Level level, String s) {
        this.pl.utils.log(level, s);
    }
    
    public String getPluginPrefix() {
        return this.pl.prefix;
    }
    
    public void setPluginPrefix(String prefix) {
        this.pl.prefix = prefix;
    }
    
    public void reSync(RegisteredServer server) {
        this.pl.sender.reSync(server);
    }
    
    public void reSyncAll() {
        this.pl.sender.reSyncAll();
    }
    
    public HashMap<String, PlayerServer> getServerMap() {
        return this.pl.serverManager.serverMap;
    }
    
    public String getServerMapSetting(String serverName, String setting) {
        return this.pl.serverManager.serverMap.get(serverName).getSetting(setting);
    }
    
    public ServerManager getServerManager() {
    	return this.pl.serverManager;
    }
    
    public void setServerMapSetting(String serverName, String setting, String value) {
        this.pl.serverManager.setServerInfo(serverName, setting, value);
    }
    
    public void clearServerMapSetting(String serverName, String setting) {
        this.pl.serverManager.serverMap.get(serverName).getAllSettings().remove(setting);
        this.pl.eventManager.fire(new ServerModifyEvent(this.pl, setting));
    }
    
    public List<String> getOnlinePlayerServers() {
        return this.pl.serverManager.playerServers;
    }
    
    public boolean getServerOnline(String serverName) {
        String serverUUID = this.pl.utils.getServerUUID(serverName);
        return !this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(serverUUID), this.pl.utils.getSrvPort(serverUUID));
    }
    
    public boolean serverFilesExist(String serverName) {
        return this.pl.serverManager.serverFilesExist(this.pl.serverManager.getOwnerId(serverName));
    }
    
    public boolean serverFilesExistUUID(UUID serverUuid) {
        return this.pl.serverManager.serverFilesExist(serverUuid.toString());
    }
    
    public UUID getServerOwnerId(String serverName) {
        return UUID.fromString(this.pl.utils.getServerUUID(serverName));
    }
    
    public String getServerOwnerName(String serverName) {
        return this.pl.utils.getName(this.pl.serverManager.getOwnerId(serverName));
    }
    
    public String getServerTemplateName(String serverName) {
        return this.pl.serverManager.getServerTemplateName(serverName);
    }
    
    public int getServerXmx(String serverName) {
        if (this.pl.serverManager.getOwnerId(serverName) == null) {
            return 0;
        }
        return this.pl.utils.memStringToInt(this.pl.serverManager.serverMap.get(this.pl.serverManager.getOwnerId(serverName)).getSetting("memory").toString().split("\\/")[1]);
    }
    
    public int getServerXms(String serverName) {
        if (this.pl.serverManager.getOwnerId(serverName) == null) {
            return 0;
        }
        return this.pl.utils.memStringToInt(this.pl.serverManager.serverMap.get(this.pl.serverManager.getOwnerId(serverName)).getSetting("memory").toString().split("\\/")[0]);
    }
    
    public String getPlayerServerName(UUID uuid) {
        return this.pl.utils.getSrvName(uuid.toString());
    }
    
    public void setPlayerServerName(UUID uuid, String name) {
        this.pl.serverManager.setServerInfo(uuid.toString(), "server-name", name);
    }
    
    public int getPlayerServerPort(UUID uuid) {
        return this.pl.utils.getSrvPort(uuid.toString());
    }
    
    public String getPropertiesSetting(UUID uuid, String setting) {
        return this.pl.settingsManager.getSetting(uuid.toString(), setting);
    }
    
    public void setPropertiesSetting(UUID uuid, String setting, String value) {
        this.pl.settingsManager.changeSetting(uuid.toString(), setting, value);
    }
    
    public void startServerName(String serverName) {
        this.pl.serverManager.startupSrv(this.pl.utils.getServerUUID(serverName), null);
    }
    
    public void startServerUUID(UUID uuid) {
        this.pl.serverManager.startupSrv(uuid.toString(), null);
    }
    
    public void startServerPlayer(String playerName) {
        this.pl.serverManager.startupSrv(this.pl.utils.getUUID(playerName), null);
    }
    
    public void stopServerName(String serverName) {
        this.pl.serverManager.stopSrv(this.pl.utils.getServerUUID(serverName));
    }
    
    public void stopServerUUID(UUID uuid) {
        this.pl.serverManager.stopSrv(uuid.toString());
    }
    
    public void stopServerPlayer(String playerName) {
        this.pl.serverManager.stopSrv(this.pl.utils.getUUID(playerName));
    }
    
    public void stopAllServers() {
        this.pl.serverManager.stopAll(null);
    }
    
    public void addVelocityServer(String serverName, String address, Integer port, String motd, int timer) {
        this.pl.serverManager.addVelocity(serverName, address, port, motd, timer);
    }
    
    public void removeVelocityServer(String serverName) {
        this.pl.serverManager.removeVelocity(serverName);
    }
    
    public void createServer(UUID uuid, String serverName) {
        this.pl.serverManager.createServer(null, this.pl.utils.getName(uuid.toString()), uuid.toString(), this.pl.templateManager.getTemplateFile(serverName));
    }
    
    public void deleteServer(UUID uuid) {
        this.pl.serverManager.deleteServer(null, uuid.toString());
    }
    
    public List<String> getAvailableTemplateNames() {
        ArrayList<String> list = new ArrayList<String>();
        Iterator<File> iterator = this.pl.templateManager.templates.keySet().iterator();
        while (iterator.hasNext()) {
            list.add(this.pl.templateManager.getTemplateSetting(iterator.next(), "template-name"));
        }
        return list;
    }
    
    public String getTemplateDescription(String templateName) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return null;
        }
        return this.pl.templateManager.getTemplateSetting(this.pl.templateManager.getTemplateFile(templateName), "template-name");
    }
    
    public List<String> getTemplateDescriptionList(String templateName) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return null;
        }
        return Arrays.asList(this.pl.templateManager.getTemplateSetting(this.pl.templateManager.getTemplateFile(templateName), "template-name").split("||"));
    }
    
    public boolean isTemplateCreatorOp(String templateName) {
        return this.pl.templateManager.getTemplateFile(templateName) != null && this.pl.templateManager.getTemplateSetting(this.pl.templateManager.getTemplateFile(templateName), "creator-gets-op").equalsIgnoreCase("true");
    }
    
    public boolean isTemplateExpireShutdown(String templateName) {
        return this.pl.templateManager.getTemplateFile(templateName) != null && this.pl.templateManager.getTemplateSetting(this.pl.templateManager.getTemplateFile(templateName), "shutdown-on-expire").equalsIgnoreCase("true");
    }
    
    public String getTemplateExpiryHuman(String templateName) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return null;
        }
        return this.pl.templateManager.getTemplateSetting(this.pl.templateManager.getTemplateFile(templateName), "default-expiry-time");
    }
    
    public boolean setTemplateName(String templateName, String newName) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return false;
        }
        this.pl.templateManager.templates.get(this.pl.templateManager.getTemplateFile(templateName)).put("template-name", newName);
        return true;
    }
    
    public boolean setTemplateDescrption(String templateName, String newDescription) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return false;
        }
        this.pl.templateManager.templates.get(this.pl.templateManager.getTemplateFile(templateName)).put("description", newDescription);
        return true;
    }
    
    public boolean setTemplateMaterial(String templateName, String newMaterial) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return false;
        }
        this.pl.templateManager.templates.get(this.pl.templateManager.getTemplateFile(templateName)).put("icon-material", newMaterial);
        return true;
    }
    
    public boolean setTemplateCreatorOP(String templateName, boolean getsOp) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return false;
        }
        this.pl.templateManager.templates.get(this.pl.templateManager.getTemplateFile(templateName)).put("creator-gets-op", getsOp);
        return true;
    }
    
    public boolean setTemplateExpireShutdown(String templateName, boolean expireShutdown) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return false;
        }
        this.pl.templateManager.templates.get(this.pl.templateManager.getTemplateFile(templateName)).put("shutdown-on-expire", expireShutdown);
        return true;
    }
    
    public boolean setTemplateExpiryTime(String templateName, String expireTime) {
        if (this.pl.templateManager.getTemplateFile(templateName) == null) {
            return false;
        }
        this.pl.templateManager.templates.get(this.pl.templateManager.getTemplateFile(templateName)).put("default-expire-time", expireTime);
        return true;
    }
    
    public void addTime(UUID uuid, int n, String s) {
        this.pl.expiryTracker.addTime(uuid.toString(), n, s);
    }
    
    public void removeTime(UUID uuid, int n, String s) {
        this.pl.expiryTracker.removeTime(uuid.toString(), n, s);
    }
    
    public long getMillisLeft(UUID uuid) {
        return this.pl.expiryTracker.msLeft(uuid.toString());
    }
    
    public String getTimeLeft(UUID uuid) {
        return this.pl.expiryTracker.timeLeft(uuid.toString());
    }
    
    public String getExpireDate(UUID uuid) {
        return this.pl.expiryTracker.getDate(uuid.toString());
    }
    
    public String getPlayerName(UUID uuid) {
        return this.pl.utils.getName(uuid.toString());
    }
    
    public UUID getPlayerUUID(String playerName) {
        if (this.pl.utils.getUUID(playerName) == null) {
            return null;
        }
        return UUID.fromString(this.pl.utils.getUUID(playerName));
    }
    
    public void putPlayerMapEntry(String playerName, UUID uuid) {
        this.pl.loadPlayer(uuid, new StoredPlayer(uuid, this.pl));
    }
    
    public boolean removePlayerMapEntry(String playerName) {
        if (this.pl.playerMap.containsKey(playerName)) {
            this.pl.playerMap.remove(playerName);
            return true;
        }
        return false;
    }
    
    public File[] listFiles(File file) {
        if (file.isDirectory()) {
            return file.listFiles();
        }
        return null;
    }
    
    public void copyFileSoft(File file, File file2) {
        this.pl.serverManager.doCopy(file, file2);
    }
    
    public void copyFileHard(File file, File file2) {
        this.pl.utils.copyFile(file, file2);
    }
    
    public void deleteFile(File file) {
        this.pl.serverManager.doDelete(file);
    }
    
    public File getDataFolder() {
    	return this.pl.getDataFolder();
    }
    
    @Deprecated
    public Controller getWrapperController() {
    	return this.pl.ctrl;
    }
    
    public int[] getPluginVersion() {    	
    	String[] version = this.pl.getDescription().getVersion().get().split("-")[0].split("\\.");

        int[] intArray = new int[version.length];
        for (int i = 0; i < version.length; i++) {
            intArray[i] = Integer.parseInt(version[i]);
        }
        return intArray;
    }
    
 // https://github.com/nuckle/minecraft-offline-uuid-generator/blob/main/src/js/uuid.js
 	public static String getOfflineUUID(String name) {
         if (name == null || name.isEmpty()) {
             return null;
         }
         String input = "OfflinePlayer:" + name;

         try {
             MessageDigest md = MessageDigest.getInstance("MD5");
             byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
             hash[6] = (byte) ((hash[6] & 0x0f) | 0x30); // Set version to 3
             hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); // Set variant to RFC 4122
             StringBuilder uuidBuilder = new StringBuilder();
             for (byte b : hash) {
                 uuidBuilder.append(String.format("%02x", b));
             }
             String uuid = uuidBuilder.toString();
             return uuid.substring(0, 8) + "-" +uuid.substring(8, 12) + "-" +uuid.substring(12, 16) + "-" +uuid.substring(16, 20) + "-" +uuid.substring(20);
         } catch (NoSuchAlgorithmException e) {
             return null;
         }
     }
}
