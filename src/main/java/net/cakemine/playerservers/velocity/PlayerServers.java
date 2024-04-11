package net.cakemine.playerservers.velocity;

import net.cakemine.playerservers.velocity.objects.PlayerServer;
import net.cakemine.playerservers.velocity.commands.*;
import net.cakemine.playerservers.velocity.wrapper.*;
import net.kyori.adventure.pointer.Pointered;
import net.cakemine.playerservers.velocity.sync.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.google.common.io.*;
import com.google.inject.Inject;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.*;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.*;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class PlayerServers {
    public ProxyServer proxy;
    private PlayerServers pl;
    public List<String> running = new ArrayList<>();
    public ServerManager serverManager;
    public ExpiryTracker expiryTracker;
    public SettingsManager settingsManager;
    public TemplateManager templateManager;
    public PlayerServerAdmin playerServerAdmin;
    public PluginSender sender;
    public PlayerServerCMD playerServer;
    protected static PlayerServersAPI api;
    public Utils utils;
    public Yaml yaml;
    protected HashMap<String, Object> config;
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> serverStore;
    protected HashMap<String, HashMap<String, String>> messages;
    protected HashMap<String, Object> guis;
    protected HashMap<String, Object> playerStore;
    protected HashMap<String, HashMap<String, Object>> online;
    public HashMap<String, HashMap<String, String>> permMap;
    public List<String> blockedCmds;
    public List<String> alwaysOP;
    public HashMap<String, String> msgMap;
    public TreeMap<String, String> playerMap;
    public String prefix;
    public String fallbackSrv;
    public String serversFolder;
    public String wrapperAddress;
    public String proxyAddress;
    public boolean debug;
    public boolean useExpiry;
    public boolean resetExpiry;
    public boolean useTitles;
    public boolean autoPurge;
    public boolean usingWindows;
    public boolean permMapChanged;
    public boolean useQueue;
    public boolean onlineMode;
    public boolean playerMapChanged;
    public long autoPurgeTime;
    public long autoPurgeInterval;
    public int wrapperPort;
    public int joinDelay;
    public int onlineJoinDelay;
    public int globalMaxServers;
    public int globalMaxRam;
    private RepeatTasks repeatTask; 
    public HashMap<String, String> defaultMem;
    public String psCommand;
    public String vers;
    public String wrapper;
    public HashMap<Player, RegisteredServer> usingHelper;
    protected Controller ctrl;
	private Path dataDirectory;
	public static Logger logger;
	@Inject
    private PluginContainer container;
	public EventManager eventManager;
	
	@Inject
    public PlayerServers(ProxyServer server, Logger proxyLogger, @DataDirectory Path dataDirectory, EventManager eventManager) {
        this.proxy = server;
        this.pl = this;
        this.eventManager = eventManager;
        logger = proxyLogger;
        this.dataDirectory = dataDirectory;
        this.serverManager = new ServerManager(this);
        this.expiryTracker = new ExpiryTracker(this);
        this.settingsManager = new SettingsManager(this);
        this.templateManager = new TemplateManager(this);
        this.playerServerAdmin = new PlayerServerAdmin(this);
        this.sender = new PluginSender(this);
        this.playerServer = null;
        this.utils = new Utils(this);
        this.permMap = new HashMap<String, HashMap<String, String>>();
        this.msgMap = new HashMap<String, String>();
        this.playerMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        this.debug = true;
        this.useExpiry = true;
        this.resetExpiry = false;
        this.useTitles = true;
        this.autoPurge = false;
        this.usingWindows = false;
        this.permMapChanged = false;
        this.useQueue = true;
        this.onlineMode = true;
        this.playerMapChanged = false;
        this.autoPurgeInterval = 21600000L;
        this.joinDelay = 10;
        this.onlineJoinDelay = 3;
        this.globalMaxServers = -1;
        this.globalMaxRam = -1;
        this.defaultMem = new HashMap<String, String>();
        this.psCommand = "playerserver";
        this.vers = "-1";
        this.wrapper = "default";
        this.usingHelper = new HashMap<Player, RegisteredServer>();
        this.ctrl = null;
    }
    
	@Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
		DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
		this.yaml = new Yaml(options);
        this.vers = container.getDescription().getVersion().get();
        this.usingWindows = this.usingWindows();
        this.proxyAddress = this.utils.getProxyIp();
        this.proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("playerservers:core"));
        this.proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("bungeecord:proxy"));

        this.proxy.getEventManager().register(this, new Listeners(this));
        this.proxy.getEventManager().register(this, new PluginListener(this));
        PlayerServers.api = new PlayerServersAPI(this);
        this.reload();
        this.setupScripts();
        this.playerServer = new PlayerServerCMD(this, this.psCommand);
        repeatTask = new RepeatTasks(this);
        this.pl.proxy.getScheduler().buildTask(this.pl, () -> {
        	repeatTask.run();
        }).delay(30L, TimeUnit.SECONDS).repeat(30L, TimeUnit.SECONDS).schedule();
        this.proxy.getCommandManager().register(this.psCommand, new PlayerServerCMD(this, this.psCommand), "pserver", "psrv", "ps");
    	this.proxy.getCommandManager().register("playerserveradmin", new PlayerServerAdmin(this), "pserveradmin", "psrvadmin", "psa");

        this.loadOnlineServers();
    }
    
    public void onDisable() {
        if (this.wrapper.equalsIgnoreCase("default")) {
            this.ctrl.disconnect();
        }
        Iterator<ScheduledTask> tasks = this.proxy.getScheduler().tasksByPlugin(pl).iterator();
    	while (tasks.hasNext()) {
    		tasks.next().cancel();
    	}
    }
    
    public boolean usingWindows() {
        return System.getProperty("os.name").matches("(?i)(.*)(windows)(.*)");
    }
    
    public void reload() {
        String psCommand = this.psCommand;
        this.playerMap.clear();
        this.msgMap.clear();
        this.serverManager.serverMap.clear();
        this.templateManager.templates.clear();
        this.loadFiles();
        this.updateConfig();
        this.loadConfig();
        this.vers = "565421"; // Don't modify this!
        this.loadMsgs();
        this.loadGUIs();
        this.loadServers();
        this.loadPlayers();
        this.loadPermissions();
        this.templateManager.loadTemplates();
        this.utils.vCheck();
        this.sender.reSyncAll();
        if (!this.psCommand.equalsIgnoreCase(psCommand)) {
        	this.proxy.getCommandManager().unregister(psCommand);
        	this.proxy.getCommandManager().unregister("playerserveradmin");
        	this.proxy.getCommandManager().register(this.psCommand, new PlayerServerCMD(this, this.psCommand), "pserver", "psrv", "ps");
        	this.proxy.getCommandManager().register("playerserveradmin", new PlayerServerAdmin(this), "pserveradmin", "psrvadmin", "psa");
        }
        if (this.ctrl == null && this.wrapper.equalsIgnoreCase("default")) {
        	this.proxy.getScheduler().buildTask(this.pl, () -> {
        		this.ctrl = new Controller(this);
        		this.ctrl.connect();
        	}).delay(3L, TimeUnit.SECONDS).schedule();
        }
        else if (this.ctrl == null && this.wrapper.equalsIgnoreCase("remote")) {
        	this.proxy.getScheduler().buildTask(this.pl, () -> {
        		this.ctrl = new Controller(this);
        		this.ctrl.setAddress(this.wrapperAddress);
        		this.ctrl.connect();
        	}).delay(3L, TimeUnit.SECONDS).schedule();
        }
        this.onlineMode = this.proxy.getConfiguration().isOnlineMode();
    }
    
    public void loadFiles() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        File config = new File(this.getDataFolder(), "config.yml");
        File servers = new File(this.getDataFolder(), "servers.yml");
        File messages = new File(this.getDataFolder(), "messages.yml");
        File guis = new File(this.getDataFolder(), "guis.yml");
        File players = new File(this.getDataFolder(), "players.yml");
        if (!config.exists()) this.copyResource(config);
        if (!servers.exists()) this.copyResource(servers);
        if (!messages.exists()) this.copyResource(messages);
        if (!guis.exists()) this.copyResource(guis);
        if (!players.exists()) this.copyResource(players);
        
		try {
			InputStream inputStream = new FileInputStream(this.getDataFolder().getPath() + File.separator + "config.yml");
			this.config = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}        
		try {
			InputStream inputStream2 = new FileInputStream(this.getDataFolder().getPath() + File.separator + "servers.yml");
			this.serverStore = yaml.load(new InputStreamReader(inputStream2, "UTF-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        
		try {
			InputStream inputStream3 = new FileInputStream(this.getDataFolder().getPath() + File.separator + "messages.yml");
			this.messages = yaml.load(new InputStreamReader(inputStream3, "UTF-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        
		try {
			InputStream inputStream4 = new FileInputStream(this.getDataFolder().getPath() + File.separator + "guis.yml");
			this.guis = yaml.load(new InputStreamReader(inputStream4, "UTF-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        		
		try {
			InputStream inputStream5 = new FileInputStream(this.getDataFolder().getPath() + File.separator + "players.yml");
			this.playerStore = yaml.load(new InputStreamReader(inputStream5, "UTF-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        
    }
    
    public void updateConfig() {
    	boolean changed = false;
        if (this.config.get("blocked-commands") == null || ((List<String>)this.config.get("blocked-commands")).isEmpty()) {
            this.config.put("blocked-commands", new String[] { "^(/execute(.*)|/)(minecraft:)(ban(-ip)?|pardon(-ip)?|stop|reload)($|\\s.*)?" });
            this.utils.log("Added missing blocked-commands config option to the config.");
            changed = true;
        }
        if (this.config.get("ps-custom-command") == null || this.config.get("ps-custom-command").toString().isEmpty()) {
            this.config.put("ps-custom-command", "/playerserver");
            this.utils.log("Added missing ps-custom-command config option to the config.");
            changed = true;
        }
        if (this.config.get("global-max-RAM") == null) {
            this.config.put("global-max-RAM", (-1));
            this.utils.log("Added missing global-max-RAM config option to the config.");
            changed = true;
        }
        if (this.config.get("global-max-servers") == null || (int)this.config.get("global-max-servers") < -1) {
            this.config.put("global-max-servers", (-1));
            this.utils.log("Added missing global-max-servers config option to the config.");
            changed = true;
        }
        if (this.config.get("always-op") == null) {
            this.config.put("always-op", Arrays.asList("Notch", "069a79f4-44e9-4726-a5be-fca90e38aaf5"));
            this.utils.log("Added default always-op list to existing config.");
            changed = true;
        }
        if (this.config.get("reset-expiry-on-create") == null) {
            this.config.put("reset-expiry-on-create", false);
            this.utils.log("Added default reset-expiry-on-create to existing config.");
            changed = true;
        }
        if (this.config.get("use-titles") == null) {
            this.config.put("use-titles", true);
            this.utils.log("Added default use-titles to existing config.");
            changed = true;
        }
        if (this.config.get("purge-servers") == null) {
            this.config.put("purge-servers", false);
            this.utils.log("Added default purge-servers to existing config.");
            changed = true;
        }
        if (this.config.get("purge-after") == null) {
            this.config.put("purge-after", "30 days");
            this.utils.log("Added default purge-after to existing config.");
            changed = true;
        }
        if (this.config.get("purge-interval") == null) {
            this.config.put("purge-interval", "6 hours");
            this.utils.log("Added default purge-interval to existing config.");
            changed = true;
        }
        if (this.config.get("wrapper") == null) {
            this.config.put("wrapper", "screen");
            this.utils.log("Added screen as 'wrapper' to existing config.");
            changed = true;
        }
        if (this.config.get("wrapper-control-address") == null) {
            this.config.put("wrapper-control-address", "localhost");
            this.utils.log("Added default wrapper-control-address to existing config.");
            changed = true;
        }
        if (this.config.get("wrapper-control-port") == null) {
            this.config.put("wrapper-control-port", 5155);
            this.utils.log("Added default wrapper-control-port to existing config.");
            changed = true;
        }
        if (this.config.get("online-join-delay") == null) {
            this.config.put("online-join-delay", 3);
            this.utils.log("Added default online-join-delay to existing config.");
            changed = true;
        }
        if (this.config.get("use-startup-queue") == null) {
            this.config.put("use-startup-queue", true);
            this.utils.log("Added default use-startup-queue to existing config.");
            changed = true;
        }
        if (changed) {
        	this.saveConfig(this.config, "config.yml");
        }
    }
    
    public void saveConfig(Object config, String s) {
    	File configFile = new File(this.getDataFolder(), s);
    	try {
            // Write configuration to file
            try (FileWriter writer = new FileWriter(configFile)) {
            	
            	this.yaml.dump(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception properly in your application
        }
    	   
    }
    public void loadConfig() {
        this.debug = (boolean)this.config.get("debug");
        this.psCommand = (String) this.config.get("ps-custom-command");
        if (this.psCommand.startsWith("/")) {
            this.psCommand = this.psCommand.substring(1);
        }
        this.blockedCmds = (List)this.config.get("blocked-commands");
        this.useExpiry = (boolean)this.config.get("use-expire-dates");
        this.utils.debug("use-expire-dates: " + this.useExpiry);
        this.prefix = this.config.get("prefix").toString();
        this.utils.debug("prefix: " + this.prefix);
        if (this.config.get("hub-server") == null || this.config.get("hub-server").equals("default")) {
            ProxyServer proxy = this.proxy;
            ProxyConfig configurationAdapter = this.proxy.getConfiguration();
            String fallback = "default_server";
            ProxyServer proxy2 = this.proxy;
            this.fallbackSrv = configurationAdapter.getAttemptConnectionOrder().get(0);
        }
        else {
            this.fallbackSrv = this.config.get("hub-server").toString();
        }
        this.utils.debug("hub-server: " + this.fallbackSrv);
        if (this.config.get("servers-folder").equals("default")) {
            this.serversFolder = this.getDataFolder().getAbsolutePath() + File.separator + "servers";
        }
        else {
            this.serversFolder = this.config.get("servers-folder").toString();
        }
        this.utils.debug("servers-folder: " + this.serversFolder);
        this.joinDelay = (int)this.config.get("startup-join-delay");
        this.onlineJoinDelay = (int)this.config.get("online-join-delay");
        this.utils.debug("config max servers = " + this.config.get("global-max-servers") + " | class =" + this.config.getClass());
        this.globalMaxServers = (int)this.config.get("global-max-servers");
        this.utils.debug("global-max-servers = " + this.globalMaxServers);
        this.globalMaxRam = (int)this.config.get("global-max-RAM");
        this.utils.debug("global-max-RAM = " + this.globalMaxRam);
        this.alwaysOP = (List)this.config.get("always-op");
        this.resetExpiry = (boolean)this.config.get("reset-expiry-on-create");
        this.utils.debug("reset-expiry-on-create = " + this.resetExpiry);
        this.useTitles = (boolean)this.config.get("use-titles");
        this.utils.debug("use-titles = " + this.useTitles);
        this.autoPurge = (boolean)this.config.get("purge-servers");
        this.utils.debug("purge-servers = " + this.autoPurge);
        this.autoPurgeTime = this.expiryTracker.stringToMillis(this.config.get("purge-after").toString());
        this.utils.debug("purge-after in milliseconds = " + this.autoPurgeTime);
        this.autoPurgeInterval = this.expiryTracker.stringToMillis(this.config.get("purge-interval").toString());
        this.utils.debug("purge-interval in milliseconds = " + this.autoPurgeInterval);
        this.wrapper = this.config.get("wrapper").toString();
        if (this.wrapper.matches("(?i)(scr(e*)n)")) {
            this.wrapper = "screen";
        }
        else if (this.wrapper.matches("(?i)(tm(u)?x)")) {
            this.wrapper = "tmux";
        }
        else if (this.wrapper.matches("(?i)(r(e)?m(o)?t(e)?)")) {
            this.wrapper = "remote";
        }
        else {
            this.wrapper = "default";
        }
        this.utils.debug("wrapper = " + this.wrapper);
        this.wrapperPort = (int)this.config.get("wrapper-control-port");
        this.utils.debug("wrapper-control-port = " + this.wrapperPort);
        this.wrapperAddress = (this.wrapper == "default") ? "127.0.0.1" : this.config.get("wrapper-control-address").toString();
        this.utils.debug("wrapper-control-address = " + this.wrapperAddress);
        this.useQueue = (boolean)this.config.get("use-startup-queue");
        this.utils.debug("use-startup-queue = " + this.useQueue);
    }
    
    public void loadGUIs() {
        this.updateGUIs();
        try {
			InputStream inputStream4 = new FileInputStream(this.getDataFolder().getPath() + File.separator + "guis.yml");
			Map<String, Object> yamlData = yaml.load(new InputStreamReader(inputStream4, "UTF-8"));
	    	ObjectMapper jsonMapper = new ObjectMapper();
	        String jsonData = jsonMapper.writeValueAsString(yamlData);
	        this.sender.guisSerialized = jsonData.toString();
		} catch (FileNotFoundException | JsonProcessingException | UnsupportedEncodingException e) {
			this.sender.guisSerialized = null;
		}
    }
    
    public void updateGUIs() {
        boolean changed = false;
        if (this.guis.get("settings-icons") instanceof HashMap && ((HashMap)this.guis.get("settings-icons")).get("expire-tracker") == null) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("item-id", "clock");
            hashMap.put("item-name", "&e&o&lTime Until Server Expires:");
            hashMap.put("item-lore", "Time Left: %time-left%||Expire Date: %expire-date%");
            this.utils.debug("updating GUI: settings with expire-tracker: " + hashMap.toString());
            ((HashMap)this.guis.get("settings-icons")).put("expire-tracker", hashMap);
            changed = true;
        }
        if (this.guis.get("control-title") == null) {
        	this.guis.put("control-title", "&5&lPS Control &0&l�");
        	changed = true;
        }
        if (this.guis.get("control-icons") == null) {
            HashMap<String, HashMap> hashMap2 = new HashMap<String, HashMap>();
            HashMap<String, String> hashMap3 = new HashMap<String, String>();
            hashMap3.put("item-id", "clock");
            hashMap3.put("item-name", "&e&o&lTime Until Server Expires:");
            hashMap3.put("item-lore", "Time Left: %time-left%||Expire Date: %expire-date%");
            hashMap2.put("expire-tracker", (HashMap)hashMap3.clone());
            HashMap<String, String> hashMap4 = new HashMap<String, String>();
            hashMap4.put("item-id", "emerald_block");
            hashMap4.put("item-name", "&a&o&lCreate a Server");
            hashMap4.put("item-lore", "You do not have a server,||click here to create one!");
            hashMap2.put("create-server", (HashMap)hashMap4.clone());
            HashMap<String, String> hashMap5 = new HashMap<String, String>();
            hashMap5.put("item-id", "redstone_block");
            hashMap5.put("item-name", "&c&o&lDelete your server");
            hashMap5.put("item-lore", "You have a server,||click here to delete it!||You will have to confirm!");
            hashMap2.put("delete-server", (HashMap)hashMap5.clone());
            HashMap<String, String> hashMap6 = new HashMap<String, String>();
            hashMap6.put("item-id", "tnt");
            hashMap6.put("item-name", "&c&lCONFIRM Server Delete!");
            hashMap6.put("item-lore", "&c&lWarning: Clicking this will||&c&lpermanently delete your server!");
            hashMap2.put("delete-confirm", (HashMap)hashMap6.clone());
            HashMap<String, String> hashMap7 = new HashMap<String, String>();
            hashMap7.put("item-id", "compass");
            hashMap7.put("item-name", "&a&lJoin Your Server");
            hashMap7.put("item-lore", "Click to join your server!");
            hashMap2.put("join-server", (HashMap)hashMap7.clone());
            HashMap<String, String> hashMap8 = new HashMap<String, String>();
            hashMap8.put("item-id", "diamond");
            hashMap8.put("item-name", "&b&lStart Your Server");
            hashMap8.put("item-lore", "Click to start your server!");
            hashMap2.put("start-server", (HashMap)hashMap8.clone());
            HashMap<String, String> hashMap9 = new HashMap<String, String>();
            hashMap9.put("item-id", "coal");
            hashMap9.put("item-name", "&c&lStop Your Server");
            hashMap9.put("item-lore", "Click to stop your server!");
            hashMap2.put("stop-server", (HashMap)hashMap9.clone());
            HashMap<String, String> hashMap10 = new HashMap<String, String>();
            hashMap10.put("item-id", "compass");
            hashMap10.put("item-name", "&c&lLeave Your Server");
            hashMap10.put("item-lore", "Click to leave your server!");
            hashMap2.put("leave-server", (HashMap)hashMap10.clone());
            this.guis.put("control-icons", hashMap2);
            changed = true;
        }
        if (((Map<String, Map<String, Object>>)this.guis.get("servers-icons")).get("server").get("item-lore").equals("Click to join this Player Server.")) {
        	((Map<String, Map<String, Object>>)this.guis.get("servers-icons")).get("server").put("item-lore", "Owner: %player%||Players: %current-players%/%max-players%||UUID: %player-uuid%||MOTD: %motd%||Template: %template-name%||Expire Date: %expire-date%||Time Left: %time-left%||Whitelist: %whitelist%");
        	changed = true;
        }
        if (changed) {
            this.saveConfig(this.guis, "guis.yml");
            this.utils.debug("Saved updated guis.yml file.");
        }
    }
    
    public void loadMsgs() {
        this.utils.debug("messages class: " + this.messages.get("messages").getClass());
        this.msgMap.put("no-permissions",  this.messages.get("messages").get("no-permissions").toString());
        this.msgMap.put("no-server", this.messages.get("messages").get("no-server").toString());
        this.msgMap.put("other-no-server", this.messages.get("messages").get("other-no-server").toString());
        this.msgMap.put("not-player-server", this.messages.get("messages").get("not-player-server").toString());
        this.msgMap.put("no-player-specified", this.messages.get("messages").get("no-player-specified").toString());
        this.msgMap.put("no-value-specified", this.messages.get("messages").get("no-value-specified").toString());
        this.msgMap.put("only-player-use", this.messages.get("messages").get("only-player-use").toString());
        this.msgMap.put("have-server", this.messages.get("messages").get("have-server").toString());
        this.msgMap.put("other-have-server", this.messages.get("messages").get("other-have-server").toString());
        this.msgMap.put("sent-fallback", this.messages.get("messages").get("sent-fallback").toString());
        this.msgMap.put("blocked-cmd", this.messages.get("messages").get("blocked-cmd").toString());
        this.msgMap.put("recently-started", this.messages.get("messages").get("recently-started").toString());
        this.msgMap.put("server-expired", this.messages.get("messages").get("server-expired").toString());
        this.msgMap.put("create-start", this.messages.get("messages").get("create-start").toString());
        this.msgMap.put("create-finished", this.messages.get("messages").get("create-finished").toString());
        this.msgMap.put("create-copying-files", this.messages.get("messages").get("create-copying-files").toString());
        this.msgMap.put("create-failed-copy", this.messages.get("messages").get("create-failed-copy").toString());
        this.msgMap.put("create-missing-template", this.messages.get("messages").get("create-missing-template").toString());
        this.msgMap.put("expire-times", this.messages.get("messages").get("expire-times").toString());
        this.msgMap.put("others-expire-times", this.messages.get("messages").get("others-expire-times").toString());
        this.msgMap.put("check-expire-times", this.messages.get("messages").get("check-expire-times").toString());
        this.msgMap.put("check-expire-unlimited", this.messages.get("messages").get("check-expire-unlimited").toString());
        this.msgMap.put("days-left", this.messages.get("messages").get("days-left").toString());
        this.msgMap.put("not-enough-time", this.messages.get("messages").get("not-enough-time").toString());
        this.msgMap.put("no-share-unlimited", this.messages.get("messages").get("no-share-unlimited").toString());
        this.msgMap.put("other-days-left", this.messages.get("messages").get("other-days-left").toString());
        this.msgMap.put("other-removed-days", this.messages.get("messages").get("other-removed-days").toString());
        this.msgMap.put("other-added-days", this.messages.get("messages").get("other-added-days").toString());
        this.msgMap.put("invalid-time-unit", this.messages.get("messages").get("invalid-time-unit").toString());
        this.msgMap.put("delete-warning", this.messages.get("messages").get("delete-warning").toString());
        this.msgMap.put("start-delete", this.messages.get("messages").get("start-delete").toString());
        this.msgMap.put("finish-delete", this.messages.get("messages").get("finish-delete").toString());
        this.msgMap.put("finish-delete-problem", this.messages.get("messages").get("finish-delete-problem").toString());
        this.msgMap.put("other-server-already-online", this.messages.get("messages").get("other-server-already-online").toString());
        this.msgMap.put("server-already-online", this.messages.get("messages").get("server-already-online").toString());
        this.msgMap.put("stopping-all-servers", this.messages.get("messages").get("stopping-all-servers").toString());
        this.msgMap.put("server-join-online-owner", this.messages.get("messages").get("server-join-online-owner").toString());
        this.msgMap.put("server-join-online-guest", this.messages.get("messages").get("server-join-online-guest").toString());
        this.msgMap.put("server-join-offline-owner", this.messages.get("messages").get("server-join-offline-owner").toString());
        this.msgMap.put("server-join-offline-guest", this.messages.get("messages").get("server-join-offline-guest").toString());
        this.msgMap.put("server-stop-online-owner", this.messages.get("messages").get("server-stop-online-owner").toString());
        this.msgMap.put("server-stop-online-guest", this.messages.get("messages").get("server-stop-online-guest").toString());
        this.msgMap.put("server-stop-offline-owner", this.messages.get("messages").get("server-stop-offline-owner").toString());
        this.msgMap.put("server-stop-offline-guest", this.messages.get("messages").get("server-stop-offline-guest").toString());
        this.msgMap.put("other-server-not-online", this.messages.get("messages").get("other-server-not-online").toString());
        this.msgMap.put("other-started-server", this.messages.get("messages").get("other-started-server").toString());
        this.msgMap.put("other-stopping-server", this.messages.get("messages").get("other-stopping-server").toString());
        this.msgMap.put("got-kicked", this.messages.get("messages").get("got-kicked").toString());
        this.msgMap.put("got-banned", this.messages.get("messages").get("got-banned").toString());
        this.msgMap.put("config-reloaded", this.messages.get("messages").get("config-reloaded").toString());
        this.msgMap.put("max-memory-changed", this.messages.get("messages").get("max-memory-changed").toString());
        this.msgMap.put("start-memory-changed", this.messages.get("messages").get("start-memory-changed").toString());
        this.msgMap.put("max-players-count", this.messages.get("messages").get("max-players-count").toString());
        this.msgMap.put("invalid-memory-format", this.messages.get("messages").get("invalid-memory-format").toString());
        this.msgMap.put("invalid-slot-count", this.messages.get("messages").get("invalid-slot-count").toString());
        this.msgMap.put("start-greater-max", this.messages.get("messages").get("start-greater-max").toString());
        this.msgMap.put("max-lessthan-start", this.messages.get("messages").get("max-lessthan-start").toString());
        this.msgMap.put("player-never-joined", this.messages.get("messages").get("player-never-joined").toString());
        this.msgMap.put("no-template-specified", this.messages.get("messages").get("no-template-specified").toString());
        this.msgMap.put("template-doesnt-exist", this.messages.get("messages").get("template-doesnt-exist").toString());
        this.msgMap.put("no-templates-found", this.messages.get("messages").get("no-templates-found").toString());
        this.msgMap.put("available-templates", this.messages.get("messages").get("available-templates").toString());
        this.msgMap.put("no-template-permissions", this.messages.get("messages").get("no-template-permissions").toString());
        this.msgMap.put("max-memory-reached", this.messages.get("messages").get("max-memory-reached").toString());
        this.msgMap.put("max-servers-reached", this.messages.get("messages").get("max-servers-reached").toString());
        this.msgMap.put("added-to-queue", this.messages.get("messages").get("added-to-queue").toString());
        this.msgMap.put("removed-from-queue", this.messages.get("messages").get("removed-from-queue").toString());
        this.msgMap.put("queue-startup", this.messages.get("messages").get("queue-startup").toString());
        this.msgMap.put("gamemode-changed", this.messages.get("messages").get("gamemode-changed").toString());
        this.msgMap.put("force-gamemode-on", this.messages.get("messages").get("force-gamemode-on").toString());
        this.msgMap.put("force-gamemode-off", this.messages.get("messages").get("force-gamemode-off").toString());
        this.msgMap.put("difficulty-changed", this.messages.get("messages").get("difficulty-changed").toString());
        this.msgMap.put("whitelist-add-timeout", this.messages.get("messages").get("whitelist-add-timeout").toString());
        this.msgMap.put("whitelist-add-cancelled", this.messages.get("messages").get("whitelist-add-cancelled").toString());
        this.msgMap.put("whitelist-modify-instructions", this.messages.get("messages").get("whitelist-modify-instructions").toString());
        this.msgMap.put("whitelist-cleared", this.messages.get("messages").get("whitelist-cleared").toString());
        this.msgMap.put("whitelist-added", this.messages.get("messages").get("whitelist-added").toString());
        this.msgMap.put("whitelist-removed", this.messages.get("messages").get("whitelist-removed").toString());
        this.msgMap.put("whitelist-enabled", this.messages.get("messages").get("whitelist-enabled").toString());
        this.msgMap.put("whitelist-disabled", this.messages.get("messages").get("whitelist-disabled").toString());
        this.msgMap.put("monster-spawns-on", this.messages.get("messages").get("monster-spawns-on").toString());
        this.msgMap.put("monster-spawns-off", this.messages.get("messages").get("monster-spawns-off").toString());
        this.msgMap.put("animal-spawns-on", this.messages.get("messages").get("animal-spawns-on").toString());
        this.msgMap.put("animal-spawns-off", this.messages.get("messages").get("animal-spawns-off").toString());
        this.msgMap.put("npc-spawns-on", this.messages.get("messages").get("npc-spawns-on").toString());
        this.msgMap.put("npc-spawns-off", this.messages.get("messages").get("npc-spawns-off").toString());
        this.msgMap.put("allow-nether-on", this.messages.get("messages").get("allow-nether-on").toString());
        this.msgMap.put("allow-nether-off", this.messages.get("messages").get("allow-nether-off").toString());
        this.msgMap.put("allow-flight-on", this.messages.get("messages").get("allow-flight-on").toString());
        this.msgMap.put("allow-flight-off", this.messages.get("messages").get("allow-flight-off").toString());
        this.msgMap.put("generate-structures-on", this.messages.get("messages").get("generate-structures-on").toString());
        this.msgMap.put("generate-structures-off", this.messages.get("messages").get("generate-structures-off").toString());
        this.msgMap.put("kicked-player", this.messages.get("messages").get("kicked-player").toString());
        this.msgMap.put("got-kicked", this.messages.get("messages").get("got-kicked").toString());
        this.msgMap.put("banned-player", this.messages.get("messages").get("banned-player").toString());
        this.msgMap.put("unbanned-player", this.messages.get("messages").get("unbanned-player").toString());
        this.msgMap.put("got-banned", this.messages.get("messages").get("got-banned").toString());
        this.msgMap.put("ban-message", this.messages.get("messages").get("ban-message").toString());
        this.msgMap.put("pvp-enabled", this.messages.get("messages").get("pvp-enabled").toString());
        this.msgMap.put("pvp-disabled", this.messages.get("messages").get("pvp-disabled").toString());
        this.msgMap.put("regain-info", this.messages.get("messages").get("regain-info").toString());
        this.msgMap.put("opped-player", this.messages.get("messages").get("opped-player").toString());
        this.msgMap.put("deopped-player", this.messages.get("messages").get("deopped-player").toString());
        this.msgMap.put("must-be-online", this.messages.get("messages").get("must-be-online").toString());
        this.msgMap.put("leave-message", this.messages.get("messages").get("leave-message").toString());
        this.msgMap.put("motd-display", this.messages.get("messages").get("motd-display").toString());
        this.msgMap.put("motd-changed", this.messages.get("messages").get("motd-changed").toString());
        this.msgMap.put("motd-too-long", this.messages.get("messages").get("motd-too-long").toString());
        this.msgMap.put("server-join-online-guest-title", this.messages.get("messages").get("server-join-online-guest-title").toString());
        this.msgMap.put("server-join-offline-guest-title", this.messages.get("messages").get("server-join-offline-guest-title").toString());
        this.msgMap.put("server-join-online-owner-title", this.messages.get("messages").get("server-join-online-owner-title").toString());
        this.msgMap.put("server-join-offline-owner-title", this.messages.get("messages").get("server-join-offline-owner-title").toString());
        this.msgMap.put("server-stop-online-owner-title", this.messages.get("messages").get("server-stop-online-owner-title").toString());
        this.msgMap.put("server-stop-online-guest-title", this.messages.get("messages").get("server-stop-online-guest-title").toString());
        this.msgMap.put("server-stop-offline-owner-title", this.messages.get("messages").get("server-stop-offline-owner-title").toString());
        this.msgMap.put("server-stop-offline-guest-title", this.messages.get("messages").get("server-stop-offline-guest-title").toString());
        this.msgMap.put("other-server-not-online-title", this.messages.get("messages").get("other-server-not-online-title").toString());
        this.msgMap.put("no-server-title", this.messages.get("messages").get("no-server-title").toString());
        this.msgMap.put("other-no-server-title", this.messages.get("messages").get("other-no-server-title").toString());
        this.msgMap.put("server-expired-title", this.messages.get("messages").get("server-expired-title").toString());
        this.msgMap.put("max-memory-reached-title", this.messages.get("messages").get("max-memory-reached-title").toString());
        this.msgMap.put("max-servers-reached-title", this.messages.get("messages").get("max-servers-reached-title").toString());
        this.msgMap.put("max-players-count", this.messages.get("messages").get("max-players-count").toString());
        this.msgMap.put("no-player-specified-title", this.messages.get("messages").get("no-player-specified-title").toString());
        this.msgMap.put("sent-fallback-title", this.messages.get("messages").get("sent-fallback-title").toString());
        this.msgMap.put("not-player-server-title", this.messages.get("messages").get("not-player-server-title").toString());
        this.msgMap.put("template-doesnt-exist-title", this.messages.get("messages").get("template-doesnt-exist-title").toString());
        this.msgMap.put("no-templates-found-title", this.messages.get("messages").get("no-templates-found-title").toString());
        this.msgMap.put("create-start-title", this.messages.get("messages").get("create-start-title").toString());
        this.msgMap.put("have-server-title", this.messages.get("messages").get("have-server-title").toString());
        this.msgMap.put("delete-warning-title", this.messages.get("messages").get("delete-warning-title").toString());
        this.msgMap.put("recently-started-title", this.messages.get("messages").get("recently-started-title").toString());
        this.msgMap.put("added-to-queue-title", this.messages.get("messages").get("added-to-queue-title").toString());
        this.msgMap.put("removed-from-queue-title", this.messages.get("messages").get("removed-from-queue-title").toString());
        this.msgMap.put("queue-startup-title", this.messages.get("messages").get("queue-startup-title").toString());
        this.msgMap.put("help-ps-header", this.messages.get("messages").get("help-ps-header").toString());
        this.msgMap.put("help-ps-join", this.messages.get("messages").get("help-ps-join").toString());
        this.msgMap.put("help-ps-leave", this.messages.get("messages").get("help-ps-leave").toString());
        this.msgMap.put("help-ps-create", this.messages.get("messages").get("help-ps-create").toString());
        this.msgMap.put("help-ps-home", this.messages.get("messages").get("help-ps-home").toString());
        this.msgMap.put("help-ps-stop", this.messages.get("messages").get("help-ps-stop").toString());
        this.msgMap.put("help-ps-delete", this.messages.get("messages").get("help-ps-delete").toString());
        this.msgMap.put("help-ps-motd", this.messages.get("messages").get("help-ps-motd").toString());
        this.msgMap.put("help-ps-time", this.messages.get("messages").get("help-ps-time").toString());
        this.msgMap.put("help-ps-worlds", this.messages.get("messages").get("help-ps-worlds").toString());
        this.msgMap.put("help-ps-sharetime", this.messages.get("messages").get("help-ps-sharetime").toString());
        this.msgMap.put("help-psa-header", this.messages.get("messages").get("help-psa-header").toString());
        this.msgMap.put("help-psa-create", this.messages.get("messages").get("help-psa-create").toString());
        this.msgMap.put("help-psa-templates", this.messages.get("messages").get("help-psa-templates").toString());
        this.msgMap.put("help-psa-join", this.messages.get("messages").get("help-psa-join").toString());
        this.msgMap.put("help-psa-start", this.messages.get("messages").get("help-psa-start").toString());
        this.msgMap.put("help-psa-stop", this.messages.get("messages").get("help-psa-stop").toString());
        this.msgMap.put("help-psa-delete", this.messages.get("messages").get("help-psa-delete").toString());
        this.msgMap.put("help-psa-stopall", this.messages.get("messages").get("help-psa-stopall").toString());
        this.msgMap.put("help-psa-kill", this.messages.get("messages").get("help-psa-kill").toString());
        this.msgMap.put("help-psa-addtime", this.messages.get("messages").get("help-psa-addtime").toString());
        this.msgMap.put("help-psa-removetime", this.messages.get("messages").get("help-psa-removetime").toString());
        this.msgMap.put("help-psa-checktime", this.messages.get("messages").get("help-psa-checktime").toString());
        this.msgMap.put("help-psa-maxmem", this.messages.get("messages").get("help-psa-maxmem").toString());
        this.msgMap.put("help-psa-startmem", this.messages.get("messages").get("help-psa-startmem").toString());
        this.msgMap.put("help-psa-slots", this.messages.get("messages").get("help-psa-slots").toString());
        this.msgMap.put("help-psa-reload", this.messages.get("messages").get("help-psa-reload").toString());
        this.msgMap.put("help-psa-motd", this.messages.get("messages").get("help-psa-motd").toString());
        this.msgMap.put("help-mys-header", this.messages.get("messages").get("help-mys-header").toString());
        this.msgMap.put("help-mys-settings", this.messages.get("messages").get("help-mys-settings").toString());
        this.msgMap.put("help-mys-ban", this.messages.get("messages").get("help-mys-ban").toString());
        this.msgMap.put("help-mys-kick", this.messages.get("messages").get("help-mys-kick").toString());
        this.msgMap.put("help-mys-whitelist", this.messages.get("messages").get("help-mys-whitelist").toString());
        this.msgMap.put("help-mys-op", this.messages.get("messages").get("help-mys-op").toString());
        this.msgMap.put("help-mys-regain", this.messages.get("messages").get("help-mys-regain").toString());
        this.msgMap.put("help-mys-stop",  this.messages.get("messages").get("help-mys-stop").toString());
        this.updateMsgs();
    }
    
    public void updateMsgs() {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("max-memory-changed", "&a%server-owner%'s server maximum memory set to %max-mem%||&aThis will take effect on next restart.");
        hashMap.put("start-memory-changed", "&a%server-owner%'s server starting memory set to %start-mem%||&aThis will take effect on next restart.");
        hashMap.put("invalid-memory-format", "&cInvalid memory format. Valid format examples: 512M, 1G, 1024M");
        hashMap.put("start-greater-max", "&cStart memory must be less than or equal to max memory.");
        hashMap.put("max-lessthan-start", "&cMax memory must be greater than or equal to the start memory.");
        hashMap.put("stopping-all-servers", "&6&lShutting down all player servers...");
        hashMap.put("no-template-specified", "&cNo template specified to create a server from!");
        hashMap.put("template-doesnt-exist", "&cThe specified template does not exist.");
        hashMap.put("no-templates-found", "&cNo templates were found in the templates folder!");
        hashMap.put("available-templates", "&b&oAvailable server templates:");
        hashMap.put("max-memory-reached", "&cMemory limit reached, try again later!");
        hashMap.put("max-servers-reached", "&cServer limit reached, try again later!");
        hashMap.put("invalid-time-unit", "&cInvalid time unit. &eValid units: ||&e&oMonths, Weeks, Days, Hours, Minutes");
        hashMap.put("no-template-permissions", "&c&oYou do not have permissions to use this template!");
        hashMap.put("invalid-slot-count", "&cSlot count must be a number greater than 0!");
        hashMap.put("max-players-count", "&a%server-owner%'s server max player count set to %slot-count%");
        hashMap.put("added-to-queue", "&aServer added to the startup queue, your server will start when a slot is available.");
        hashMap.put("queue-startup", "&b&lIt's your turn in the queue, starting up %server-owner%'s server!");
        hashMap.put("removed-from-queue", "&cYou've been removed from the startup queue!");
        hashMap.put("create-copying-files", "&e&oCopying %template-name% &e&oTemplate & Modifying settings...");
        hashMap.put("create-failed-copy", "&cFailed to copy server files for template: %template-name%");
        hashMap.put("create-missing-template", "&c%template-name% &cdoesn't exist!");
        hashMap.put("finish-delete-problem", "&c&lUh oh, something went wrong while deleting the server! It might have been deleted already.");
        hashMap.put("check-expire-times", "&e&oYour server will expire in %time-left%, on %expire-date%.");
        hashMap.put("check-expire-unlimited", "&e&oYou have unlimited server time!");
        hashMap.put("gamemode-changed", "&aDefault Gamemode set to &o%gamemode%!");
        hashMap.put("force-gamemode-on", "&aForce gamemode &2&oON||&e&oThis will take effect on next server restart!");
        hashMap.put("force-gamemode-off", "&aForce gamemode &c&oOFF||&e&oThis will take effect on next server restart!");
        hashMap.put("difficulty-changed", "&aServer difficulty set to &2&l&o%difficulty%!");
        hashMap.put("whitelist-enabled", "&eServer Whitelist: &aON");
        hashMap.put("whitelist-disabled", "&eServer Whitelist: &cOFF");
        hashMap.put("whitelist-add-timeout", "&c&oWhoops! &e&oTime ran out waiting for your input! Try again.");
        hashMap.put("whitelist-add-cancelled", "&c&oCancelled.");
        hashMap.put("whitelist-modify-instructions", "&aPlease type the player's name or UUID in chat &7&o(or 'cancel' to cancel):");
        hashMap.put("whitelist-cleared", "&c&lCleared the whitelist!");
        hashMap.put("whitelist-added", "&aAdded &o%player%&a to the whitelist.");
        hashMap.put("whitelist-removed", "&cRemoved &o%player%&c from the whitelist.");
        hashMap.put("monster-spawns-on", "&aMonster spawning set to &2&oTrue||&e&oThis will take effect on next server restart!");
        hashMap.put("monster-spawns-off", "&aMonster spawning set to &c&oFalse.||&e&oThis will take effect on next server restart!");
        hashMap.put("animal-spawns-on", "&aAnimal spawning set to &2&oTrue||&e&oThis will take effect on next server restart!");
        hashMap.put("animal-spawns-off", "&aAnimal spawning set to &c&oFalse.||&e&oThis will take effect on next server restart!");
        hashMap.put("npc-spawns-on", "&aVillager spawning set to &2&oTrue||&e&oThis will take effect on next server restart!");
        hashMap.put("npc-spawns-off", "&aVillager spawning set to &c&oFalse.||&e&oThis will take effect on next server restart!");
        hashMap.put("allow-nether-on", "&aAllow nether set to &a&oTrue||&e&oThis will take effect on next server restart!");
        hashMap.put("allow-nether-off", "&aAllow nether set to &c&oFalse||&e&oThis will take effect on next server restart!");
        hashMap.put("allow-flight-on", "&aAllow flight set to &a&oTrue||&e&oThis will take effect on next server restart!");
        hashMap.put("allow-flight-off", "&aAllow flight set to &c&oFalse||&e&oThis will take effect on next server restart!");
        hashMap.put("generate-structures-on", "&aGenerate structures set to &a&oTrue||&e&oThis will take effect on next server restart!");
        hashMap.put("generate-structures-off", "&aGenerate structures set to &c&oFalse||&e&oThis will take effect on next server restart!");
        hashMap.put("kicked-player", "&aYou have kicked &6&l%player% &afrom your server!");
        hashMap.put("got-kicked", "&e&oYou were kicked from this server by %player%");
        hashMap.put("unbanned-player", "&aYou have unbanned &6&l$%layer% &afrom your server!");
        hashMap.put("banned-player", "&aYou have banned &6&l$%layer% &afrom your server!");
        hashMap.put("got-banned", "&e&oYou were evicted! Reason: &c%reason% | &c&oEvicted by: &6%player%");
        hashMap.put("ban-message", "Eviction Reason: %reason% | Evicted by: %player%");
        hashMap.put("pvp-enabled", "&c&lPvP enabled in all worlds!");
        hashMap.put("pvp-disabled", "&a&lPvP disabled in all worlds!");
        hashMap.put("regain-info", "&c&lRelog to OP yourself, use &6&l/myserver regain if others are deOPing you.");
        hashMap.put("opped-player", "&a&lYou have promoted 6&l%player% &a&lto OP.");
        hashMap.put("deopped-player", "&a&lYou have promoted 6&l%player% &a&lto OP.");
        hashMap.put("must-be-online", "&cPlayer must be online to do that!");
        hashMap.put("leave-message", "&a'Til next time! &eSending you to '%server%'...");
        hashMap.put("motd-changed", "&e&o&lServer's MOTD changed to:||%motd%");
        hashMap.put("motd-display", "&e&o&lServer MOTD:||%motd%");
        hashMap.put("motd-too-long", "&cServer MOTDs are limited to 30 characters, not including color codes.");
        hashMap.put("help-ps-header", "&3&o%ps-command% (/ps) Help: &7&o<required> [optional]");
        hashMap.put("help-ps-join", "Join another player's server.");
        hashMap.put("help-ps-leave", "Leaves a player's server and sends you to the default server.");
        hashMap.put("help-ps-create", "Creates a server for you. Optionally specify the world/template to use.");
        hashMap.put("help-ps-home", "Starts and joins your server.");
        hashMap.put("help-ps-stop", "Stops your server.");
        hashMap.put("help-ps-delete", "Deletes your server.");
        hashMap.put("help-ps-motd", "View or set your server MOTD.");
        hashMap.put("help-ps-time", "Check how much time you have left on your server.");
        hashMap.put("help-ps-sharetime", "Share some of your expire time with another player.");
        hashMap.put("help-ps-worlds", "List the available setups.");
        hashMap.put("help-psa-header", "&b&oValid Commands for /playerserveradmin (/psa): &7&o<required> [optional]");
        hashMap.put("help-psa-create", "Creates a server for the player.");
        hashMap.put("help-psa-templates", "List the available templates");
        hashMap.put("help-psa-join", "Joins the specified player's server.");
        hashMap.put("help-psa-start", "Starts the specified player's server.");
        hashMap.put("help-psa-stop", "Stops the specified player's server.");
        hashMap.put("help-psa-delete", "Deletes the specified player's server. Add the 'confirm' argument to delete!");
        hashMap.put("help-psa-stopall", "Stops ALL currently running servers.");
        hashMap.put("help-psa-addtime", "Adds time to a player's server expiry time.");
        hashMap.put("help-psa-removetime", "Removes time from a player's server expiry time.");
        hashMap.put("help-psa-checktime", "Checks how much time a player has left.");
        hashMap.put("help-psa-maxmem", "Set the max memory for a player's server. Valid format ex: 512M, 1G, 1024M");
        hashMap.put("help-psa-startmem", "Set the max memory for a player's server. Valid format ex: 512M, 1G, 1024M");
        hashMap.put("help-psa-slots", "Set the max player slots for a player's server.");
        hashMap.put("help-psa-reload", "Reloads the bungee config.");
        hashMap.put("help-psa-motd", "View or set a player's server MOTD.");
        hashMap.put("help-mys-header", "&b&o/MyServer (/mys) Help:");
        hashMap.put("help-mys-settings", "Edit your server settings.");
        hashMap.put("help-mys-ban", "Ban or unban (evict) a player from your server.");
        hashMap.put("help-mys-kick", "Kick a player from your server.");
        hashMap.put("help-mys-whitelist", "Controls the whitelist of your server.");
        hashMap.put("help-mys-op", "OP or deOPs a player on your server.");
        hashMap.put("help-mys-regain", "Wipes OP list and regains control of your server");
        hashMap.put("help-mys-stop", "Stops your server. Restart with &6&l/ps home");
        hashMap.put("not-enough-time", "&cSorry, you do not have enough time to do this.");
        hashMap.put("no-share-unlimited", "&cSorry, you can't share unlimited server time!");
        hashMap.put("server-join-online-guest-title", "&a&o%server-owner%'s server online!||&eSending you now.");
        hashMap.put("server-join-offline-guest-title", "&c&oServer offline!||&eYou need to wait for the player to start it.");
        hashMap.put("server-join-online-owner-title", "&a&oServer online!||&eSending you there now.");
        hashMap.put("server-join-offline-owner-title", "&c&oServer offline!||&eStarting it up, this will just take a sec...");
        hashMap.put("server-stop-online-owner-title", "&e&oStopping your server");
        hashMap.put("server-stop-online-guest-title", "&cStopping %server-owner%'s server.");
        hashMap.put("server-stop-offline-owner-title", "&cYour server is not online!");
        hashMap.put("server-stop-offline-guest-title", "&c%server-owner%'s server is offline.");
        hashMap.put("other-server-not-online-title", "&c%server-owner%'s server offline!");
        hashMap.put("no-server-title", "&cYou do not have a server!");
        hashMap.put("other-no-server-title", "&cThis player does not have a server!");
        hashMap.put("server-expired-title", "&cSorry, server time expired!||&eYour server time has run out.");
        hashMap.put("max-memory-reached-title", "&cMemory limit reached||&eTry again later!");
        hashMap.put("max-servers-reached-title", "&cServer limit reached||&eTry again later!");
        hashMap.put("no-player-specified-title", "&cYou didn't specify a player!");
        hashMap.put("sent-fallback-title", "&aSent back to %fallback-server%!");
        hashMap.put("not-player-server-title", "&cYou aren't on a player server.");
        hashMap.put("template-doesnt-exist-title", "&cThe specified template does not exist.");
        hashMap.put("no-templates-found-title", "&cNo templates were found!");
        hashMap.put("create-start-title", "&b&oCreating your server!||&e&oThis may take a minute...");
        hashMap.put("have-server-title", "&aYou have a server!||&e&oStart and join it with &6&l/ps home");
        hashMap.put("delete-warning-title", "&c&lWARNING!||&c&l&k* &e&lREAD CHAT &c&l&k*");
        hashMap.put("recently-started-title", "&cPlease wait a minute.||&e&oYou recently started, stopped, or deleted your server.");
        hashMap.put("player-never-joined", "&cThis player has never joined the server.");
        hashMap.put("added-to-queue-title", "&aAdded to queue!||&eYour server will start when a slot is available.");
        hashMap.put("queue-startup-title", "&b&lYou are up next!||&eStarting up %server-owner%'s server.");
        hashMap.put("removed-from-queue-title", "&cRemoved from queue!||&eYou've been removed from the startup queue!");
        if (this.msgMap.containsKey("recently-started") && this.msgMap.get("recently-started").equalsIgnoreCase("&cYou recently started or stopped your server, please wait a minute.")) {
            this.msgMap.put("recently-started", "&cYou recently started, stopped, or deleted your server, please wait a minute.");
        }
        Iterator<Map.Entry<String, String>> iterator = this.msgMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String s = entry.getKey();
            String s2 = entry.getValue();
            if (s2.contains("%server-owners%")) {
                s2 = s2.replaceAll("%server-owners%", "%server-owner%");
                iterator.remove();
                hashMap.put(s, s2);
                this.utils.debug(s + ": fixed %server-owner% typo.");
            }
            if (s2.matches("(?i)(.*)(%days-changed%)(\\sday(s)?)?(.*)")) {
                s2 = s2.replaceAll("(?i)(%days-changed%)(\\sday(s)?)?", "%time-changed%");
                iterator.remove();
                hashMap.put(s, s2);
                this.utils.debug(s + ": updated existing %days-changed% placeholders to %time-changed%.");
            }
            if (s2.matches("(?i)(.*)(%days-left%)(\\sday(s)?)?(.*)")) {
                String replaceAll = s2.replaceAll("(?i)(%days-left%)(\\sday(s)?)?", "%time-left%");
                iterator.remove();
                hashMap.put(s, replaceAll);
                this.utils.debug(s + ": updated existing %days-left% placeholders to %time-left%.");
            }
        }
        boolean b = false;
        if (hashMap.size() > 0) {
            for (Map.Entry<String, String> entry2 : hashMap.entrySet()) {
                if (!this.msgMap.containsKey(entry2.getKey().toString())) {
                    this.msgMap.put(entry2.getKey(), entry2.getValue());
                    this.utils.debug(entry2.getKey() + ": saved changes.");
                    b = true;
                }
            }
        }
        if (b) {
        	this.messages.put("messages", this.msgMap);
            this.saveConfig(this.messages, "messages.yml");
            this.utils.debug("Saved updated messages.yml file.");
        }
    }
    
    public void loadServers() {
        File file = new File(this.getDataFolder() + File.separator + "servers");
        this.utils.debug("serverDir = " + file.toString());
        if (!file.exists()) {
            file.mkdir();
        }
        this.utils.debug("servers config class = " + this.serverStore.get("servers").getClass());
        this.serverManager.serverMap.clear();
        if (this.serverStore.get("servers") != null ) {
        	((HashMap<String,HashMap<String,HashMap<String,String>>>)this.serverStore.get("servers")).keySet().forEach(server -> {
            	this.serverManager.serverMap.put(server, new PlayerServer(server));
            });
        }
        this.utils.log(this.serverManager.serverMap.size() + " player servers saved.");
    }
    
    public void loadOnlineServers() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }

        File online = new File(this.getDataFolder(), "online.yml");
        if (!online.exists()) this.copyResource(online);
        
		try {
			InputStream inputStream = new FileInputStream(this.getDataFolder().getPath() + File.separator + "online.yml");
			this.online = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
        
        
		this.online.get("servers").keySet().forEach(s -> {
			if (this.serverManager.addedServers != null && !this.serverManager.addedServers.containsKey(s)) {
				this.serverManager.addVelocity(s.toString(), ((HashMap) this.online.get("servers").get(s)).get("address").toString(), Integer.valueOf(((HashMap) this.online.get("servers").get(s)).get("port").toString()), ((HashMap) this.online.get("servers").get(s)).get("motd").toString(), 1);
			}
		});
    }
    

	public void loadPermissions() {
        if (this.playerStore.get("permissions") == null) {
        	this.playerStore.put("permissions", this.permMap);
            this.saveConfig(this.playerStore, "players.yml");
        }
        else {
            this.permMap.clear();
            
            if (this.playerStore.get("permissions") instanceof HashMap) {
                this.permMap = (HashMap<String, HashMap<String, String>>)this.playerStore.get("permissions");
            }
        }
        HashMap<String, HashMap<String, String>> hashMap = new HashMap<String, HashMap<String, String>>();
        boolean b = false;
        for (Map.Entry<String, HashMap<String, String>> entry : this.permMap.entrySet()) {
            HashMap<String, String> permissions = new HashMap<String, String>();
            for (Map.Entry<String, String> perms : entry.getValue().entrySet()) {
                if (perms.getValue().equalsIgnoreCase("true")) {
                	permissions.put(perms.getKey(), perms.getValue());
                }
                else {
                    b = true;
                }
            }
            if (permissions.size() > 0) {
                hashMap.put(entry.getKey(), permissions);
            }
        }
        if (b && !this.permMap.equals(hashMap)) {
            this.permMap.clear();
            this.permMap.putAll(hashMap);
            this.permMapChanged = true;
        }
    }
    
    public void savePermissions() {
    	this.playerStore.put("permissions", this.permMap);
        this.proxy.getScheduler()
        .buildTask(this.pl, () -> {
        	saveConfig(this.playerStore, "players.yml");
        })
        .delay(2L, TimeUnit.SECONDS)
        .schedule();
    }
    
    public void loadPlayers() {
        if (this.onlineMode && this.playerStore.get("players") != null) {
            this.utils.log(Level.WARNING, "Removing all players from players.yml file, no need to store them anymore!");
            this.playerStore.put("players", null);
            saveConfig(this.playerStore, "players.yml");
        }
        if (!this.onlineMode) {
            this.playerMap.clear();
            if (this.playerStore.get("players") == null) {
            	this.playerStore.put("players", this.playerMap);
                this.savePlayers();
            }
            if (this.playerStore.get("players") != null && this.playerStore.get("players") instanceof TreeMap) {
                this.playerMap = (TreeMap<String, String>)this.playerStore.get("players");
            }
            else if ((this.playerStore.get("players") != null && this.playerStore.get("players") instanceof HashMap) || this.playerStore.get("players") instanceof LinkedHashMap) {
                this.playerMap.putAll((Map<? extends String, ? extends String>)this.playerStore.get("players"));
            }
            else {
                this.utils.log(Level.SEVERE, "Invalid players.yml formatting!");
            }
            this.utils.log(this.playerMap.size() + " saved players loaded.");
        }
    }
    
    public void putPlayer(String s, String s2) {
        this.playerMap.put(s, s2);
        this.playerMapChanged = true;
    }
    
    public void savePlayers() {
    	this.playerStore.put("players", this.playerMap);
        this.proxy.getScheduler()
        .buildTask(this.pl, () -> {
        	saveConfig(this.playerStore, "players.yml");
        	
        })
        .schedule();
    }
    
    public void copyResource(File file) {
    	if (!file.exists()) {
    		try {
                try (InputStream resourceAsStream = PlayerServers.class.getClassLoader().getResourceAsStream(file.getName());
                     FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    ByteStreams.copy(resourceAsStream, fileOutputStream);
                }
            }
            catch (IOException ex) {
            	ex.printStackTrace();
            }
    	}
    }

	public void setupScripts() {
    	//
    	// Stop being a skript kiddie and code your own plugin >:c
    	//
        File file = new File(this.getDataFolder(), "scripts");
        this.utils.debug("scriptsDir = " + file.toString());
        if (!file.exists()) {
            file.mkdir();
        }
        this.copyResource(new File(this.getDataFolder(), "scripts" + File.separator + "PSWrapper.jar"));
    }
    
    public File getDataFolder() {
		Path dataFolder = Path.of(this.dataDirectory + File.separator);
		return dataFolder.toFile();
	}

	public static PlayerServersAPI getApi() {
        return PlayerServers.api;
    }
    
    public PlayerServers getInstance() {
        return this;
    }

	public PluginDescription getDescription() {
		// TODO Auto-generated method stub
		return this.container.getDescription();
	}

	public Logger getLogger() {
		// TODO Auto-generated method stub
		return logger;
	}
}
