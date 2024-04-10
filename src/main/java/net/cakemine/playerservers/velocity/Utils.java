package net.cakemine.playerservers.velocity;

import java.util.logging.*;
import java.util.concurrent.*;
import com.google.common.base.*;
import com.google.common.io.ByteStreams;

import java.util.*;
import java.util.Map.Entry;

import com.google.gson.*;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import net.cakemine.playerservers.velocity.objects.PlayerServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.regex.*;

import org.jetbrains.annotations.NotNull;

public class Utils
{
    PlayerServers pl;
    
    public Utils(PlayerServers pl) {
        this.pl = pl;
    }
    
    public void sendMsg(Player proxiedPlayer, String s) {
        if (s == null) {
            this.log(Level.WARNING, "Message missing somehow! Update your messages.yml, MOVE it (to keep a backup of your changes) and let it regenerate.");
            return;
        }
        if (s.isEmpty()) {
            return;
        }
        if (s.contains("||")) {
            String[] split = s.split("\\|\\|");
            for (int length = split.length, i = 0; i < length; ++i) {
                proxiedPlayer.sendMessage(this.pl.utils.color(this.pl.prefix + split[i]));
            }
        }
        else {
            proxiedPlayer.sendMessage(this.color(this.pl.prefix + s));
        }
    }
    
    public void sendMsg(CommandSource commandSender, String s) {
        if (s == null || s.isEmpty() || s.matches("^" + this.pl.prefix + "(\\s)?$")) {
            this.log(Level.WARNING, "Message missing somehow! Update your messages.yml, MOVE it (to keep a backup of your changes) and let it regenerate.");
            return;
        }
        if (s.isEmpty()) {
            return;
        }
        if (s.contains("||")) {
            String[] split = s.split("\\|\\|");
            for (int length = split.length, i = 0; i < length; ++i) {
                commandSender.sendMessage(this.color(this.pl.prefix + split[i]));
            }
        }
        else {
            commandSender.sendMessage(this.color(this.pl.prefix + s));
        }
    }
    
    public @NotNull TextComponent color(String s) {
    	Map<String, String> colorMap = new HashMap<>();
        colorMap.put("&0", "<#000000>");
        colorMap.put("&1", "<#0000AA>");
        colorMap.put("&2", "<#00AA00>");
        colorMap.put("&3", "<#00AAAA>");
        colorMap.put("&4", "<#AA0000>");
        colorMap.put("&5", "<#AA00AA>");
        colorMap.put("&6", "<#FFAA00>");
        colorMap.put("&7", "<#AAAAAA>");
        colorMap.put("&8", "<#555555>");
        colorMap.put("&9", "<#5555FF>");
        colorMap.put("&a", "<#55FF55>");
        colorMap.put("&b", "<#55FFFF>");
        colorMap.put("&c", "<#FF5555>");
        colorMap.put("&d", "<#FF55FF>");
        colorMap.put("&e", "<#FFFF55>");
        colorMap.put("&f", "<#FFFFFF>");
        colorMap.put("&o", "<italic>");
        colorMap.put("&k", "<obfuscated>");
        colorMap.put("&u", "<underlined>");
        colorMap.put("&m", "<strikethrough>");
        colorMap.put("&r", "<reset>");
        colorMap.put("&l", "<bold>");

        Pattern pattern = Pattern.compile("&[0-9a-fA-FkKlLmMoOrR]");

        // Create a matcher to find color codes in the input string
        Matcher matcher = pattern.matcher(s);

        // Replace color codes with Velocity color codes
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String colorCode = matcher.group();
            String color = colorMap.get(colorCode.toLowerCase());
            if (color != null) {
                matcher.appendReplacement(output, color);
            }
            
        }
        matcher.appendTail(output);
        Component component = MiniMessage.miniMessage().deserialize(output.toString());
        @NotNull TextComponent plainText = Component.text().append(component).build();
        return plainText;
    }
    
    public @NotNull TextComponent stripColor(TextComponent s) {
    	Map<String, String> colorMap = new HashMap<>();
        colorMap.put("<black>", "");
        colorMap.put("<dark_blue>", "");
        colorMap.put("<dark_green>", "");
        colorMap.put("<dark_aqua>", "");
        colorMap.put("<dark_red>", "");
        colorMap.put("<dark_purple>", "");
        colorMap.put("<blue>", "");
        colorMap.put("<green>", "");
        colorMap.put("<aqua>", "");
        colorMap.put("<red>", "");
        colorMap.put("<light_purple>", "");
        colorMap.put("<yellow>", "");
        colorMap.put("<white>", "");
        

        Pattern pattern = Pattern.compile("<[^>]+>");

        // Create a matcher to find color codes in the input string
        Matcher matcher = pattern.matcher(MiniMessage.miniMessage().serialize(s));

        // Replace color codes with Velocity color codes
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String colorCode = matcher.group();
            String color = colorMap.get(colorCode.toLowerCase());
            if (color != null) {
                matcher.appendReplacement(output, color);
            }
        }
        matcher.appendTail(output);
        Component component = MiniMessage.miniMessage().deserialize(output.toString());
        @NotNull TextComponent plainText = Component.text().append(component).build();
        return plainText;
    }
    
    public void movePlayer(Player proxiedPlayer, String s, int n) {
        if (proxiedPlayer == null) {
            this.log(Level.SEVERE, "Player returned null when trying to send them to a server! Did they disconnect?");
            return;
        }
        if (s == null) {
            this.log(Level.SEVERE, "Server returned null when trying to send " + proxiedPlayer.getUsername() + " to it! Server removed/shutdown/failed to start?");
            return;
        }
        if (n <= 0) {
            if (pl.proxy.getServer(s).get().getServerInfo() == null) {
                this.log(Level.SEVERE, "Server info for server '" + s + "' returned null when trying to send " + proxiedPlayer.getUsername() + " to it! Server removed/shutdown/failed to start?");
                return;
            }
            proxiedPlayer.createConnectionRequest(pl.proxy.getServer(s).get()).connectWithIndication();
        }
        else {
        	this.pl.proxy.getScheduler()
        	  .buildTask(this.pl, () -> {
        		  if (pl.proxy.getServer(s).get().getServerInfo() == null) {
                      log(Level.SEVERE, "Server info for server '" + s + "' returned null when trying to send " + proxiedPlayer.getUsername() + " to it! Server removed/shutdown/failed to start?");
                      return;
                  }
                  proxiedPlayer.createConnectionRequest(pl.proxy.getServer(s).get()).connectWithIndication();
        	  })
        	  .delay((long)n, TimeUnit.SECONDS)
        	  .schedule();
        }
    }
    
    public void helpMessage(CommandSource commandSender, String replaceAll, String s) {
        if (commandSender instanceof Player) {
            if (!this.pl.psCommand.equals("playerserver")) {
                replaceAll = replaceAll.replaceAll("/ps ", "/" + this.pl.psCommand + " ");
            }
            Player proxiedPlayer = (Player)commandSender;
            proxiedPlayer.sendMessage(this.color("&b" + replaceAll + "&8 » &6" + s));
        }
        else {
        	commandSender.sendMessage(this.stripColor(this.color("&b" + replaceAll + "&8 » &6" + s)));
        }
    }
    
    public String getSrvName(String s) {
        if (!this.pl.serverManager.hasServer(s)) {
            this.pl.utils.log(Level.WARNING, "Tried to get server name for " + s + "'s server, but server did not exist in the server map (servers.yml)! Returned \"empty\"");
            return "empty";
        }
        if (this.pl.serverManager.getServerInfo(s, "server-name") != null && !this.pl.serverManager.getServerInfo(s, "server-name").equalsIgnoreCase("null")) {
            return this.pl.serverManager.getServerInfo(s, "server-name");
        }
        if (this.pl.settingsManager.propExists(s) && this.pl.settingsManager.getSetting(s, "server-name") != null && !this.pl.settingsManager.getSetting(s, "server-name").equalsIgnoreCase("null")) {
            return this.pl.settingsManager.getSetting(s, "server-name");
        }
        this.pl.utils.log(Level.SEVERE, s + "'s server name was null! Using their name instead");
        return this.pl.utils.getName(s);
    }
    
    public String getSrvIp(String s) {
        if (!this.pl.serverManager.hasServer(s)) {
            this.pl.utils.log(Level.WARNING, "Tried to get server IP for " + s + "'s server, but server did not exist in the server map (servers.yml)!");
            return "127.0.0.1";
        }
        if (this.pl.serverManager.serverMap.get(s).getAllSettings().containsKey("server-ip")) {
            return this.pl.serverManager.serverMap.get(s).getSetting("server-ip").toString();
        }
        this.pl.serverManager.verifySettings(s);
        String setting;
        if (s == null || s.equalsIgnoreCase("null") || (setting = this.pl.settingsManager.getSetting(s, "server-ip")) == null) {
            return "127.0.0.1";
        }
        if (this.pl.serverManager.hasServer(s) && (this.pl.serverManager.getServerInfo(s, "server-ip") == null || this.pl.serverManager.getServerInfo(s, "server-ip").equalsIgnoreCase("null") || !(this.pl.serverManager.serverMap.get(s).getSetting("server-ip").equalsIgnoreCase(setting)))) {
            this.pl.serverManager.setServerInfo(s, "server-ip", setting);
        }
        return setting;
    }
    
    public int getSrvPort(String s) {
        if (s != null && !s.equalsIgnoreCase("null")) {
            if (!this.pl.serverManager.hasServer(s)) {
                this.pl.utils.log(Level.WARNING, "Tried to get server port for " + s + "'s server, but server did not exist in the server map (servers.yml)!");
                return 0;
            }
            if (String.valueOf(this.pl.serverManager.getServerInfo(s, "port")).matches("(\\d)+")) {
                return Integer.parseInt(this.pl.serverManager.getServerInfo(s, "port"));
            }
            if (this.pl.settingsManager.propExists(s) && this.pl.settingsManager.getSetting(s, "server-port").matches("(\\d)+")) {
                return Integer.parseInt(this.pl.settingsManager.getSetting(s, "server-port"));
            }
        }
        this.pl.utils.log(Level.SEVERE, "Server port was not a number! " + s + " | servers.yml value: " + String.valueOf(this.pl.serverManager.getServerInfo(s, "port")) + " | server.properties value: " + String.valueOf(this.pl.settingsManager.getSetting(s, "server-port")));
        return 0;
    }
    
    public int getNextPort() {
        return Integer.valueOf(this.pl.config.get("next-port").toString());
    }
    
    public void iteratePort() {
    	this.pl.config.put("next-port", (this.getNextPort() + 1));
        this.pl.saveConfig(this.pl.config, "config.yml");
    }
    
    public boolean isPortOpen(String s, int n) {
    	 boolean b = false;
         Socket socket = null;
         if (s.equals(pl.utils.getProxyIp())) {
             pl.utils.debug("isPortOpen input address was the same as proxy address! Check server.properties settings of your server templates is set to 127.0.0.1!");
         }
         try {
             socket = new Socket();
             socket.connect(new InetSocketAddress(s, n), 2500);
             b = false;
         }
         catch (Exception ex3) {
             b = true;
             if (socket != null) {
                 try {
                     socket.close();
                 }
                 catch (IOException ex) {
                     ex.printStackTrace();
                 }
             }
         }
         finally {
             if (socket != null) {
                 try {
                     socket.close();
                 }
                 catch (IOException ex2) {
                     ex2.printStackTrace();
                 }
             }
         }
         return b;
    }
    
    public String getServerUUID(String s) {
        for (Entry<String, PlayerServer> entry : this.pl.serverManager.serverMap.entrySet()) {
            if (s.equals(entry.getValue().getSetting("server-name"))) {
                return entry.getKey().toString();
            }
        }
        return null;
    }
    
    public boolean hasJoined(String s) {
        return this.pl.playerMap.containsKey(s);
    }
    
    public String getUUID(String s) {
        long currentTimeMillis = System.currentTimeMillis();
        if (this.pl.playerMap.containsKey(s)) {
            this.debug("Finished lookup (direct): " + (System.currentTimeMillis() - currentTimeMillis) + "ms later.");
            return this.pl.playerMap.get(s);
        }
        for (String s2 : this.pl.playerMap.keySet()) {
            if (s2.equalsIgnoreCase(s)) {
                this.debug("Finished lookup (loop): " + (System.currentTimeMillis() - currentTimeMillis) + "ms later.");
                return this.pl.playerMap.get(s2);
            }
        }
        if (this.pl.proxy.getConfiguration().isOnlineMode()) {
            this.log(Level.WARNING, s + "'s UUID needed but not stored, fetching it from the web.");
            String fetchUUID = this.fetchUUID(s);
            if (fetchUUID != null) {
                this.debug("That's so fetch. " + (System.currentTimeMillis() - currentTimeMillis) + "ms later.");
                return fetchUUID;
            }
        }
        this.log(Level.SEVERE, s + "'s UUID not found! getUUID returned null! " + (System.currentTimeMillis() - currentTimeMillis) + "ms later.");
        return null;
    }
    
    public String fetchUUID(String s) {
        if (this.pl.playerMap.containsKey(s)) {
            return this.pl.playerMap.get(s);
        }
        if (!this.pl.proxy.getConfiguration().isOnlineMode()) {
            this.pl.utils.debug("Offline mode network, generating UUID from OfflinePlayer name.");
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + s).getBytes(Charsets.UTF_8)).toString();
        }
        try {
            URLConnection openConnection2 = new URL("https://playerdb.co/api/player/minecraft/" + s).openConnection();
            openConnection2.setConnectTimeout(2000);
            openConnection2.setReadTimeout(1500);
            openConnection2.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36");
            InputStreamReader reader = new InputStreamReader(openConnection2.getInputStream());
            BufferedReader buffer = new BufferedReader(reader);
            String line2 = buffer.readLine();
            debug("PlayerDB.com response = " + line2);
            JsonObject jsonObject2 = (JsonObject)new Gson().fromJson(line2, JsonObject.class);
            if (jsonObject2.get("code").getAsString().contains("player.found")) {
                String uuid = jsonObject2.getAsJsonObject("data").getAsJsonObject("player").get("id").getAsString();
                pl.putPlayer(s, uuid);
                debug("fetchUUID from PlayerDB.com returns " + uuid);
                return uuid;
            }
            log("Unable to fetch " + s + "'s UUID from PlayerDB.com, error: " + jsonObject2.get("code").getAsString());
        }
        catch (SocketTimeoutException ex6) {
            log(Level.WARNING, "Connection to PlayerDB.com timed out, trying next provider...");
        }
        catch (FileNotFoundException ex7) {
            debug("FileNotFoundException, usually means the username is invalid or website is down.");
        }
        catch (IOException ex2) {
            log(Level.SEVERE, "Unable to fetch " + s + "'s UUID from PlayerDB.com");
            if (pl.debug) {
                ex2.printStackTrace();
            }
        }
        try {
            URLConnection openConnection3 = new URL("https://api.mojang.com/users/profiles/minecraft/" + s).openConnection();
            openConnection3.setConnectTimeout(1500);
            openConnection3.setReadTimeout(1500);
            openConnection3.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36");
            InputStreamReader reader = new InputStreamReader(openConnection3.getInputStream());
            String line3 = new BufferedReader(reader).readLine();
            debug("mojang response = " + line3);
            JsonObject jsonObject3 = (JsonObject)new Gson().fromJson(line3, JsonObject.class);
            if (jsonObject3.get("error") == null) {
                String replaceAll = jsonObject3.get("id").getAsString().replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                pl.putPlayer(s, replaceAll);
                debug("fetchUUID from Mojang returns " + replaceAll);
                return replaceAll;
            }
            log("Unable to fetch " + s + "'s UUID from Mojang, error: " + jsonObject3.get("error").getAsString());
        }
        catch (SocketTimeoutException ex8) {
            log(Level.WARNING, "Connection to mc-api.net timed out, trying next provider...");
        }
        catch (FileNotFoundException ex9) {
            debug("FileNotFoundException, usually means the username is invalid or the website is down.");
        }
        catch (IOException ex3) {
            log(Level.SEVERE, "Unable to fetch " + s + "'s UUID from Mojang");
            if (pl.debug) {
                ex3.printStackTrace();
            }
        }
        return null;
    }
    
    public String fetchName(String s) {
        if (this.pl.playerMap.containsValue(s)) {
            for (Map.Entry<String, String> entry : this.pl.playerMap.entrySet()) {
                if (entry.getValue().equals(s)) {
                    return entry.getKey();
                }
            }
        }
        if (!this.pl.proxy.getConfiguration().isOnlineMode()) {
            this.pl.utils.debug("Cannot fetch name on offline-mode networks.");
            return null;
        }
        FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
            	try {
                    URLConnection openConnection2 = new URL("https://playerdb.co/api/player/minecraft/" + s).openConnection();
                    openConnection2.setConnectTimeout(2000);
                    openConnection2.setReadTimeout(1500);
                    openConnection2.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36");
                    InputStreamReader reader = new InputStreamReader(openConnection2.getInputStream());
                    String line2 = new BufferedReader(reader).readLine();
                    debug("PlayerDB.com response = " + line2);
                    JsonObject jsonObject2 = (JsonObject)new Gson().fromJson(line2, JsonObject.class);
                    if (jsonObject2.get("code").getAsString().contains("player.found")) {
                        String uuid = jsonObject2.getAsJsonObject("data").getAsJsonObject("player").get("username").getAsString();
                        pl.putPlayer(s, uuid);
                        debug("fetchUUID from PlayerDB.com returns " + uuid);
                        return uuid;
                    }
                    log("Unable to fetch " + s + "'s UUID from PlayerDB.com, error: " + jsonObject2.get("code").getAsString());
                }
                catch (SocketTimeoutException ex6) {
                    log(Level.WARNING, "Connection to PlayerDB.com timed out, trying next provider...");
                }
                catch (FileNotFoundException ex7) {
                    debug("FileNotFoundException, usually means the username is invalid or website is down.");
                }
                catch (IOException ex2) {
                    log(Level.SEVERE, "Unable to fetch " + s + "'s UUID from PlayerDB.com");
                    if (pl.debug) {
                        ex2.printStackTrace();
                    }
                }
                return null;
            }
        });
        try {
            return futureTask.get(5L, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        catch (ExecutionException ex2) {
            ex2.printStackTrace();
        }
        catch (TimeoutException ex3) {
            ex3.printStackTrace();
        }
        return null;
    }
    
    public String getName(String s) {
        String fetchName = null;
        if (s == null) {
            this.log(Level.SEVERE, "getName was passed a NULL UUID. Please send the following stack trace to the developer:");
            Thread.dumpStack();
            return null;
        }
        if (this.pl.playerMap.containsValue(s)) {
            for (Map.Entry<String, String> entry : this.pl.playerMap.entrySet()) {
                if (entry.getKey() == null) {
                    this.pl.utils.log(Level.WARNING, "A playerMap key was null!");
                }
                else if (entry.getValue() == null) {
                    this.pl.utils.log(Level.WARNING, entry.getKey() + "'s playerMap value was null!");
                }
                else {
                    if (!entry.getValue().equals(s)) {
                        continue;
                    }
                    fetchName = entry.getKey();
                }
            }
        }
        if (fetchName == null) {
            fetchName = this.fetchName(s);
        }
        if (fetchName == null) {
            this.log(Level.SEVERE, s + "'s name not found in server map and can't/failed to fetch! getName returned null!");
        }
        return fetchName;
    }
    
    public String getProxyIp() {
        if (this.pl.proxyAddress == null) {
            this.pl.utils.log("Checking external IP address...");
            String line = null;
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()));
                line = bufferedReader.readLine();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    }
                    catch (IOException ex2) {
                        ex2.printStackTrace();
                    }
                }
            }
            finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    }
                    catch (IOException ex3) {
                        ex3.printStackTrace();
                    }
                }
            }
            if (line == null) {
                this.pl.utils.log(Level.WARNING, "Proxy IP returned null!");
            }
            else {
                this.pl.utils.log("Proxy external IP found: " + line);
            }
            return line;
        }
        return this.pl.proxyAddress;
    }
    
    public void log(Level level, String s) {
        this.pl.getLogger().log(level, s);
    }
    
    public void log(String s) {
        this.pl.getLogger().info(s);
    }
    
    public void debug(String s) {
        if (this.pl.debug) {
            this.pl.getLogger().warning("DEBUG: " + s);
        }
    }
    
    public String doPlaceholders(String s, String s2) {
        if (s2.contains("%server-owner%")) {
            s2 = s2.replaceAll("%server-owner%", (this.getName(s) != null) ? this.getName(s) : "this player");
        }
        if (s2.contains("%fallback-server%")) {
            s2 = s2.replaceAll("%fallback-server%", this.pl.fallbackSrv);
        }
        if (s2.contains("%days-left%")) {
            s2 = s2.replaceAll("%days-left%", String.valueOf(this.pl.expiryTracker.daysLeft(s)));
        }
        if (s2.contains("%time-left%")) {
            s2 = s2.replaceAll("%time-left%", this.pl.expiryTracker.timeLeft(s));
        }
        if (s2.contains("%expire-date%")) {
            s2 = s2.replaceAll("%expire-date%", this.pl.expiryTracker.expireDate(s));
        }
        if (s2.contains("%max-mem%")) {
            s2 = s2.replaceAll("%max-mem%", String.valueOf(this.pl.serverManager.getServerInfo(s, "memory")).split("\\/")[0]);
        }
        if (s2.contains("%start-mem%")) {
            s2 = s2.replaceAll("%start-mem%", String.valueOf(this.pl.serverManager.getServerInfo(s, "memory")).split("\\/")[1]);
        }
        if (s2.contains("%slot-count%")) {
            s2 = s2.replaceAll("%slot-count%", this.pl.serverManager.getServerInfo(s, "max-players"));
        }
        if (s2.contains("%motd%")) {
            s2 = s2.replaceAll("%motd%", this.pl.serverManager.getServerInfo(s, "motd"));
        }
        if (s2.contains("%whitelist%")) {
            s2 = s2.replaceAll("%whitelist%", this.pl.serverManager.getServerInfo(s, "white-list"));
        }
        if (s2.contains("%max-players%")) {
            s2 = s2.replaceAll("%max-players%", this.pl.serverManager.getServerInfo(s, "max-players"));
        }
        return s2;
    }
    
    public void copyFile(File file, File file2) {
    	try (InputStream inputStream = new FileInputStream(file)) {
            // Create output stream for writing to symbolic link
            try (OutputStream outputStream = Files.newOutputStream(file2.toPath())) {
                // Copy content from input stream to output stream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
			this.pl.utils.log(Level.SEVERE, "Failed to copy file (" + file.getPath() + "). Please send this stack trace to the developer.");
        	e.printStackTrace();
		}
    }
    
    protected void vCheck() {
        this.pl.proxy.getScheduler().buildTask(pl, () -> {
        	String a = "spigot";
        	String b = "mc.";
        	String c = "org";
        	String[] spigotVersion;
        	try {
        		boolean update = false;
        		boolean dev = false;
        		InputStreamReader reader = new InputStreamReader(new URL("https://api." + a + b + c + "/legacy/update.php?resource=79587").openStream());
        		spigotVersion = new BufferedReader(reader).readLine().replace(".", " ").split("-")[0].split(" ");
				int[] currentVersion = pl.getApi().getPluginVersion();
				
				int[] newVersion = new int[spigotVersion.length];
		        for (int i = 0; i < spigotVersion.length; i++) {
		        	newVersion[i] = Integer.parseInt(spigotVersion[i]);
		        }

		        for (int i = 0; i < currentVersion.length; i++) {
		        	if (newVersion[i] > currentVersion[i]) {
		        		update = true;
		        		break;
		        	}
		        	if (currentVersion[i] > newVersion[i]) {
		        		pl.utils.log(String.valueOf(currentVersion[i]));
		        		pl.utils.log(String.valueOf(newVersion[i]));
		        		dev = true;
		        		break;
		        	}
		        }
		    	if (dev) {
					Utils.this.log(Level.WARNING, "This is a dev build, updates are disabled!");
					return;
				}
		    	
		    	if (update) {
		    		Utils.this.log(Level.WARNING, "PlayerServers update " + String.valueOf(newVersion).replace(" ", ".") + " available! You're on " + String.valueOf(currentVersion).replace(" ", "."));
		    	}
        		else {
					Utils.this.log(Level.INFO, "No updates found!");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }).delay(2L, TimeUnit.SECONDS).schedule();
    }
    
    public int memStringToInt(String s) {
        if (Pattern.compile("[mM]").matcher(s).find()) {
            s = s.replaceAll("[MmBb]", "");
            return Integer.valueOf(s);
        }
        if (Pattern.compile("[Gg]").matcher(s).find()) {
            s = s.replaceAll("[GgBb]", "");
            return Integer.valueOf(s) * 1024;
        }
        return Integer.valueOf(s);
    }
    
    public boolean hasPerm(CommandSource commandSender, String s) {
        if (!(commandSender instanceof Player)) {
            return true;
        }
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        return proxiedPlayer.hasPermission(s) || (this.pl.permMap.containsKey(string) && this.pl.permMap.get(string).get(s) != null && this.pl.permMap.get(string).get(s).equalsIgnoreCase("true"));
    }
    
    public boolean hasPerm(String s, String s2) {
        return this.pl.permMap.containsKey(s) && this.pl.permMap.get(s).get(s2) != null && this.pl.permMap.get(s).get(s2).equalsIgnoreCase("true");
    }
    
    public void sendTitle(Player proxiedPlayer, String s) {
        if (s == null) {
            this.log(Level.WARNING, "Title Message missing somehow! Update your messages.yml, MOVE it (to keep a backup of your changes) and let it regenerate.");
            return;
        }
        if (s.isEmpty()) {
            return;
        }
        String[] split = s.split("\\|\\|");
        this.debug("lines length = " + split.length);
        if (split.length < 2) {
            this.sendTitlePart(proxiedPlayer, split[0], null);
        }
        else if (split.length == 3) {
            this.sendTitlePart(proxiedPlayer, split[0], split[1]);
            this.sendActionBar(proxiedPlayer, split[2]);
        }
        else if (split.length > 3) {
            int n = 1;
            int n2 = 1;
            for (String s2 : split) {
                if (n % 2 == 0) {
                    if (n2 != 0) {
                        this.debug("sending first title:" + split[n - 2] + "||" + split[n - 1]);
                        this.sendTitlePart(proxiedPlayer, split[n - 2], split[n - 1]);
                        n2 = 0;
                    }
                    else {
                    	debug("sending more titles:" + split[n - 2] + "||" + split[n - 1]);
                        sendTitlePart(proxiedPlayer, split[n - 2], split[n - 1]);
                    }
                }
                else if (split.length == n) {
                    this.debug("sending only title: " + split[n - 1]);
                    this.sendTitlePart(proxiedPlayer, split[n - 1], null);
                }
                ++n;
            }
        }
        else {
            this.sendTitlePart(proxiedPlayer, split[0], split[1]);
        }
    }
    
    public void sendTitlePart(Player proxiedPlayer, String s, String s2) {
    	Component title = Component.text("");
        Component subtitle = Component.text("");
    	
        if (s != null) {;
            title = this.color(s);
        }
        if (s2 != null) {
        	subtitle = this.color(s2);
        }

        if (s != null || s2 != null) {
        	proxiedPlayer.showTitle(Title.title(title, subtitle));
        }
    }
    
    public void sendActionBar(Player proxiedPlayer, String s) {
        if (s == null) {
            this.log(Level.WARNING, "Action Bar Message missing somehow! Update your messages.yml, MOVE it (to keep a backup of your changes) and let it regenerate.");
            return;
        }
        if (s.isEmpty()) {
            return;
        }
        proxiedPlayer.sendActionBar(this.color(s));
    }
    
    public CommandSource getSender(String s) {
        if (!s.equalsIgnoreCase("console")) {
            return this.pl.proxy.getPlayer(UUID.fromString(s)).get();
        }
        return this.pl.proxy.getConsoleCommandSource();
    }
    
    public ServerConnection getCurrentServer(String s) {
        Player player = this.pl.proxy.getPlayer(UUID.fromString(s)).get();
        if (player == null) {
            this.debug("getCurrentServer of " + s + " failed! Player was null.");
            return null;
        }
        if (player.getCurrentServer() == null) {
            this.debug("getCurrentServer of " + s + " failed! Player was found but server was null.");
        }
        return player.getCurrentServer().get();
    }
}
