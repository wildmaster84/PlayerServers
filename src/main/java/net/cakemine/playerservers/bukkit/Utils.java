package net.cakemine.playerservers.bukkit;

import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.*;
import org.bukkit.command.*;
import org.bukkit.*;
import com.google.common.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.attribute.*;
import java.nio.file.*;
import java.nio.file.Files;
import java.io.*;

public class Utils
{
    PlayerServers pl;
    
    public Utils(PlayerServers pl) {
        this.pl = pl;
    }
    
    public static String getLevel() {
        File file = new File("");
        try {
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath() + File.separator + "server.properties");
            Properties properties = new Properties();
            properties.load(fileInputStream);
            fileInputStream.close();
            return properties.getProperty("level-name");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public String getOwnerId() {
        File parentFile = this.pl.getDataFolder().getParentFile().getAbsoluteFile().getParentFile();
        this.debug("getOwnerId file = " + parentFile.getAbsolutePath() + " | Owner Id:: " + parentFile.getName());
        return parentFile.getName();
    }
    
    public void shutdown(int n) {
        Scheduler.runTaskLater(this.pl, ()-> {
        	for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                player.sendMessage(Utils.this.pl.messages.get("shutdown-msg"));
                this.movePlayer(player, this.pl.fallbackSrv);
            }
        	Bukkit.getServer().shutdown();
        }, n);
    }
    
    public void debug(String s) {
        if (this.pl.debug) {
            this.pl.getLogger().warning("DEBUG: " + s);
        }
    }
    
    public void debug(JavaPlugin plugin, String s) {
    	plugin.getLogger().warning("DEBUG: " + s);
    }
    
    public void broadcast(String s) {
        if (s == null) {
            this.log(Level.WARNING, "Message missing somehow! Update your messages.yml, MOVE it (to keep a backup of your changes) and let it regenerate.");
            return;
        }
        if (s.isEmpty()) {
            return;
        }
        if (s.contains("||")) {
            for (String s2 : s.split("\\|\\|")) {
                if (this.pl.listener.syncDone) {
                    Bukkit.broadcastMessage(this.pl.prefix + this.color(s2));
                }
                else {
                    Bukkit.broadcastMessage(this.color(s2));
                }
            }
        }
        else if (this.pl.listener.syncDone) {
            Bukkit.broadcastMessage(this.pl.prefix + this.color(s));
        }
        else {
            Bukkit.broadcastMessage(this.color(s));
        }
    }
    
    public void sendMsg(Player player, String s) {
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
                player.sendMessage(this.pl.prefix + this.color(split[i]));
            }
        }
        else {
            player.sendMessage(this.pl.prefix + this.color(s));
        }
    }
    
    public void sendMsg(CommandSender commandSender, String s) {
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
                commandSender.sendMessage(this.pl.prefix + this.color(split[i]));
            }
        }
        else {
            commandSender.sendMessage(this.pl.prefix + this.color(s));
        }
    }
    
    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    
    public String stripColor(String s) {
        return ChatColor.stripColor(this.color(s));
    }
    
    public void log(Level level, String s) {
        Bukkit.getLogger().log(level, s);
    }
    
    public void log(String s) {
        Bukkit.getLogger().info(s);
    }
    
    public void helpMessage(Player player, String s, String s2) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("sendHelpMessage");
        dataOutput.writeUTF(player.getUniqueId().toString());
        dataOutput.writeUTF(s);
        dataOutput.writeUTF(s2);
        this.pl.sender.sendPluginMsg(player, dataOutput);
    }
    
    public String doPlaceholders(String s, String s2) {
        if (s2.contains("%fallback-server%")) {
            s2 = s2.replaceAll("%fallback-server%", this.pl.fallbackSrv);
        }
        if (s2.contains("%days-left%")) {
            s2 = s2.replaceAll("%days-left%", this.pl.daysLeft);
        }
        if (s2.contains("%time-left%")) {
            s2 = s2.replaceAll("%time-left%", this.pl.timeLeft);
        }
        if (s2.contains("%expire-date%")) {
            s2 = s2.replaceAll("%expire-date%", this.pl.expireDate);
        }
        if (s2.contains("%server-owner%")) {
            s2 = s2.replaceAll("%server-owner%", Bukkit.getOfflinePlayer(UUID.fromString(s)).getName());
        }
        return s2;
    }
    
    public void movePlayer(Player player, String s) {
    	Scheduler.runTaskLater(pl, () -> {
    		ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("Connect");
            dataOutput.writeUTF(player.getUniqueId().toString());
            dataOutput.writeUTF(s);
            player.sendPluginMessage(this.pl, "bungeecord:proxy", dataOutput.toByteArray());
    	}, 10L);
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
    
    public void doSoftCopy(File file, File file2) {
        if (file.isDirectory()) {
            if (!file2.exists()) {
                file2.mkdir();
            }
            for (File file3 : file.listFiles()) {
                this.doSoftCopy(file3, new File(file2, file3.getName()));
            }
        }
        else if (!this.pl.usingWindows && Files.isSymbolicLink(file.toPath())) {
            this.pl.utils.debug("Attempting to symlink " + file.getName() + " to " + file2.getAbsolutePath());
            try {
                Files.createSymbolicLink(file2.toPath(), file.toPath().toAbsolutePath(), (FileAttribute<?>[])new FileAttribute[0]);
            }
            catch (FileAlreadyExistsException ex3) {
                this.debug("Linked file already existed when trying to link to player's server.");
            }
            catch (IOException ex) {
                this.log(Level.SEVERE, "Failed to link file (" + file.getPath() + "). Please send this stack trace to the developer.");
                ex.printStackTrace();
            }
            catch (UnsupportedOperationException ex4) {
                this.log(Level.WARNING, "Failed to create symbolic link, creating a copy instead.");
                this.log(Level.WARNING, "Symbolic links may not be available on Windows, or certain file systems.");
                this.log(Level.WARNING, "Be sure to update ALL player server files when updating PlayerServers!");
                this.log(Level.WARNING, "You may want to create a script to update them all.");
                this.copyFile(file, file2);
            }
        }
        else if (!this.pl.usingWindows && file.getName().contains(".jar")) {
            this.pl.utils.debug("Attempting to symlink " + file.getName() + " to " + file2.getAbsolutePath());
            try {
                Files.createSymbolicLink(file2.toPath(), file.toPath().toAbsolutePath(), (FileAttribute<?>[])new FileAttribute[0]);
            }
            catch (FileAlreadyExistsException ex5) {
                this.log(Level.WARNING, file.getName() + " already existed at " + file2.getAbsolutePath() + " when trying to link to player's server.");
            }
            catch (IOException ex2) {
                this.log(Level.SEVERE, "Failed to link file (" + file.getPath() + "). Please send this stack trace to the developer.");
                ex2.printStackTrace();
            }
            catch (UnsupportedOperationException ex6) {
                this.log(Level.WARNING, "Failed to create symbolic link, creating a copy instead.");
                this.log(Level.WARNING, "Symbolic links may not be available on Windows, or certain file systems.");
                this.log(Level.WARNING, "Be sure to update ALL player server files when updating PlayerServers!");
                this.log(Level.WARNING, "You may want to create a script to update them all.");
                this.copyFile(file, file2);
            }
        }
        else {
            this.pl.utils.debug("Copying " + file.getName() + " to " + file2.getAbsolutePath());
            this.copyFile(file, file2);
        }
    }
    
    public void copyFile(File file, File file2) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            byte[] array = new byte[1024];
            int read;
            while ((read = fileInputStream.read(array)) > 0) {
                fileOutputStream.write(array, 0, read);
            }
            fileInputStream.close();
            fileOutputStream.close();
        }
        catch (IOException ex) {
            this.log(Level.SEVERE, "Failed to copy file (" + file.getPath() + "). Please send this stack trace to the developer.");
            ex.printStackTrace();
        }
    }
    
    public boolean doDelete(File file) {
        if (file.isDirectory()) {
            String[] list = file.list();
            for (int length = list.length, i = 0; i < length; ++i) {
                this.doDelete(new File(file, list[i]));
            }
            return file.delete();
        }
        return file.delete();
    }
    
    public String timeLeft() {
        return this.niceTime(this.pl.msLeft);
    }
    
    public String niceTime(long n) {
        long n2 = 1000L;
        long n3 = 60L * n2;
        long n4 = 60L * n3;
        long n5 = 24L * n4;
        double n6 = 2.63E9;
        StringBuilder sb = new StringBuilder();
        if (n > n6) {
            int n7 = (int)Math.round(n / n6);
            sb.append(n7);
            if (n7 == 1) {
                sb.append(" month ");
            }
            else {
                sb.append(" months ");
            }
            n %= (long)n6;
        }
        if (n > n5) {
            int n8 = (int)Math.round(Math.ceil(n / n5));
            sb.append(n8);
            if (n8 == 1) {
                sb.append(" day ");
            }
            else {
                sb.append(" days ");
            }
            n %= n5;
        }
        if (n > n4) {
            int n9 = (int)Math.round(Math.ceil(n / n4));
            sb.append(n9);
            if (n9 == 1) {
                sb.append(" hour ");
            }
            else {
                sb.append(" hours ");
            }
            n %= n4;
        }
        if (n > n3) {
            int n10 = (int)Math.round(Math.ceil(n / n3));
            sb.append(n10);
            if (n10 == 1) {
                sb.append(" min ");
            }
            else {
                sb.append(" mins ");
            }
            n %= n3;
        }
        if (n > n2 && sb.length() < 2) {
            sb.append((int)Math.round(Math.ceil(n / n2)));
            sb.append(" seconds ");
        }
        if (sb.length() > 1) {
            if (sb.charAt(sb.length() - 1) == ' ') {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        else {
            sb.append("0 min");
        }
        return sb.toString();
    }
}
