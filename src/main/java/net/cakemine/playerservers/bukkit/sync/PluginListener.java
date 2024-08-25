package net.cakemine.playerservers.bukkit.sync;

import org.bukkit.plugin.messaging.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bukkit.entity.*;
import net.cakemine.playerservers.bukkit.*;
import org.bukkit.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;

public class PluginListener implements PluginMessageListener
{
    PlayerServers pl;
    PluginSender pSend;
    ObjectMapper mapper = new ObjectMapper();
    
    public PluginListener(PlayerServers pl) {
        this.pl = pl;
    }
    
    public void onPluginMessageReceived(String s, Player player, byte[] array) {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(array)));
        try {
            String utf = dataInputStream.readUTF();
            this.pl.utils.debug("pluginmessage subchannel = " + utf);
            String utf2 = dataInputStream.readUTF();
            String s2 = utf;
            switch (s2) {
                case "psCustomCmd": {
                    PluginSender sender = this.pl.sender;
                    ++sender.syncCount;
                    break;
                }
                case "test": {
                    this.pl.utils.log("TEST SUCCESS!");
                    break;
                }
                case "messages": {
                	HashMap<String, String> jsonMap = mapper.readValue(Base64.getDecoder().decode(utf2), new TypeReference<HashMap<String, String>>() {});
                	this.pl.messages = jsonMap;
                    this.pl.utils.debug("Loaded messages count " + this.pl.messages.size());
                    PluginSender sender2 = this.pl.sender;
                    ++sender2.syncCount;
                    break;
                }
                case "prefix": {
                    this.pl.prefix = ChatColor.translateAlternateColorCodes('&', utf2);
                    this.pl.utils.debug("prefix = " + this.pl.prefix);
                    PluginSender sender3 = this.pl.sender;
                    ++sender3.syncCount;
                    break;
                }
                case "fallback": {
                    this.pl.fallbackSrv = utf2;
                    this.pl.utils.debug("fallbackSrv = " + this.pl.fallbackSrv);
                    PluginSender sender4 = this.pl.sender;
                    ++sender4.syncCount;
                    break;
                }
                case "expiredate": {
                    this.pl.expireDate = utf2;
                    this.pl.utils.debug("expireDate = " + this.pl.expireDate);
                    PluginSender sender5 = this.pl.sender;
                    ++sender5.syncCount;
                    break;
                }
                case "daysleft": {
                    this.pl.daysLeft = utf2;
                    this.pl.utils.debug("daysLeft = " + this.pl.daysLeft);
                    PluginSender sender6 = this.pl.sender;
                    ++sender6.syncCount;
                    break;
                }
                case "timeleft": {
                    this.pl.timeLeft = utf2;
                    this.pl.utils.debug("timeLeft = " + this.pl.daysLeft);
                    PluginSender sender7 = this.pl.sender;
                    ++sender7.syncCount;
                    break;
                }
                case "useExpire": {
                    this.pl.useExpire = Boolean.valueOf(utf2);
                    this.pl.utils.debug("useExpire = " + this.pl.useExpire);
                    if (this.pl.useExpire && this.pl.isSlave()) {
                        this.pl.expiryTask();
                    }
                    PluginSender sender8 = this.pl.sender;
                    ++sender8.syncCount;
                    break;
                }
                case "expirecheck": {
                    this.pl.msLeft = Long.valueOf(utf2);
                    this.pl.utils.debug("msLeft = " + this.pl.msLeft);
                    if (this.pl.useExpire && this.pl.expireShutdown && this.pl.msLeft <= 0L) {
                        this.pl.utils.shutdown(20);
                        this.pl.utils.log("Shut down server because it expired!");
                    }
                    PluginSender sender9 = this.pl.sender;
                    ++sender9.syncCount;
                    break;
                }
                case "blockedcmds": {
                    PlayerListener.blockedCmds.clear();
                    Collections.addAll(PlayerListener.blockedCmds, utf2.split("(%%%)"));
                    this.pl.utils.debug("blockedCmds = " + PlayerListener.blockedCmds);
                    this.pl.cmdsLoaded = true;
                    PluginSender sender10 = this.pl.sender;
                    ++sender10.syncCount;
                    break;
                }
                case "alwaysops": {
                    PlayerListener.alwaysOps.clear();
                    for (String s5 : utf2.split("(%%%)")) {
                        PlayerListener.alwaysOps.add(s5);
                        Player player2;
                        if (!s5.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") && (player2 = Bukkit.getPlayer(s5)) != null) {
                            player2.setWhitelisted(true);
                            player2.setOp(true);
                            this.pl.utils.debug("OPing " + player2.getName() + " because their name is on the always-op list.");
                        }
                        else {
                            Player player3;
                            if (s5.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") && (player3 = Bukkit.getPlayer(UUID.fromString(s5))) != null) {
                                player3.setWhitelisted(true);
                                player3.setOp(true);
                                this.pl.utils.debug("OPing " + player3.getName() + " because their UUID is on the always-op list.");
                            }
                        }
                    }
                    PluginSender sender11 = this.pl.sender;
                    ++sender11.syncCount;
                    break;
                }
                case "debug": {
                    this.pl.debug = utf2.equals("1");
                    this.pl.utils.debug("DEBUG is TRUE if you can read this");
                    PluginSender sender12 = this.pl.sender;
                    ++sender12.syncCount;
                    break;
                }
                case "templates": {
                	HashMap<String, HashMap<String, String>> jsonMap = mapper.readValue(utf2, new TypeReference<HashMap<String, HashMap<String, String>>>() {});
                	this.pl.utils.debug("input = " + jsonMap.toString());
                	for (String tamplate : jsonMap.keySet()) {
                		this.pl.templates.put(tamplate, jsonMap.get(tamplate));
                    }
                    this.pl.utils.debug("Loaded Template Count = " + this.pl.templates.size());
                    PluginSender sender13 = this.pl.sender;
                    ++sender13.syncCount;
                    break;
                }
                case "controlGUI": {
                    HashMap<String, String> jsonMap = mapper.readValue(Base64.getDecoder().decode(utf2), new TypeReference<HashMap<String, String>>() {});
                    this.pl.utils.debug("input = " + jsonMap.toString());
                    String uuid = dataInputStream.readUTF();
                    this.pl.gui.getGUI("control").open(this.pl.getServer().getPlayer(UUID.fromString(uuid)), null, 0, jsonMap);
                    
                    break;
                }
                case "worldselect": {
                    this.pl.gui.getGUI("templates").open(this.pl.getServer().getPlayer(UUID.fromString(utf2)), null, 0);
                    break;
                }
                case "serverselect": {
                	HashMap<String,  HashMap<String, String>> jsonMap = mapper.readValue(Base64.getDecoder().decode(utf2), new TypeReference<HashMap<String,  HashMap<String, String>>>() {});
            		String uuid = dataInputStream.readUTF();
            		this.pl.utils.debug("input = " + jsonMap.toString());
            		this.pl.utils.debug("command sender = " + uuid);
            		this.pl.gui.getGUI("servers").openWithExtras(this.pl.getServer().getPlayer(UUID.fromString(uuid)), null, 0, jsonMap);
                    break;
                }
                case "reSync": {
                    this.pl.listener.syncDone = false;
                    this.pl.sender.syncCount = 0;
                    this.pl.sender.syncTotal = 0;
                    break;
                }
                case "finishSync": {
                    this.pl.sender.syncTotal = Integer.valueOf(utf2);
                    this.pl.sender.confirmSync();
                    break;
                }
                case "version": {
                    this.pl.sender.versionMatcher(utf2);
                    PluginSender sender14 = this.pl.sender;
                    ++sender14.syncCount;
                    break;
                }
                case "guis": {
                    this.pl.gui.deserializeGUIs(utf2);
                    PluginSender sender15 = this.pl.sender;
                    ++sender15.syncCount;
                    break;
                }
            }
            dataInputStream.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        	
    }
}