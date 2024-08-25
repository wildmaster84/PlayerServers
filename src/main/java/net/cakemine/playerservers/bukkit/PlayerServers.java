package net.cakemine.playerservers.bukkit;

import org.bukkit.plugin.java.*;
import net.cakemine.playerservers.bukkit.gui.*;
import org.bukkit.configuration.file.*;
import java.util.*;
import net.cakemine.playerservers.bukkit.sync.*;
import org.bukkit.plugin.messaging.*;
import net.cakemine.playerservers.bukkit.commands.*;
import org.bukkit.configuration.*;
import java.util.logging.*;
import java.io.*;
import org.bukkit.*;

public class PlayerServers extends JavaPlugin {
    PlayerServers pl;
    protected FileConfiguration psrvCfg;
    protected File psrv;
    private static PlayerServersAPI api;
    public Utils utils;
    public GUIManager gui;
    public PlayerListener listener;
    public PluginSender sender;
    public SettingsManager settingsManager;
    public HashMap<String, String> messages;
    public String fallbackSrv;
    public boolean debug;
    public String prefix;
    public String ownerJoinMsg;
    public List<String> ownerJoinCmds;
    public boolean useExpire;
    public boolean usingWindows;
    public boolean expireShutdown;
    public String expireDate;
    public String daysLeft;
    public String timeLeft;
    public long msLeft;
    public boolean cmdsLoaded;
    public boolean runningExpiryTask;
    public HashMap<String, HashMap<String, String>> servers;
    public boolean isSlave;
    public boolean isVelocity;
    public TreeMap<String, HashMap<String, String>> templates;
    public String psCommand;
    public PlayerServer psCmd;
    
    public PlayerServers() {
    	
        this.psrvCfg = (FileConfiguration)new YamlConfiguration();
        this.psrv = null;
        this.utils = new Utils(this);
        this.gui = new GUIManager(this);
        this.listener = new PlayerListener(this);
        this.sender = new PluginSender(this);
        this.settingsManager = new SettingsManager(this);
        this.messages = new HashMap<String, String>();
        this.debug = false;
        this.prefix = ChatColor.translateAlternateColorCodes('&', "&b&oPlayrSrv &7&o�&f ");
        this.ownerJoinMsg = "&eWelcome the owner of the server!||&6&o%player%&e has joined!";
        this.ownerJoinCmds = new ArrayList<String>();
        this.useExpire = false;
        this.usingWindows = false;
        this.expireShutdown = false;
        this.expireDate = null;
        this.daysLeft = null;
        this.timeLeft = null;
        this.msLeft = 1L;
        this.cmdsLoaded = false;
        this.runningExpiryTask = false;
        this.servers = new HashMap<String, HashMap<String, String>>();
        this.isSlave = false;
        this.templates = new TreeMap<String, HashMap<String, String>>();
        this.psCommand = "playerserver";
        this.psCmd = new PlayerServer(this);
    }
    
    public void onEnable() {
        this.usingWindows = this.usingWindows();
        this.pl = this;
        this.isVelocity = false;
        PlayerServers.api = new PlayerServersAPI(this);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "playerservers:core", new PluginListener(this));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "playerservers:core");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bungeecord:proxy");
        this.checkSlave();
        if (this.isSlave()) {
            this.pl.utils.log("Server is a PlayerServer!");
            this.loadPsrv();
            this.checkUseless();
            this.getCommand("myserver").setExecutor(new MyServer(this));
            this.defaultMessages();
            this.checkSettings();
        }
        else {
            this.pl.utils.log("Server is not a PlayerServer!");
            this.getCommand("playerserveradmin").setExecutor(new PlayerServerAdmin(this));
            this.getCommand("playerserver").setExecutor(new PlayerServer(this));
            this.getCommand("pssync").setExecutor(new Sync(this));
        }
        this.gui.registerGUIs();
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.sender.doSync();
    }
    
    public boolean usingWindows() {
        return System.getProperty("os.name").matches("(?i)(.*)(windows)(.*)");
    }
    
    public void checkSlave() {
        this.isSlave = (new File("PlayerServers.yml").exists() || this.utils.getOwnerId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"));
    }
    
    public boolean isSlave() {
        return this.isSlave;
    }
    
    public void loadPsrv() {
        this.psrv = new File("PlayerServers.yml");
        if (!this.psrv.exists()) {
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(this.getResource("PlayerServers.yml"), "UTF8");
                this.psrvCfg.setDefaults((Configuration)YamlConfiguration.loadConfiguration(inputStreamReader));
                this.saveConfig(this.psrvCfg, this.psrv);
                inputStreamReader.close();
            }
            catch (UnsupportedEncodingException ex) {
                this.pl.utils.log(Level.SEVERE, "&cFailed to load PlayerServers.yml config! Please send the following stack trace to the developer.");
                ex.printStackTrace();
            }
            catch (IOException ex2) {
                ex2.printStackTrace();
            }
        }
        this.psrvCfg = (FileConfiguration)YamlConfiguration.loadConfiguration(this.psrv);
        this.expireShutdown = this.psrvCfg.getBoolean("shutdown-on-expire");
        if (this.psrvCfg.getString("owner-join-message") == null) {
            this.psrvCfg.set("owner-join-message", (Object)"&eWelcome the owner of the server!||&6&o%player%&e has joined!");
            this.saveConfig(this.psrvCfg, this.psrv);
            this.ownerJoinMsg = "&eWelcome the owner of the server!||&6&o%player%&e has joined!";
        }
        else {
            this.ownerJoinMsg = this.psrvCfg.getString("owner-join-message");
        }
        if (this.psrvCfg.getStringList("owner-join-commands") == null) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("#enter custom commands to run on join here.");
            list.add("#commands starting with # won't be ran.");
            list.add("#placeholders: %owner-name%, %owner-uuid%, %template-name%, %server-name%, %server-port%");
            this.psrvCfg.set("owner-join-commands", (Object)list);
            this.saveConfig(this.psrvCfg, this.psrv);
        }
        else {
            this.ownerJoinCmds = (List<String>)this.psrvCfg.getStringList("owner-join-commands");
        }
    }
    
    public boolean getOPCheck() {
        boolean boolean1 = this.psrvCfg.getBoolean("creator-gets-op");
        this.pl.utils.debug("Server creator gets OP: " + boolean1);
        return boolean1;
    }
    
    public void checkSettings() {
        File file = new File("bukkit.yml");
        boolean changed = false;
        if (file.exists()) {
            YamlConfiguration loadConfiguration = YamlConfiguration.loadConfiguration(file);
            if (((FileConfiguration)loadConfiguration).getInt("settings.connection-throttle") > 0) {
                ((FileConfiguration)loadConfiguration).set("settings.connection-throttle", -1);
                try {
                    loadConfiguration.save(file);
                    this.pl.utils.log(Level.WARNING, "Bukkit.yml connection-throttle must be set to -1 when using BungeeCord, fixed it automatically.");
                    changed = true;
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        File paperConfig = new File("config/paper-global.yml");
        if (paperConfig.exists()) {
            YamlConfiguration loadConfiguration2 = YamlConfiguration.loadConfiguration(paperConfig);
            if (loadConfiguration2.getBoolean("proxies.velocity.enabled")) {
                this.pl.utils.log(Level.WARNING, "config/paper-global.yml velocity is set to TRUE! Enabling velocity support.");
                this.isVelocity = true;
            }
        }
        
        File file2 = new File("spigot.yml");
        if (file2.exists()) {
            YamlConfiguration loadConfiguration2 = YamlConfiguration.loadConfiguration(file2);
            if (!this.isVelocity) {
	            if (!loadConfiguration2.getBoolean("settings.bungeecord")) {
	                loadConfiguration2.set("settings.bungeecord", true);
	                try {
	                    loadConfiguration2.save(file2);
	                    this.pl.utils.log(Level.WARNING, "Spigot.yml bungeecord setting must be set to TRUE! Fixed it automatically.");
		                changed = true;
	                }
	                catch (IOException ex2) {
	                    ex2.printStackTrace();
	                }
	            }
            } else {
            	if (loadConfiguration2.getBoolean("settings.bungeecord")) {
	                loadConfiguration2.set("settings.bungeecord", false);
	                try {
	                    loadConfiguration2.save(file2);
	                    this.pl.utils.log(Level.WARNING, "Spigot.yml bungeecord setting must be set to TRUE! Fixed it automatically.");
		                changed = true;
	                }
	                catch (IOException ex2) {
	                    ex2.printStackTrace();
	                }
	            }
            }
        }
        if (changed) {
        	
            this.pl.utils.log(Level.WARNING, "spigot.yml and/or bukkit.yml settings fixed on start, restart for the changes to take effect!");
        }
    }
    
    public void saveConfig(FileConfiguration fileConfiguration, File file) {
        if (fileConfiguration != null) {
            try {
                fileConfiguration.save(file);
            }
            catch (IOException ex) {
                this.pl.utils.log(Level.SEVERE, "&cFailed to save config! Please send the following stack trace to the developer.");
                ex.printStackTrace();
            }
        }
    }
    
    public void checkUseless() {
    	Scheduler.runTaskLater(this.pl, () -> {
    		if (Bukkit.getOnlinePlayers().size() < 1) {
                PlayerServers.this.utils.log(Level.WARNING, "Server went unused for 90 seconds. Nobody connected? Shutting it down.");
                PlayerServers.this.utils.shutdown(10);
            }
        }, 1800L);
    }
    
    public void expiryTask() {
        if (!this.pl.runningExpiryTask) {
            this.pl.runningExpiryTask = true;
            this.pl.utils.debug("Starting server expiry checking task.");
            Scheduler.runTaskTimer(this, () ->{
            	PlayerServers.this.sender.expireCheck();
            }, 600L, 600L);
        }
    }
    
    public void defaultMessages() {
        this.messages.put("shutdown-msg", "Player Server shut down.");
        this.messages.put("only-player-use", "&cThis can only be used by a player.");
        this.messages.put("gamemode-changed", "&aDefault Gamemode set to &o%gamemode%!");
        this.messages.put("force-gamemode-on", "&aForce gamemode &2&oON||&e&oThis will take effect on next server restart!");
        this.messages.put("force-gamemode-off", "&aForce gamemode &c&oOFF||&e&oThis will take effect on next server restart!");
        this.messages.put("difficulty-changed", "&aServer difficulty set to &2&l&o%difficulty%!");
        this.messages.put("whitelist-enabled", "&eServer Whitelist: &aON");
        this.messages.put("whitelist-disabled", "&eServer Whitelist: &cOFF");
        this.messages.put("whitelist-add-timeout", "&c&oWhoops! &e&oTime ran out waiting for your input! Try again.");
        this.messages.put("whitelist-add-cancelled", "&c&oCancelled.");
        this.messages.put("whitelist-modify-instructions", "&aPlease type the player's name or UUID in chat &7&o(or 'cancel' to cancel):");
        this.messages.put("whitelist-cleared", "&c&lCleared the whitelist!");
        this.messages.put("whitelist-added", "&aAdded &o%player%&a to the whitelist.");
        this.messages.put("whitelist-removed", "&cRemoved &o%player%&c from the whitelist.");
        this.messages.put("monster-spawns-on", "&aMonster spawning set to &2&oTrue||&e&oThis will take effect on next server restart!");
        this.messages.put("monster-spawns-off", "&aMonster spawning set to &c&oFalse.||&e&oThis will take effect on next server restart!");
        this.messages.put("animal-spawns-on", "&aAnimal spawning set to &2&oTrue||&e&oThis will take effect on next server restart!");
        this.messages.put("animal-spawns-off", "&aAnimal spawning set to &c&oFalse.||&e&oThis will take effect on next server restart!");
        this.messages.put("npc-spawns-on", "&aVillager spawning set to &2&oTrue||&e&oThis will take effect on next server restart!");
        this.messages.put("npc-spawns-off", "&aVillager spawning set to &c&oFalse.||&e&oThis will take effect on next server restart!");
        this.messages.put("allow-nether-on", "&aAllow nether set to &a&oTrue||&e&oThis will take effect on next server restart!");
        this.messages.put("allow-nether-off", "&aAllow nether set to &c&oFalse||&e&oThis will take effect on next server restart!");
        this.messages.put("allow-flight-on", "&aAllow flight set to &a&oTrue||&e&oThis will take effect on next server restart!");
        this.messages.put("allow-flight-off", "&aAllow flight set to &c&oFalse||&e&oThis will take effect on next server restart!");
        this.messages.put("generate-structures-on", "&aGenerate structures set to &a&oTrue||&e&oThis will take effect on next server restart!");
        this.messages.put("generate-structures-off", "&aGenerate structures set to &c&oFalse||&e&oThis will take effect on next server restart!");
        this.messages.put("kicked-player", "&aYou have kicked &6&l%player% &afrom your server!");
        this.messages.put("got-kicked", "&e&oYou were kicked from this server by %player%");
        this.messages.put("unbanned-player", "&aYou have unbanned &6&l$%layer% &afrom your server!");
        this.messages.put("banned-player", "&aYou have banned &6&l$%layer% &afrom your server!");
        this.messages.put("got-banned", "&e&oYou were evicted! Reason: &c%reason% | &c&oEvicted by: &6%player%");
        this.messages.put("ban-message", "Eviction Reason: %reason% | Evicted by: %player%");
        this.messages.put("pvp-enabled", "&c&lPvP enabled in all worlds!");
        this.messages.put("pvp-disabled", "&a&lPvP disabled in all worlds!");
        this.messages.put("regain-info", "&c&lRelog to OP yourself, use &6&l/myserver regain if others are deOPing you.");
        this.messages.put("opped-player", "&a&lYou have promoted 6&l%player% &a&lto OP.");
        this.messages.put("deopped-player", "&a&lYou have promoted 6&l%player% &a&lto OP.");
        this.messages.put("must-be-online", "&cPlayer must be online to do that!");
        this.messages.put("leave-message", "&a'Til next time! &eSending you to '%server%'...");
        this.messages.put("help-mys-header", "&b&o/MyServer (/mys) Help:");
        this.messages.put("help-mys-settings", "Edit your server settings.");
        this.messages.put("help-mys-ban", "Ban or unban (evict) a player from your server.");
        this.messages.put("help-mys-kick", "Kick a player from your server.");
        this.messages.put("help-mys-whitelist", "Controls the whitelist of your server.");
        this.messages.put("help-mys-op", "OP or deOPs a player on your server.");
        this.messages.put("help-mys-regain", "Wipes OP list and regains control of your server");
        this.messages.put("help-mys-stop", "Stops your server. Restart with &6&l/ps home");
    }
    
    public static PlayerServersAPI getApi() {
        return PlayerServers.api;
    }

}
