package net.cakemine.playerservers.bungee;

import net.md_5.bungee.api.*;
import net.cakemine.playerservers.bungee.commands.*;
import net.cakemine.playerservers.bungee.objects.PlayerServer;
import net.cakemine.playerservers.bungee.objects.StoredPlayer;
import net.md_5.bungee.api.connection.*;
import net.cakemine.playerservers.bungee.wrapper.*;
import net.md_5.bungee.config.*;
import net.cakemine.playerservers.bungee.sync.*;
import java.util.concurrent.*;
import net.md_5.bungee.api.plugin.*;
import java.util.logging.*;
import net.md_5.bungee.api.config.*;
import java.util.*;
import com.google.common.io.*;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;


public class PlayerServers extends Plugin {
    public ProxyServer proxy;
    public ServerManager serverManager;
    public ExpiryTracker expiryTracker;
    public SettingsManager settingsManager;
    public TemplateManager templateManager;
    public PlayerServerAdmin playerServerAdmin;
    public PluginSender sender;
    public PlayerServerCMD playerServer;
    protected static PlayerServersAPI api;
    public Utils utils;
    public ConfigurationProvider cfg;
    protected Configuration config;
    protected Configuration messages;
    protected Configuration guis;
    protected Configuration online;
    public HashMap<String, HashMap<String, String>> permMap;
    public List<String> blockedCmds;
    public List<String> alwaysOP;
    public HashMap<String, String> msgMap;
    public HashMap<UUID, StoredPlayer> playerMap;
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
    public int downloadsPort;
    public int wrapperPort;
    public int joinDelay;
    public int onlineJoinDelay;
    public int globalMaxServers;
    public int globalMaxRam;
    public HashMap<String, String> defaultMem;
    public String psCommand;
    public String vers;
    public String wrapper;
    public HashMap<ProxiedPlayer, Server> usingHelper;
    protected Controller ctrl;
	protected File guiConfig;
    
    public PlayerServers() {
        this.proxy = ProxyServer.getInstance();
        this.serverManager = new ServerManager(this);
        this.expiryTracker = new ExpiryTracker(this);
        this.settingsManager = new SettingsManager(this);
        this.templateManager = new TemplateManager(this);
        this.playerServerAdmin = new PlayerServerAdmin(this);
        this.sender = new PluginSender(this);
        this.playerServer = null;
        this.utils = new Utils(this);
        this.cfg = ConfigurationProvider.getProvider(YamlConfiguration.class);
        this.permMap = new HashMap<String, HashMap<String, String>>();
        this.msgMap = new HashMap<String, String>();
        this.playerMap = new HashMap<UUID, StoredPlayer>();
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
        this.usingHelper = new HashMap<ProxiedPlayer, Server>();
        this.ctrl = null;
    }
    
    public void onEnable() {
        vers = getDescription().getVersion();
        usingWindows = usingWindows();
        proxyAddress = utils.getProxyIp();
        proxy.registerChannel("playerservers:core");
        proxy.registerChannel("bungeecord:proxy");
        proxy.getPluginManager().registerListener(this, (Listener)new Listeners(this));
        proxy.getPluginManager().registerListener(this, (Listener)new PluginListener(this));
        PlayerServers.api = new PlayerServersAPI(this);
        reload();
        setupScripts();
        playerServer = new PlayerServerCMD(this, psCommand);
        proxy.getScheduler().schedule(this, new RepeatTasks(this), 30L, 30L, TimeUnit.SECONDS);
        proxy.getPluginManager().registerCommand(this, (Command)new PlayerServerCMD(this, psCommand));
        proxy.getPluginManager().registerCommand(this, (Command)new PlayerServerAdmin(this));
    }
    
    public void onDisable() {
        proxy.getScheduler().cancel(this);
        if (wrapper.equalsIgnoreCase("default")) {
            ctrl.disconnect();
        }
    }
    
    public boolean usingWindows() {
        return System.getProperty("os.name").matches("(?i)(.*)(windows)(.*)");
    }
    
    public void reload() {
        String psCommand = this.psCommand;
        msgMap.clear();
        serverManager.serverMap.clear();
        templateManager.templates.clear();
        loadFiles();
        updateConfig();
        loadConfig();
        vers = "565421"; // Don't modify this!
        loadMsgs();
        loadGUIs();
        loadServers();
        clearOnlineServers();
        templateManager.loadTemplates();
        utils.vCheck();
        sender.reSyncAll();
        if (!psCommand.equalsIgnoreCase(psCommand)) {
            proxy.getPluginManager().unregisterCommands(this);
            proxy.getPluginManager().registerCommand(this, new PlayerServerCMD(this, psCommand));
            proxy.getPluginManager().registerCommand(this, new PlayerServerAdmin(this));
        }
        if (ctrl == null && wrapper.equalsIgnoreCase("default")) {
            proxy.getScheduler().runAsync(this, (ctrl = new Controller(this)));
        }
        else if (ctrl == null && wrapper.equalsIgnoreCase("remote")) {
            (ctrl = new Controller(this)).setAddress(wrapperAddress);
            proxy.getScheduler().runAsync(this, ctrl);
        }
        onlineMode = proxy.getConfig().isOnlineMode();
    }
    
    public void loadFiles() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "config.yml");
        copyResource(file);
        try {
            config = cfg.load(file);
        }
        catch (IOException ex) {
            utils.log(Level.SEVERE, "Failed to load config.yml file! Please send this stack trace to the developer.");
            ex.printStackTrace();
        }
        File file3 = new File(getDataFolder(), "messages.yml");
        copyResource(file3);
        try {
            messages = cfg.load(file3);
        }
        catch (IOException ex3) {
            utils.log(Level.SEVERE, "Failed to load messages.yml file! Please send this stack trace to the developer.");
            ex3.printStackTrace();
        }
        File file4 = new File(getDataFolder(), "guis.yml");
        copyResource(file4);
        try {
            guis = cfg.load(file4);
            guiConfig = file4;
        }
        catch (IOException ex5) {
            utils.log(Level.SEVERE, "Failed to load GUIs.yml file! Please send this stack trace to the developer!");
        }
        
        File serverFolder = new File(getDataFolder() + File.separator + "data" + File.separator + "servers");
        utils.debug("serverDir = " + serverFolder.toString());
        if (!serverFolder.exists()) {
        	serverFolder.mkdirs();
        }
        
        File playersFolder = new File(getDataFolder() + File.separator + "data" + File.separator + "players");
        utils.debug("playerDir = " + playersFolder.toString());
        if (!playersFolder.exists()) {
        	playersFolder.mkdirs();
        }
        
        File server = new File(getDataFolder() + File.separator + "servers");
        utils.debug("serverDir = " + file.toString());
        if (!server.exists()) {
        	server.mkdir();
        }
    }
    
    public void updateConfig() {
    	boolean changed = false;
        if (config.getStringList("blocked-commands") == null || config.getStringList("blocked-commands").isEmpty()) {
            config.set("blocked-commands", new String[] { "^(/execute(.*)|/)(minecraft:)(ban(-ip)?|pardon(-ip)?|stop|reload)($|\\s.*)?" });
            utils.log("Added missing blocked-commands config option to the config.");
            changed = true;
        }
        if (config.getString("ps-custom-command") == null || config.getString("ps-custom-command").isEmpty()) {
            config.set("ps-custom-command", "/playerserver");
            utils.log("Added missing ps-custom-command config option to the config.");
            changed = true;
        }
        if (config.get("global-max-RAM") == null) {
            config.set("global-max-RAM", (-1));
            utils.log("Added missing global-max-RAM config option to the config.");
            changed = true;
        }
        if (config.get("global-max-servers") == null || config.getInt("global-max-servers") < -1) {
            config.set("global-max-servers", (-1));
            utils.log("Added missing global-max-servers config option to the config.");
            changed = true;
        }
        if (config.get("always-op") == null) {
            config.set("always-op", Arrays.asList("Notch", "069a79f4-44e9-4726-a5be-fca90e38aaf5"));
            utils.log("Added default always-op list to existing config.");
            changed = true;
        }
        if (config.get("reset-expiry-on-create") == null) {
            config.set("reset-expiry-on-create", false);
            utils.log("Added default reset-expiry-on-create to existing config.");
            changed = true;
        }
        if (config.get("use-titles") == null) {
            config.set("use-titles", true);
            utils.log("Added default use-titles to existing config.");
            changed = true;
        }
        if (config.get("purge-servers") == null) {
            config.set("purge-servers", false);
            utils.log("Added default purge-servers to existing config.");
            changed = true;
        }
        if (config.get("purge-after") == null) {
            config.set("purge-after", "30 days");
            utils.log("Added default purge-after to existing config.");
            changed = true;
        }
        if (config.get("purge-interval") == null) {
            config.set("purge-interval", "6 hours");
            utils.log("Added default purge-interval to existing config.");
            changed = true;
        }
        if (config.get("wrapper") == null) {
            config.set("wrapper", "screen");
            utils.log("Added screen as 'wrapper' to existing config.");
            changed = true;
        }
        if (config.get("wrapper-control-address") == null) {
            config.set("wrapper-control-address", "localhost");
            utils.log("Added default wrapper-control-address to existing config.");
            changed = true;
        }
        if (config.get("wrapper-control-port") == null) {
            config.set("wrapper-control-port", 5155);
            utils.log("Added default wrapper-control-port to existing config.");
            changed = true;
        }
        if (config.get("online-join-delay") == null) {
            config.set("online-join-delay", 3);
            utils.log("Added default online-join-delay to existing config.");
            saveConfig(config, "config.yml");
        }
        if (config.get("use-startup-queue") == null) {
            config.set("use-startup-queue", true);
            utils.log("Added default use-startup-queue to existing config.");
            changed = true;
        }
        if (changed) {
        	saveConfig(config, "config.yml");
        }
    }
    
    public void saveConfig(Configuration configuration, String s) {
        File file = new File(getDataFolder(), s);
        try {
            cfg.save(configuration, file);
        }
        catch (IOException ex) {
            utils.log(Level.SEVERE, "Failed to save the file " + file.getPath() + ", please send this stack trace to the developer.");
            ex.printStackTrace();
        }
    }
    
    public void loadConfig() {
        debug = config.getBoolean("debug", false);
        psCommand = config.getString("ps-custom-command", "playerserver");
        if (psCommand.startsWith("/")) {
            psCommand = psCommand.substring(1);
        }
        blockedCmds = (List<String>)config.getStringList("blocked-commands");
        useExpiry = config.getBoolean("use-expire-dates", true);
        utils.debug("use-expire-dates: " + useExpiry);
        prefix = config.getString("prefix", "&b&oPlayrSrv &7&o�&f");
        utils.debug("prefix: " + prefix);
        if (config.getString("hub-server") == null || config.getString("hub-server").equals("default")) {
            ConfigurationAdapter configurationAdapter = proxy.getConfigurationAdapter();
            String s = "default_server";
            fallbackSrv = configurationAdapter.getString(s, proxy.getServers().values().iterator().next().getName());
        }
        else {
            fallbackSrv = config.getString("hub-server");
        }
        utils.debug("hub-server: " + fallbackSrv);
        if (config.getString("servers-folder").equals("default")) {
            serversFolder = getDataFolder().getAbsolutePath() + File.separator + "servers";
        }
        else {
            serversFolder = config.getString("servers-folder");
        }
        utils.debug("servers-folder: " + serversFolder);
        downloadsPort = config.getInt("downloads-port", 8080);
        utils.debug("downloads-port: " + downloadsPort);
        joinDelay = config.getInt("startup-join-delay", 10);
        onlineJoinDelay = config.getInt("online-join-delay", 3);
        utils.debug("config max servers = " + config.get("global-max-servers") + " | class =" + config.get("global-max-servers").getClass());
        globalMaxServers = config.getInt("global-max-servers");
        utils.debug("global-max-servers = " + globalMaxServers);
        if (config.get("global-max-RAM") instanceof Integer) {
            globalMaxRam = config.getInt("global-max-RAM");
        }
        else {
            globalMaxRam = utils.memStringToInt(config.getString("global-max-RAM"));
        }
        utils.debug("global-max-RAM = " + globalMaxRam);
        alwaysOP = (List<String>)config.getStringList("always-op");
        resetExpiry = config.getBoolean("reset-expiry-on-create");
        utils.debug("reset-expiry-on-create = " + resetExpiry);
        useTitles = config.getBoolean("use-titles");
        utils.debug("use-titles = " + useTitles);
        autoPurge = config.getBoolean("purge-servers");
        utils.debug("purge-servers = " + autoPurge);
        autoPurgeTime = expiryTracker.stringToMillis(config.getString("purge-after"));
        utils.debug("purge-after in milliseconds = " + autoPurgeTime);
        autoPurgeInterval = expiryTracker.stringToMillis(config.getString("purge-interval"));
        utils.debug("purge-interval in milliseconds = " + autoPurgeInterval);
        wrapper = config.getString("wrapper");
        if (wrapper.matches("(?i)(scr(e*)n)")) {
            wrapper = "screen";
        }
        else if (wrapper.matches("(?i)(tm(u)?x)")) {
            wrapper = "tmux";
        }
        else if (wrapper.matches("(?i)(r(e)?m(o)?t(e)?)")) {
            wrapper = "remote";
        }
        else {
            wrapper = "default";
        }
        utils.debug("wrapper = " + wrapper);
        wrapperPort = config.getInt("wrapper-control-port");
        utils.debug("wrapper-control-port = " + wrapperPort);
        wrapperAddress = (wrapper == "default") ? "127.0.0.1" : config.getString("wrapper-control-address");
        utils.debug("wrapper-control-address = " + wrapperAddress);
        useQueue = config.getBoolean("use-startup-queue");
        utils.debug("use-startup-queue = " + useQueue);
    }
    
    public void loadGUIs() {
        updateGUIs();
        
        sender.guisSerialized = YamlToJson().toString();
    }
    
    private String YamlToJson() {
    	try (InputStream inputStream = new FileInputStream(guiConfig)) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(inputStream);
            ObjectMapper jsonMapper = new ObjectMapper();
            String jsonData = jsonMapper.writeValueAsString(yamlData);
            return jsonData;

        } catch (IOException e) {
            e.printStackTrace();
        }
    	return null;

    }
    
    public void updateGUIs() {
        boolean b = false;
        if (guis.get("settings-icons") instanceof HashMap && ((HashMap)guis.get("settings-icons")).get("expire-tracker") == null) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("item-id", "clock");
            hashMap.put("item-name", "&e&o&lTime Until Server Expires:");
            hashMap.put("item-lore", "Time Left: %time-left%||Expire Date: %expire-date%");
            utils.debug("updating GUI: settings with expire-tracker: " + hashMap.toString());
            ((HashMap)guis.get("settings-icons")).put("expire-tracker", hashMap);
            b = true;
        }
        if (guis.get("control-title") == null) {
            guis.set("control-title", "&5&lPS Control &0&l�");
            b = true;
        }
        if (guis.get("control-icons") == null) {
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
            guis.set("control-icons", hashMap2);
            b = true;
        }
        if (guis.get("servers-icons.server.item-lore").equals("Click to join this Player Server.")) {
            guis.set("servers-icons.server.item-lore", "Owner: %player%||Players: %current-players%/%max-players%||UUID: %player-uuid%||MOTD: %motd%||Template: %template-name%||Expire Date: %expire-date%||Time Left: %time-left%||Whitelist: %whitelist%");
            b = true;
        }
        if (b) {
            saveConfig(guis, "guis.yml");
            utils.debug("Saved updated guis.yml file.");
        }
    }
    
    public void loadMsgs() {
        utils.debug("messages class: " + messages.get("messages").getClass());
        msgMap.put("no-permissions",  messages.get("messages.no-permissions").toString());
        msgMap.put("no-server", messages.get("messages.no-server").toString());
        msgMap.put("other-no-server", messages.get("messages.other-no-server").toString());
        msgMap.put("not-player-server", messages.get("messages.not-player-server").toString());
        msgMap.put("no-player-specified", messages.get("messages.no-player-specified").toString());
        msgMap.put("no-value-specified", messages.get("messages.no-value-specified").toString());
        msgMap.put("only-player-use", messages.get("messages.only-player-use").toString());
        msgMap.put("have-server", messages.get("messages.have-server").toString());
        msgMap.put("other-have-server", messages.get("messages.other-have-server").toString());
        msgMap.put("sent-fallback", messages.get("messages.sent-fallback").toString());
        msgMap.put("blocked-cmd", messages.get("messages.blocked-cmd").toString());
        msgMap.put("recently-started", messages.get("messages.recently-started").toString());
        msgMap.put("server-expired", messages.get("messages.server-expired").toString());
        msgMap.put("create-start", messages.get("messages.create-start").toString());
        msgMap.put("create-finished", messages.get("messages.create-finished").toString());
        msgMap.put("create-copying-files", messages.get("messages.create-copying-files").toString());
        msgMap.put("create-failed-copy", messages.get("messages.create-failed-copy").toString());
        msgMap.put("create-missing-template", messages.get("messages.create-missing-template").toString());
        msgMap.put("expire-times", messages.get("messages.expire-times").toString());
        msgMap.put("others-expire-times", messages.get("messages.others-expire-times").toString());
        msgMap.put("check-expire-times", messages.get("messages.check-expire-times").toString());
        msgMap.put("check-expire-unlimited", messages.get("messages.check-expire-unlimited").toString());
        msgMap.put("days-left", messages.get("messages.days-left").toString());
        msgMap.put("not-enough-time", messages.get("messages.not-enough-time").toString());
        msgMap.put("no-share-unlimited", messages.get("messages.no-share-unlimited").toString());
        msgMap.put("other-days-left", messages.get("messages.other-days-left").toString());
        msgMap.put("other-removed-days", messages.get("messages.other-removed-days").toString());
        msgMap.put("other-added-days", messages.get("messages.other-added-days").toString());
        msgMap.put("invalid-time-unit", messages.get("messages.invalid-time-unit").toString());
        msgMap.put("delete-warning", messages.get("messages.delete-warning").toString());
        msgMap.put("start-delete", messages.get("messages.start-delete").toString());
        msgMap.put("finish-delete", messages.get("messages.finish-delete").toString());
        msgMap.put("finish-delete-problem", messages.get("messages.finish-delete-problem").toString());
        msgMap.put("other-server-already-online", messages.get("messages.other-server-already-online").toString());
        msgMap.put("server-already-online", messages.get("messages.server-already-online").toString());
        msgMap.put("stopping-all-servers", messages.get("messages.stopping-all-servers").toString());
        msgMap.put("server-join-online-owner", messages.get("messages.server-join-online-owner").toString());
        msgMap.put("server-join-online-guest", messages.get("messages.server-join-online-guest").toString());
        msgMap.put("server-join-offline-owner", messages.get("messages.server-join-offline-owner").toString());
        msgMap.put("server-join-offline-guest", messages.get("messages.server-join-offline-guest").toString());
        msgMap.put("server-stop-online-owner", messages.get("messages.server-stop-online-owner").toString());
        msgMap.put("server-stop-online-guest", messages.get("messages.server-stop-online-guest").toString());
        msgMap.put("server-stop-offline-owner", messages.get("messages.server-stop-offline-owner").toString());
        msgMap.put("server-stop-offline-guest", messages.get("messages.server-stop-offline-guest").toString());
        msgMap.put("other-server-not-online", messages.get("messages.other-server-not-online").toString());
        msgMap.put("other-started-server", messages.get("messages.other-started-server").toString());
        msgMap.put("other-stopping-server", messages.get("messages.other-stopping-server").toString());
        msgMap.put("got-kicked", messages.get("messages.got-kicked").toString());
        msgMap.put("got-banned", messages.get("messages.got-banned").toString());
        msgMap.put("config-reloaded", messages.get("messages.config-reloaded").toString());
        msgMap.put("max-memory-changed", messages.get("messages.max-memory-changed").toString());
        msgMap.put("start-memory-changed", messages.get("messages.start-memory-changed").toString());
        msgMap.put("max-players-count", messages.get("messages.max-players-count").toString());
        msgMap.put("invalid-memory-format", messages.get("messages.invalid-memory-format").toString());
        msgMap.put("invalid-slot-count", messages.get("messages.invalid-slot-count").toString());
        msgMap.put("start-greater-max", messages.get("messages.start-greater-max").toString());
        msgMap.put("max-lessthan-start", messages.get("messages.max-lessthan-start").toString());
        msgMap.put("player-never-joined", messages.get("messages.player-never-joined").toString());
        msgMap.put("no-template-specified", messages.get("messages.no-template-specified").toString());
        msgMap.put("template-doesnt-exist", messages.get("messages.template-doesnt-exist").toString());
        msgMap.put("no-templates-found", messages.get("messages.no-templates-found").toString());
        msgMap.put("available-templates", messages.get("messages.available-templates").toString());
        msgMap.put("no-template-permissions", messages.get("messages.no-template-permissions").toString());
        msgMap.put("max-memory-reached", messages.get("messages.max-memory-reached").toString());
        msgMap.put("max-servers-reached", messages.get("messages.max-servers-reached").toString());
        msgMap.put("added-to-queue", messages.get("messages.added-to-queue").toString());
        msgMap.put("removed-from-queue", messages.get("messages.removed-from-queue").toString());
        msgMap.put("queue-startup", messages.get("messages.queue-startup").toString());
        msgMap.put("gamemode-changed", messages.get("messages.gamemode-changed").toString());
        msgMap.put("force-gamemode-on", messages.get("messages.force-gamemode-on").toString());
        msgMap.put("force-gamemode-off", messages.get("messages.force-gamemode-off").toString());
        msgMap.put("difficulty-changed", messages.get("messages.difficulty-changed").toString());
        msgMap.put("whitelist-add-timeout", messages.get("messages.whitelist-add-timeout").toString());
        msgMap.put("whitelist-add-cancelled", messages.get("messages.whitelist-add-cancelled").toString());
        msgMap.put("whitelist-modify-instructions", messages.get("messages.whitelist-modify-instructions").toString());
        msgMap.put("whitelist-cleared", messages.get("messages.whitelist-cleared").toString());
        msgMap.put("whitelist-added", messages.get("messages.whitelist-added").toString());
        msgMap.put("whitelist-removed", messages.get("messages.whitelist-removed").toString());
        msgMap.put("whitelist-enabled", messages.get("messages.whitelist-enabled").toString());
        msgMap.put("whitelist-disabled", messages.get("messages.whitelist-disabled").toString());
        msgMap.put("monster-spawns-on", messages.get("messages.monster-spawns-on").toString());
        msgMap.put("monster-spawns-off", messages.get("messages.monster-spawns-off").toString());
        msgMap.put("animal-spawns-on", messages.get("messages.animal-spawns-on").toString());
        msgMap.put("animal-spawns-off", messages.get("messages.animal-spawns-off").toString());
        msgMap.put("npc-spawns-on", messages.get("messages.npc-spawns-on").toString());
        msgMap.put("npc-spawns-off", messages.get("messages.npc-spawns-off").toString());
        msgMap.put("allow-nether-on", messages.get("messages.allow-nether-on").toString());
        msgMap.put("allow-nether-off", messages.get("messages.allow-nether-off").toString());
        msgMap.put("allow-flight-on", messages.get("messages.allow-flight-on").toString());
        msgMap.put("allow-flight-off", messages.get("messages.allow-flight-off").toString());
        msgMap.put("generate-structures-on", messages.get("messages.generate-structures-on").toString());
        msgMap.put("generate-structures-off", messages.get("messages.generate-structures-off").toString());
        msgMap.put("kicked-player", messages.get("messages.kicked-player").toString());
        msgMap.put("got-kicked", messages.get("messages.got-kicked").toString());
        msgMap.put("banned-player", messages.get("messages.banned-player").toString());
        msgMap.put("unbanned-player", messages.get("messages.unbanned-player").toString());
        msgMap.put("got-banned", messages.get("messages.got-banned").toString());
        msgMap.put("ban-message", messages.get("messages.ban-message").toString());
        msgMap.put("pvp-enabled", messages.get("messages.pvp-enabled").toString());
        msgMap.put("pvp-disabled", messages.get("messages.pvp-disabled").toString());
        msgMap.put("regain-info", messages.get("messages.regain-info").toString());
        msgMap.put("opped-player", messages.get("messages.opped-player").toString());
        msgMap.put("deopped-player", messages.get("messages.deopped-player").toString());
        msgMap.put("must-be-online", messages.get("messages.must-be-online").toString());
        msgMap.put("leave-message", messages.get("messages.leave-message").toString());
        msgMap.put("motd-display", messages.get("messages.motd-display").toString());
        msgMap.put("motd-changed", messages.get("messages.motd-changed").toString());
        msgMap.put("motd-too-long", messages.get("messages.motd-too-long").toString());
        msgMap.put("server-join-online-guest-title", messages.get("messages.server-join-online-guest-title").toString());
        msgMap.put("server-join-offline-guest-title", messages.get("messages.server-join-offline-guest-title").toString());
        msgMap.put("server-join-online-owner-title", messages.get("messages.server-join-online-owner-title").toString());
        msgMap.put("server-join-offline-owner-title", messages.get("messages.server-join-offline-owner-title").toString());
        msgMap.put("server-stop-online-owner-title", messages.get("messages.server-stop-online-owner-title").toString());
        msgMap.put("server-stop-online-guest-title", messages.get("messages.server-stop-online-guest-title").toString());
        msgMap.put("server-stop-offline-owner-title", messages.get("messages.server-stop-offline-owner-title").toString());
        msgMap.put("server-stop-offline-guest-title", messages.get("messages.server-stop-offline-guest-title").toString());
        msgMap.put("other-server-not-online-title", messages.get("messages.other-server-not-online-title").toString());
        msgMap.put("no-server-title", messages.get("messages.no-server-title").toString());
        msgMap.put("other-no-server-title", messages.get("messages.other-no-server-title").toString());
        msgMap.put("server-expired-title", messages.get("messages.server-expired-title").toString());
        msgMap.put("max-memory-reached-title", messages.get("messages.max-memory-reached-title").toString());
        msgMap.put("max-servers-reached-title", messages.get("messages.max-servers-reached-title").toString());
        msgMap.put("max-players-count", messages.get("messages.max-players-count").toString());
        msgMap.put("no-player-specified-title", messages.get("messages.no-player-specified-title").toString());
        msgMap.put("sent-fallback-title", messages.get("messages.sent-fallback-title").toString());
        msgMap.put("not-player-server-title", messages.get("messages.not-player-server-title").toString());
        msgMap.put("template-doesnt-exist-title", messages.get("messages.template-doesnt-exist-title").toString());
        msgMap.put("no-templates-found-title", messages.get("messages.no-templates-found-title").toString());
        msgMap.put("create-start-title", messages.get("messages.create-start-title").toString());
        msgMap.put("have-server-title", messages.get("messages.have-server-title").toString());
        msgMap.put("delete-warning-title", messages.get("messages.delete-warning-title").toString());
        msgMap.put("recently-started-title", messages.get("messages.recently-started-title").toString());
        msgMap.put("added-to-queue-title", messages.get("messages.added-to-queue-title").toString());
        msgMap.put("removed-from-queue-title", messages.get("messages.removed-from-queue-title").toString());
        msgMap.put("queue-startup-title", messages.get("messages.queue-startup-title").toString());
        msgMap.put("help-ps-header", messages.get("messages.help-ps-header").toString());
        msgMap.put("help-ps-join", messages.get("messages.help-ps-join").toString());
        msgMap.put("help-ps-leave", messages.get("messages.help-ps-leave").toString());
        msgMap.put("help-ps-create", messages.get("messages.help-ps-create").toString());
        msgMap.put("help-ps-home", messages.get("messages.help-ps-home").toString());
        msgMap.put("help-ps-stop", messages.get("messages.help-ps-stop").toString());
        msgMap.put("help-ps-delete", messages.get("messages.help-ps-delete").toString());
        msgMap.put("help-ps-motd", messages.get("messages.help-ps-motd").toString());
        msgMap.put("help-ps-time", messages.get("messages.help-ps-time").toString());
        msgMap.put("help-ps-worlds", messages.get("messages.help-ps-worlds").toString());
        msgMap.put("help-ps-sharetime", messages.get("messages.help-ps-sharetime").toString());
        msgMap.put("help-psa-header", messages.get("messages.help-psa-header").toString());
        msgMap.put("help-psa-create", messages.get("messages.help-psa-create").toString());
        msgMap.put("help-psa-templates", messages.get("messages.help-psa-templates").toString());
        msgMap.put("help-psa-join", messages.get("messages.help-psa-join").toString());
        msgMap.put("help-psa-start", messages.get("messages.help-psa-start").toString());
        msgMap.put("help-psa-stop", messages.get("messages.help-psa-stop").toString());
        msgMap.put("help-psa-delete", messages.get("messages.help-psa-delete").toString());
        msgMap.put("help-psa-stopall", messages.get("messages.help-psa-stopall").toString());
        msgMap.put("help-psa-kill", messages.get("messages.help-psa-kill").toString());
        msgMap.put("help-psa-addtime", messages.get("messages.help-psa-addtime").toString());
        msgMap.put("help-psa-removetime", messages.get("messages.help-psa-removetime").toString());
        msgMap.put("help-psa-checktime", messages.get("messages.help-psa-checktime").toString());
        msgMap.put("help-psa-maxmem", messages.get("messages.help-psa-maxmem").toString());
        msgMap.put("help-psa-startmem", messages.get("messages.help-psa-startmem").toString());
        msgMap.put("help-psa-slots", messages.get("messages.help-psa-slots").toString());
        msgMap.put("help-psa-reload", messages.get("messages.help-psa-reload").toString());
        msgMap.put("help-psa-motd", messages.get("messages.help-psa-motd").toString());
        msgMap.put("help-mys-header", messages.get("messages.help-mys-header").toString());
        msgMap.put("help-mys-settings", messages.get("messages.help-mys-settings").toString());
        msgMap.put("help-mys-ban", messages.get("messages.help-mys-ban").toString());
        msgMap.put("help-mys-kick", messages.get("messages.help-mys-kick").toString());
        msgMap.put("help-mys-whitelist", messages.get("messages.help-mys-whitelist").toString());
        msgMap.put("help-mys-op", messages.get("messages.help-mys-op").toString());
        msgMap.put("help-mys-regain", messages.get("messages.help-mys-regain").toString());
        msgMap.put("help-mys-stop",  messages.get("messages.help-mys-stop").toString());
        updateMsgs();
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
        hashMap.put("not-enough-time", "&cSorry, you do not have enough time to do ");
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
        if (msgMap.containsKey("recently-started") && msgMap.get("recently-started").equalsIgnoreCase("&cYou recently started or stopped your server, please wait a minute.")) {
            msgMap.put("recently-started", "&cYou recently started, stopped, or deleted your server, please wait a minute.");
        }
        Iterator<Map.Entry<String, String>> iterator = msgMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String s = entry.getKey();
            String s2 = entry.getValue();
            if (s2.contains("%server-owners%")) {
                s2 = s2.replaceAll("%server-owners%", "%server-owner%");
                iterator.remove();
                hashMap.put(s, s2);
                utils.debug(s + ": fixed %server-owner% typo.");
            }
            if (s2.matches("(?i)(.*)(%days-changed%)(\\sday(s)?)?(.*)")) {
                s2 = s2.replaceAll("(?i)(%days-changed%)(\\sday(s)?)?", "%time-changed%");
                iterator.remove();
                hashMap.put(s, s2);
                utils.debug(s + ": updated existing %days-changed% placeholders to %time-changed%.");
            }
            if (s2.matches("(?i)(.*)(%days-left%)(\\sday(s)?)?(.*)")) {
                String replaceAll = s2.replaceAll("(?i)(%days-left%)(\\sday(s)?)?", "%time-left%");
                iterator.remove();
                hashMap.put(s, replaceAll);
                utils.debug(s + ": updated existing %days-left% placeholders to %time-left%.");
            }
        }
        boolean b = false;
        if (hashMap.size() > 0) {
            for (Map.Entry<String, String> entry2 : hashMap.entrySet()) {
                if (!msgMap.containsKey(entry2.getKey().toString())) {
                    msgMap.put(entry2.getKey(), entry2.getValue());
                    utils.debug(entry2.getKey() + ": saved changes.");
                    b = true;
                }
            }
        }
        if (b) {
            messages.set("messages", msgMap);
            saveConfig(messages, "messages.yml");
            utils.debug("Saved updated messages.yml file.");
        }
    }
    
    public void loadServers() {
        File serverFolder = new File(getDataFolder() + File.separator + "data" + File.separator + "servers");
        utils.debug("serverDir = " + serverFolder.toString());
        if (!serverFolder.exists()) {
        	serverFolder.mkdirs();
        }
        if (serverFolder.listFiles() != null) {
        	for (File serverFile : serverFolder.listFiles()) {
    			try {
    				Configuration data = cfg.load(serverFile);
    				if (data.getKeys() != null ) {
    	        		data.getKeys().forEach(server -> {
    	                	serverManager.serverMap.put(server, new PlayerServer(UUID.fromString(server), this));
    	                });
    	            }
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
            }
        }
        utils.log(serverManager.serverMap.size() + " player servers saved.");
    }
    
    public void clearOnlineServers() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "online.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
                try (InputStream resourceAsStream = getResourceAsStream("online.yml");
                     FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    ByteStreams.copy(resourceAsStream, (OutputStream)fileOutputStream);
                }
            }
            catch (IOException ex) {
                throw new RuntimeException("Unable to create online servers file!", ex);
            }
        }
        try {
            online = cfg.load(file);
            
            online.set("servers", new HashMap<>());
            cfg.save(online, file);
        }
        catch (IOException ex2) {
            ex2.printStackTrace();
        }
    }
    
    public void loadPlayer(UUID s, StoredPlayer s2) {
        playerMap.put(s, s2);
        playerMapChanged = true;
    }
    
    public void copyResource(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
                try {
                    ByteStreams.copy(getResourceAsStream(file.getName()), (OutputStream)new FileOutputStream(file));
                }
                catch (IOException ex) {
                    utils.log(Level.SEVERE, "Failed to copy the resource file " + file.getPath() + "! Please send this stack trace to the developer.");
                    ex.printStackTrace();
                }
            }
            catch (IOException ex2) {
                utils.log(Level.SEVERE, "Failed to create the resource file " + file.getPath() + "! Please send this stack trace to the developer.");
                ex2.printStackTrace();
            }
        }
    }
    
    public void setupScripts() {
    	//
    	// Stop being a skript kiddie and code your own plugin >:c
    	//
        File file = new File(getDataFolder() + File.separator + "scripts");
        utils.debug("scriptsDir = " + file.toString());
        if (!file.exists()) {
            file.mkdir();
        }
        copyResource(new File(getDataFolder(), "scripts" + File.separator + "PSWrapper.jar"));
    }
    
    public static PlayerServersAPI getApi() {
        return PlayerServers.api;
    }
    
    public PlayerServers getInstance() {
        return this;
    }
}
