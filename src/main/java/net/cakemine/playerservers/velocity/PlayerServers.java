package net.cakemine.playerservers.velocity;

import net.cakemine.playerservers.velocity.objects.StoredPlayer;
import net.cakemine.playerservers.velocity.commands.PlayerServerAdmin;
import net.cakemine.playerservers.velocity.commands.PlayerServerCMD;
import net.cakemine.playerservers.velocity.wrapper.*;
import net.cakemine.playerservers.velocity.sync.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class PlayerServers {

    public ProxyServer proxy;
    private PlayerServers instance;
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
    protected HashMap<String, String> messages;
    protected HashMap<String, Object> guis;
    protected HashMap<String, HashMap<String, Object>> online;
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
    public int wrapperPort;
    public int joinDelay;
    public int onlineJoinDelay;
    public int globalMaxServers;
    public int globalMaxRam;

    private RepeatTasks repeatTask;
    public HashMap<String, String> defaultMem;
    public String psCommand;
    public String version;
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
        this.instance = this;
        this.eventManager = eventManager;
        logger = proxyLogger;
        this.dataDirectory = dataDirectory;
        this.serverManager = new ServerManager(this);
        this.expiryTracker = new ExpiryTracker(this);
        this.settingsManager = new SettingsManager(this);
        this.templateManager = new TemplateManager(this);
        this.playerServerAdmin = new PlayerServerAdmin(this);
        this.sender = new PluginSender(this);
        this.utils = new Utils(this);

        // Initialize collections
        this.permMap = new HashMap<>();
        this.msgMap = new HashMap<>();
        this.playerMap = new HashMap<>();
        this.defaultMem = new HashMap<>();
        this.usingHelper = new HashMap<>();
        
        // Set default values
        this.debug = false;
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
        this.psCommand = "playerserver";
        this.version = "-1";
        this.wrapper = "default";
        this.ctrl = null;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        initializeYaml();
        this.version = container.getDescription().getVersion().get();
        this.usingWindows = usingWindows();
        this.proxyAddress = this.utils.getProxyIp();
        
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("playerservers:core"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("bungeecord:proxy"));
        
        eventManager.register(this, new Listeners(this));
        eventManager.register(this, new PluginListener(this));

        PlayerServers.api = new PlayerServersAPI(this);

        reload();
        setupScripts();

        this.playerServer = new PlayerServerCMD(this, this.psCommand);
        repeatTask = new RepeatTasks(this);

        proxy.getScheduler().buildTask(this, () -> repeatTask.run())
                .delay(30L, TimeUnit.SECONDS)
                .repeat(30L, TimeUnit.SECONDS)
                .schedule();

        proxy.getCommandManager().register(this.psCommand, new PlayerServerCMD(this, this.psCommand), "pserver", "psrv", "ps");
        proxy.getCommandManager().register("playerserveradmin", new PlayerServerAdmin(this), "pserveradmin", "psrvadmin", "psa");

    }

    public void onDisable() {
        if ("default".equalsIgnoreCase(wrapper)) {
            ctrl.disconnect();
        }
        proxy.getScheduler().tasksByPlugin(instance).forEach(ScheduledTask::cancel);
    }

    private void initializeYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        this.yaml = new Yaml(options);
    }

    public boolean usingWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public void reload() {
        String previousPsCommand = this.psCommand;
        resetData();
        loadFiles();
        updateConfig();
        loadConfig();
        clearOnlineServers();
        this.version = "565421"; // Don't modify this!
        loadMsgs();
        loadGUIs();
        templateManager.loadTemplates();
        utils.vCheck();
        sender.reSyncAll();
        
        if (!this.psCommand.equalsIgnoreCase(previousPsCommand)) {
            proxy.getCommandManager().unregister(previousPsCommand);
            proxy.getCommandManager().unregister("playerserveradmin");
            proxy.getCommandManager().register(this.psCommand, new PlayerServerCMD(this, this.psCommand), "pserver", "psrv", "ps");
            proxy.getCommandManager().register("playerserveradmin", new PlayerServerAdmin(this), "pserveradmin", "psrvadmin", "psa");
        }

        handleControllerConnection();
        this.onlineMode = proxy.getConfiguration().isOnlineMode();
    }

    private void resetData() {
        msgMap.clear();
        serverManager.serverMap.clear();
        templateManager.templates.clear();
    }

    private void handleControllerConnection() {
        if (ctrl == null) {
            proxy.getScheduler().buildTask(instance, () -> {
                ctrl = new Controller(this);
                ctrl.setAddress(wrapperAddress);
                ctrl.connect();
            }).delay(3L, TimeUnit.SECONDS).schedule();
        }
    }

    public void loadFiles() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        
        config = (HashMap<String, Object>) loadFile("config.yml");
        messages = (HashMap<String, String>) (loadFile("messages.yml").get("messages") != null ? loadFile("messages.yml").get("messages") : loadFile("messages.yml"));
        guis = (HashMap<String, Object>) loadFile("guis.yml");

        createDirectory("data/servers");
        createDirectory("data/players");
        createDirectory("servers");

        loadYamlFile("config.yml", config);
        loadYamlFile("messages.yml", messages);
        loadYamlFile("guis.yml", guis);
    }

    private HashMap<String, ?> loadFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            copyResource(file);
        }
        InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			HashMap<String, Object> data = yaml.load(inputStream);

			return data;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		utils.log(Level.SEVERE, "Failed to load " + fileName);
        return null;
    }
    
    private HashMap<String, HashMap<String, String>> loadFile2(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            copyResource(file);
        }
        InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			HashMap<String, HashMap<String, String>> data = yaml.load(inputStream);

			return data;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    private void createDirectory(String path) {
        File dir = new File(getDataFolder(), path.replace("/", File.separator));
        utils.debug("Directory = " + dir.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void loadYamlFile(String fileName, HashMap<String, ?> targetMap) {
        try (InputStream inputStream = new FileInputStream(getDataFolder().getPath() + File.separator + fileName)) {
            targetMap = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadMsgYamlFile(String fileName, HashMap<String, HashMap<String, String>> targetMap) {
        try (InputStream inputStream = new FileInputStream(getDataFolder().getPath() + File.separator + fileName)) {
            targetMap = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateConfig() {
        boolean changed = false;

        changed |= updateConfigOption("blocked-commands", Arrays.asList("^(/execute(.*)|/)(minecraft:)(ban(-ip)?|pardon(-ip)?|stop|reload)($|\\s.*)?"));
        changed |= updateConfigOption("ps-custom-command", "/playerserver");
        changed |= updateConfigOption("global-max-RAM", -1);
        changed |= updateConfigOption("global-max-servers", -1);
        changed |= updateConfigOption("always-op", Arrays.asList("Notch", "069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        changed |= updateConfigOption("reset-expiry-on-create", false);
        changed |= updateConfigOption("use-titles", true);
        changed |= updateConfigOption("purge-servers", false);
        changed |= updateConfigOption("purge-after", "30 days");
        changed |= updateConfigOption("purge-interval", "6 hours");
        changed |= updateConfigOption("wrapper", "default");
        changed |= updateConfigOption("wrapper-control-address", "localhost");
        changed |= updateConfigOption("wrapper-control-port", 5155);
        changed |= updateConfigOption("online-join-delay", 3);
        changed |= updateConfigOption("use-startup-queue", true);
        changed |= updateConfigOption("debug", false);

        if (changed) {
            saveConfig(config, "config.yml");
        }
    }

    private boolean updateConfigOption(String key, Object defaultValue) {
    	if (config == null)
        if (!config.containsKey(key)) {
            config.put(key, defaultValue);
            utils.log("Added missing " + key + " config option to the config.");
            return true;
        }
        return false;
    }

    public void saveConfig(Object config, String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        debug = (boolean) config.get("debug");
        psCommand = config.get("ps-custom-command").toString();
        if (psCommand.startsWith("/")) {
            psCommand = psCommand.substring(1);
        }
        blockedCmds = (List<String>) config.get("blocked-commands");
        useExpiry = (boolean) config.get("use-expire-dates");
        prefix = config.get("prefix").toString();
        
        fallbackSrv = resolveFallbackServer();
        serversFolder = resolveServersFolder();

        joinDelay = (int) config.get("startup-join-delay");
        onlineJoinDelay = (int) config.get("online-join-delay");
        globalMaxServers = (int) config.get("global-max-servers");
        globalMaxRam = (int) config.get("global-max-RAM");
        alwaysOP = (List<String>) config.get("always-op");
        resetExpiry = (boolean) config.get("reset-expiry-on-create");
        useTitles = (boolean) config.get("use-titles");
        autoPurge = (boolean) config.get("purge-servers");
        autoPurgeTime = expiryTracker.stringToMillis(config.get("purge-after").toString());
        autoPurgeInterval = expiryTracker.stringToMillis(config.get("purge-check-interval").toString());
        wrapper = resolveWrapper(config.get("wrapper").toString());
        wrapperPort = (int) config.get("wrapper-control-port");
        wrapperAddress = (wrapper.equals("default")) ? "127.0.0.1" : config.get("wrapper-control-address").toString();
        useQueue = (boolean) config.get("use-startup-queue");
    }

    private String resolveFallbackServer() {
        if (config.get("hub-server") == null || config.get("hub-server").equals("default")) {
            return proxy.getConfiguration().getAttemptConnectionOrder().get(0);
        } else {
            return config.get("hub-server").toString();
        }
    }

    private String resolveServersFolder() {
        if (config.get("servers-folder").equals("default")) {
            return getDataFolder().getAbsolutePath() + File.separator + "servers";
        } else {
            return config.get("servers-folder").toString();
        }
    }

    private String resolveWrapper(String wrapperValue) {
        if (wrapperValue.matches("(?i)(scr(e*)n)")) {
            return "screen";
        } else if (wrapperValue.matches("(?i)(tm(u)?x)")) {
            return "tmux";
        } else if (wrapperValue.matches("(?i)(r(e)?m(o)?t(e)?)")) {
            return "remote";
        } else {
            return "default";
        }
    }

    public void loadGUIs() {
        updateGUIs();
        try {
            InputStream inputStream = new FileInputStream(getDataFolder().getPath() + File.separator + "guis.yml");
            Map<String, Object> yamlData = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
            ObjectMapper jsonMapper = new ObjectMapper();
            sender.guisSerialized = jsonMapper.writeValueAsString(yamlData);
        } catch (FileNotFoundException | JsonProcessingException | UnsupportedEncodingException e) {
            sender.guisSerialized = null;
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
            saveConfig(guis, "guis.yml");
            utils.debug("Saved updated guis.yml file.");
        }
    }

    public void loadMsgs() {
    	this.utils.debug("messages class: " + this.messages.getClass());
        this.msgMap.put("no-permissions",  this.messages.get("no-permissions").toString());
        this.msgMap.put("no-server", this.messages.get("no-server").toString());
        this.msgMap.put("other-no-server", this.messages.get("other-no-server").toString());
        this.msgMap.put("not-player-server", this.messages.get("not-player-server").toString());
        this.msgMap.put("no-player-specified", this.messages.get("no-player-specified").toString());
        this.msgMap.put("no-value-specified", this.messages.get("no-value-specified").toString());
        this.msgMap.put("only-player-use", this.messages.get("only-player-use").toString());
        this.msgMap.put("have-server", this.messages.get("have-server").toString());
        this.msgMap.put("other-have-server", this.messages.get("other-have-server").toString());
        this.msgMap.put("sent-fallback", this.messages.get("sent-fallback").toString());
        this.msgMap.put("blocked-cmd", this.messages.get("blocked-cmd").toString());
        this.msgMap.put("recently-started", this.messages.get("recently-started").toString());
        this.msgMap.put("server-expired", this.messages.get("server-expired").toString());
        this.msgMap.put("create-start", this.messages.get("create-start").toString());
        this.msgMap.put("create-finished", this.messages.get("create-finished").toString());
        this.msgMap.put("create-copying-files", this.messages.get("create-copying-files").toString());
        this.msgMap.put("create-failed-copy", this.messages.get("create-failed-copy").toString());
        this.msgMap.put("create-missing-template", this.messages.get("create-missing-template").toString());
        this.msgMap.put("expire-times", this.messages.get("expire-times").toString());
        this.msgMap.put("others-expire-times", this.messages.get("others-expire-times").toString());
        this.msgMap.put("check-expire-times", this.messages.get("check-expire-times").toString());
        this.msgMap.put("check-expire-unlimited", this.messages.get("check-expire-unlimited").toString());
        this.msgMap.put("days-left", this.messages.get("days-left").toString());
        this.msgMap.put("not-enough-time", this.messages.get("not-enough-time").toString());
        this.msgMap.put("no-share-unlimited", this.messages.get("no-share-unlimited").toString());
        this.msgMap.put("other-days-left", this.messages.get("other-days-left").toString());
        this.msgMap.put("other-removed-days", this.messages.get("other-removed-days").toString());
        this.msgMap.put("other-added-days", this.messages.get("other-added-days").toString());
        this.msgMap.put("invalid-time-unit", this.messages.get("invalid-time-unit").toString());
        this.msgMap.put("delete-warning", this.messages.get("delete-warning").toString());
        this.msgMap.put("start-delete", this.messages.get("start-delete").toString());
        this.msgMap.put("finish-delete", this.messages.get("finish-delete").toString());
        this.msgMap.put("finish-delete-problem", this.messages.get("finish-delete-problem").toString());
        this.msgMap.put("other-server-already-online", this.messages.get("other-server-already-online").toString());
        this.msgMap.put("server-already-online", this.messages.get("server-already-online").toString());
        this.msgMap.put("stopping-all-servers", this.messages.get("stopping-all-servers").toString());
        this.msgMap.put("server-join-online-owner", this.messages.get("server-join-online-owner").toString());
        this.msgMap.put("server-join-online-guest", this.messages.get("server-join-online-guest").toString());
        this.msgMap.put("server-join-offline-owner", this.messages.get("server-join-offline-owner").toString());
        this.msgMap.put("server-join-offline-guest", this.messages.get("server-join-offline-guest").toString());
        this.msgMap.put("server-stop-online-owner", this.messages.get("server-stop-online-owner").toString());
        this.msgMap.put("server-stop-online-guest", this.messages.get("server-stop-online-guest").toString());
        this.msgMap.put("server-stop-offline-owner", this.messages.get("server-stop-offline-owner").toString());
        this.msgMap.put("server-stop-offline-guest", this.messages.get("server-stop-offline-guest").toString());
        this.msgMap.put("other-server-not-online", this.messages.get("other-server-not-online").toString());
        this.msgMap.put("other-started-server", this.messages.get("other-started-server").toString());
        this.msgMap.put("other-stopping-server", this.messages.get("other-stopping-server").toString());
        this.msgMap.put("got-kicked", this.messages.get("got-kicked").toString());
        this.msgMap.put("got-banned", this.messages.get("got-banned").toString());
        this.msgMap.put("config-reloaded", this.messages.get("config-reloaded").toString());
        this.msgMap.put("max-memory-changed", this.messages.get("max-memory-changed").toString());
        this.msgMap.put("start-memory-changed", this.messages.get("start-memory-changed").toString());
        this.msgMap.put("max-players-count", this.messages.get("max-players-count").toString());
        this.msgMap.put("invalid-memory-format", this.messages.get("invalid-memory-format").toString());
        this.msgMap.put("invalid-slot-count", this.messages.get("invalid-slot-count").toString());
        this.msgMap.put("start-greater-max", this.messages.get("start-greater-max").toString());
        this.msgMap.put("max-lessthan-start", this.messages.get("max-lessthan-start").toString());
        this.msgMap.put("player-never-joined", this.messages.get("player-never-joined").toString());
        this.msgMap.put("no-template-specified", this.messages.get("no-template-specified").toString());
        this.msgMap.put("template-doesnt-exist", this.messages.get("template-doesnt-exist").toString());
        this.msgMap.put("no-templates-found", this.messages.get("no-templates-found").toString());
        this.msgMap.put("available-templates", this.messages.get("available-templates").toString());
        this.msgMap.put("no-template-permissions", this.messages.get("no-template-permissions").toString());
        this.msgMap.put("max-memory-reached", this.messages.get("max-memory-reached").toString());
        this.msgMap.put("max-servers-reached", this.messages.get("max-servers-reached").toString());
        this.msgMap.put("added-to-queue", this.messages.get("added-to-queue").toString());
        this.msgMap.put("removed-from-queue", this.messages.get("removed-from-queue").toString());
        this.msgMap.put("queue-startup", this.messages.get("queue-startup").toString());
        this.msgMap.put("gamemode-changed", this.messages.get("gamemode-changed").toString());
        this.msgMap.put("force-gamemode-on", this.messages.get("force-gamemode-on").toString());
        this.msgMap.put("force-gamemode-off", this.messages.get("force-gamemode-off").toString());
        this.msgMap.put("difficulty-changed", this.messages.get("difficulty-changed").toString());
        this.msgMap.put("whitelist-add-timeout", this.messages.get("whitelist-add-timeout").toString());
        this.msgMap.put("whitelist-add-cancelled", this.messages.get("whitelist-add-cancelled").toString());
        this.msgMap.put("whitelist-modify-instructions", this.messages.get("whitelist-modify-instructions").toString());
        this.msgMap.put("whitelist-cleared", this.messages.get("whitelist-cleared").toString());
        this.msgMap.put("whitelist-added", this.messages.get("whitelist-added").toString());
        this.msgMap.put("whitelist-removed", this.messages.get("whitelist-removed").toString());
        this.msgMap.put("whitelist-enabled", this.messages.get("whitelist-enabled").toString());
        this.msgMap.put("whitelist-disabled", this.messages.get("whitelist-disabled").toString());
        this.msgMap.put("monster-spawns-on", this.messages.get("monster-spawns-on").toString());
        this.msgMap.put("monster-spawns-off", this.messages.get("monster-spawns-off").toString());
        this.msgMap.put("animal-spawns-on", this.messages.get("animal-spawns-on").toString());
        this.msgMap.put("animal-spawns-off", this.messages.get("animal-spawns-off").toString());
        this.msgMap.put("npc-spawns-on", this.messages.get("npc-spawns-on").toString());
        this.msgMap.put("npc-spawns-off", this.messages.get("npc-spawns-off").toString());
        this.msgMap.put("allow-nether-on", this.messages.get("allow-nether-on").toString());
        this.msgMap.put("allow-nether-off", this.messages.get("allow-nether-off").toString());
        this.msgMap.put("allow-flight-on", this.messages.get("allow-flight-on").toString());
        this.msgMap.put("allow-flight-off", this.messages.get("allow-flight-off").toString());
        this.msgMap.put("generate-structures-on", this.messages.get("generate-structures-on").toString());
        this.msgMap.put("generate-structures-off", this.messages.get("generate-structures-off").toString());
        this.msgMap.put("kicked-player", this.messages.get("kicked-player").toString());
        this.msgMap.put("got-kicked", this.messages.get("got-kicked").toString());
        this.msgMap.put("banned-player", this.messages.get("banned-player").toString());
        this.msgMap.put("unbanned-player", this.messages.get("unbanned-player").toString());
        this.msgMap.put("got-banned", this.messages.get("got-banned").toString());
        this.msgMap.put("ban-message", this.messages.get("ban-message").toString());
        this.msgMap.put("pvp-enabled", this.messages.get("pvp-enabled").toString());
        this.msgMap.put("pvp-disabled", this.messages.get("pvp-disabled").toString());
        this.msgMap.put("regain-info", this.messages.get("regain-info").toString());
        this.msgMap.put("opped-player", this.messages.get("opped-player").toString());
        this.msgMap.put("deopped-player", this.messages.get("deopped-player").toString());
        this.msgMap.put("must-be-online", this.messages.get("must-be-online").toString());
        this.msgMap.put("leave-message", this.messages.get("leave-message").toString());
        this.msgMap.put("motd-display", this.messages.get("motd-display").toString());
        this.msgMap.put("motd-changed", this.messages.get("motd-changed").toString());
        this.msgMap.put("motd-too-long", this.messages.get("motd-too-long").toString());
        this.msgMap.put("server-join-online-guest-title", this.messages.get("server-join-online-guest-title").toString());
        this.msgMap.put("server-join-offline-guest-title", this.messages.get("server-join-offline-guest-title").toString());
        this.msgMap.put("server-join-online-owner-title", this.messages.get("server-join-online-owner-title").toString());
        this.msgMap.put("server-join-offline-owner-title", this.messages.get("server-join-offline-owner-title").toString());
        this.msgMap.put("server-stop-online-owner-title", this.messages.get("server-stop-online-owner-title").toString());
        this.msgMap.put("server-stop-online-guest-title", this.messages.get("server-stop-online-guest-title").toString());
        this.msgMap.put("server-stop-offline-owner-title", this.messages.get("server-stop-offline-owner-title").toString());
        this.msgMap.put("server-stop-offline-guest-title", this.messages.get("server-stop-offline-guest-title").toString());
        this.msgMap.put("other-server-not-online-title", this.messages.get("other-server-not-online-title").toString());
        this.msgMap.put("no-server-title", this.messages.get("no-server-title").toString());
        this.msgMap.put("other-no-server-title", this.messages.get("other-no-server-title").toString());
        this.msgMap.put("server-expired-title", this.messages.get("server-expired-title").toString());
        this.msgMap.put("max-memory-reached-title", this.messages.get("max-memory-reached-title").toString());
        this.msgMap.put("max-servers-reached-title", this.messages.get("max-servers-reached-title").toString());
        this.msgMap.put("max-players-count", this.messages.get("max-players-count").toString());
        this.msgMap.put("no-player-specified-title", this.messages.get("no-player-specified-title").toString());
        this.msgMap.put("sent-fallback-title", this.messages.get("sent-fallback-title").toString());
        this.msgMap.put("not-player-server-title", this.messages.get("not-player-server-title").toString());
        this.msgMap.put("template-doesnt-exist-title", this.messages.get("template-doesnt-exist-title").toString());
        this.msgMap.put("no-templates-found-title", this.messages.get("no-templates-found-title").toString());
        this.msgMap.put("create-start-title", this.messages.get("create-start-title").toString());
        this.msgMap.put("have-server-title", this.messages.get("have-server-title").toString());
        this.msgMap.put("delete-warning-title", this.messages.get("delete-warning-title").toString());
        this.msgMap.put("recently-started-title", this.messages.get("recently-started-title").toString());
        this.msgMap.put("added-to-queue-title", this.messages.get("added-to-queue-title").toString());
        this.msgMap.put("removed-from-queue-title", this.messages.get("removed-from-queue-title").toString());
        this.msgMap.put("queue-startup-title", this.messages.get("queue-startup-title").toString());
        this.msgMap.put("help-ps-header", this.messages.get("help-ps-header").toString());
        this.msgMap.put("help-ps-join", this.messages.get("help-ps-join").toString());
        this.msgMap.put("help-ps-leave", this.messages.get("help-ps-leave").toString());
        this.msgMap.put("help-ps-create", this.messages.get("help-ps-create").toString());
        this.msgMap.put("help-ps-home", this.messages.get("help-ps-home").toString());
        this.msgMap.put("help-ps-stop", this.messages.get("help-ps-stop").toString());
        this.msgMap.put("help-ps-delete", this.messages.get("help-ps-delete").toString());
        this.msgMap.put("help-ps-motd", this.messages.get("help-ps-motd").toString());
        this.msgMap.put("help-ps-time", this.messages.get("help-ps-time").toString());
        this.msgMap.put("help-ps-worlds", this.messages.get("help-ps-worlds").toString());
        this.msgMap.put("help-ps-sharetime", this.messages.get("help-ps-sharetime").toString());
        this.msgMap.put("help-psa-header", this.messages.get("help-psa-header").toString());
        this.msgMap.put("help-psa-create", this.messages.get("help-psa-create").toString());
        this.msgMap.put("help-psa-templates", this.messages.get("help-psa-templates").toString());
        this.msgMap.put("help-psa-join", this.messages.get("help-psa-join").toString());
        this.msgMap.put("help-psa-start", this.messages.get("help-psa-start").toString());
        this.msgMap.put("help-psa-stop", this.messages.get("help-psa-stop").toString());
        this.msgMap.put("help-psa-delete", this.messages.get("help-psa-delete").toString());
        this.msgMap.put("help-psa-stopall", this.messages.get("help-psa-stopall").toString());
        this.msgMap.put("help-psa-kill", this.messages.get("help-psa-kill").toString());
        this.msgMap.put("help-psa-addtime", this.messages.get("help-psa-addtime").toString());
        this.msgMap.put("help-psa-removetime", this.messages.get("help-psa-removetime").toString());
        this.msgMap.put("help-psa-checktime", this.messages.get("help-psa-checktime").toString());
        this.msgMap.put("help-psa-maxmem", this.messages.get("help-psa-maxmem").toString());
        this.msgMap.put("help-psa-startmem", this.messages.get("help-psa-startmem").toString());
        this.msgMap.put("help-psa-slots", this.messages.get("help-psa-slots").toString());
        this.msgMap.put("help-psa-reload", this.messages.get("help-psa-reload").toString());
        this.msgMap.put("help-psa-motd", this.messages.get("help-psa-motd").toString());
        this.msgMap.put("help-mys-header", this.messages.get("help-mys-header").toString());
        this.msgMap.put("help-mys-settings", this.messages.get("help-mys-settings").toString());
        this.msgMap.put("help-mys-ban", this.messages.get("help-mys-ban").toString());
        this.msgMap.put("help-mys-kick", this.messages.get("help-mys-kick").toString());
        this.msgMap.put("help-mys-whitelist", this.messages.get("help-mys-whitelist").toString());
        this.msgMap.put("help-mys-op", this.messages.get("help-mys-op").toString());
        this.msgMap.put("help-mys-regain", this.messages.get("help-mys-regain").toString());
        this.msgMap.put("help-mys-stop",  this.messages.get("help-mys-stop").toString());
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
        
        if (!hashMap.isEmpty()) {
            messages = new HashMap<>(msgMap);
            saveConfig(messages, "messages.yml");
            utils.debug("Saved updated messages.yml file.");
        }
    }

    public void clearOnlineServers() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File onlineFile = new File(getDataFolder(), "online.yml");
        if (!onlineFile.exists()) copyResource(onlineFile);

        try (InputStream inputStream = new FileInputStream(getDataFolder().getPath() + File.separator + "online.yml")) {
            online = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        online.put("servers", new HashMap<>());
        saveConfig(online, "online.yml");
    }

    public void loadPlayer(UUID uuid, StoredPlayer player) {
        playerMap.put(uuid, player);
        playerMapChanged = true;
    }

    public void copyResource(File file) {
        if (!file.exists()) {
            try (InputStream resourceStream = PlayerServers.class.getClassLoader().getResourceAsStream(file.getName());
                 FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                ByteStreams.copy(resourceStream, fileOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupScripts() {
        // Example script setup logic
        File scriptDir = new File(getDataFolder(), "scripts");
        utils.debug("scriptsDir = " + scriptDir);
        if (!scriptDir.exists()) {
            scriptDir.mkdir();
        }
        copyResource(new File(getDataFolder(), "scripts" + File.separator + "PSWrapper.jar"));
    }

    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    public static PlayerServersAPI getApi() {
        return PlayerServers.api;
    }

    public PlayerServers getInstance() {
        return this;
    }

    public PluginDescription getDescription() {
        return this.container.getDescription();
    }

    public Logger getLogger() {
        return logger;
    }
}
