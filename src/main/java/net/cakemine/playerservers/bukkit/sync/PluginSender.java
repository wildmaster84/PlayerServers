package net.cakemine.playerservers.bukkit.sync;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import com.google.common.io.*;
import org.bukkit.*;
import java.util.logging.*;
import org.bukkit.permissions.*;

public class PluginSender {
    PlayerServers pl;
    public int syncCount;
    public int syncTotal;
    
    public PluginSender(PlayerServers pl) {
        this.pl = pl;
    }
    
    public void doSync() {
        if (this.pl.getServer().getOnlinePlayers().size() > 0) {
            this.doSync(this.pl.getServer().getOnlinePlayers().iterator().next());
        }
    }
    
    public void doSync(Player player) {
        this.pl.utils.debug("Starting sync...");
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("reSync");
        dataOutput.writeUTF(player.getUniqueId().toString());
        dataOutput.writeUTF("sync");
        this.sendPluginMsg(player, dataOutput);
    }
    
    public void confirmSync() {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            Player player = Bukkit.getOnlinePlayers().iterator().next();
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            this.pl.utils.debug("Confirming sync. syncTotal: " + this.syncTotal + " | syncCount: " + this.syncCount);
            dataOutput.writeUTF("confirmSync");
            dataOutput.writeUTF(player.getUniqueId().toString());
            if (this.syncCount >= this.syncTotal) {
                dataOutput.writeUTF("confirm");
                this.pl.listener.syncDone = true;
            }
            else {
                dataOutput.writeUTF("failed");
            }
            this.sendPluginMsg(player, dataOutput);
        }
        else {
            this.pl.utils.log(Level.WARNING, "Need at least one player online to send sync confirmation!");
        }
    }
    
    public void versionMatcher(String s) {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            Player player = Bukkit.getOnlinePlayers().iterator().next();
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("version");
            dataOutput.writeUTF(player.getUniqueId().toString());
            if (!s.equalsIgnoreCase(this.pl.getDescription().getVersion())) {
                dataOutput.writeUTF("mismatch");
                this.pl.utils.log(Level.SEVERE, "================================================");
                this.pl.utils.log(Level.SEVERE, " PLUGIN VERSION MISMATCH! Please update BOTH    ");
                this.pl.utils.log(Level.SEVERE, " Bungee and Bukkit plugins to the same version! ");
                this.pl.utils.log(Level.SEVERE, "================================================");
            }
            else {
                dataOutput.writeUTF("match");
            }
            this.sendPluginMsg(player, dataOutput);
        }
    }
    
    public void expireCheck() {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            Player player = Bukkit.getOnlinePlayers().iterator().next();
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("expirecheck");
            dataOutput.writeUTF(player.getUniqueId().toString());
            dataOutput.writeUTF("check");
            this.sendPluginMsg(player, dataOutput);
        }
    }
    
    public void changeExpireTime(boolean b, int n, String s) {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            this.pl.utils.debug("Adding expire time...");
            Player player = Bukkit.getOnlinePlayers().iterator().next();
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            if (b) {
                dataOutput.writeUTF("APIaddTime");
            }
            else {
                dataOutput.writeUTF("APIremoveTime");
            }
            dataOutput.writeUTF(this.pl.utils.getOwnerId());
            dataOutput.writeUTF(String.valueOf(n));
            dataOutput.writeUTF(s);
            this.sendPluginMsg(player, dataOutput);
            this.doSync(player);
        }
        else {
            this.pl.utils.log(Level.WARNING, "&cOnline player required to send plugin message!");
        }
    }
    
    public void apiCall(ByteArrayDataOutput byteArrayDataOutput) {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            this.sendPluginMsg(Bukkit.getOnlinePlayers().iterator().next(), byteArrayDataOutput);
        }
        else {
            this.pl.utils.log(Level.WARNING, "&cOnline player required to send plugin message!");
        }
    }
    
    public void helperAnnounce(Player player) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("helperAnnounce");
        dataOutput.writeUTF(player.getUniqueId().toString());
        this.pl.utils.debug("announcing helper usage for " + player.getName());
        this.sendPluginMsg(player, dataOutput);
    }
    
    public void sendPerms(Player player) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("permissionCheck");
        dataOutput.writeUTF(player.getUniqueId().toString());
        this.pl.utils.debug("syncing permissions for " + player.getName());
        StringBuilder sb = new StringBuilder();
        for (String s : new String[] { "player", "admin", "psa.create", "psa.start", "psa.stop", "psa.stopall", "psa.addtime", "psa.removetime", "psa.checktime", "psa.memory", "psa.reload", "psa.delete", "psa.motd", "psa.kill", "ps.join", "ps.join.selector", "ps.create", "ps.startother", "ps.delete", "ps.checktime", "ps.motd", "ps.sharetime", "bypassexpire", "bypassblock", "bypasspurge", "bypassmaxservers" }) {
            sb.append("playerservers.").append(s).append(":");
            sb.append(String.valueOf(player.hasPermission("playerservers." + s)));
            sb.append("%%%");
        }
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (permissionAttachmentInfo.getPermission().matches("(?i)(playerservers\\.template(s)?\\.)(\\*|.*)")) {
                sb.append(permissionAttachmentInfo.getPermission()).append(":");
                sb.append(String.valueOf(player.hasPermission(permissionAttachmentInfo.getPermission())));
                sb.append("%%%");
            }
        }
        if (player.isOp()) {
            sb.append("playerservers.templates.all").append(":");
            sb.append("true");
            sb.append("%%%");
        }
        sb.delete(sb.length() - 3, sb.length());
        dataOutput.writeUTF(sb.toString());
        this.sendPluginMsg(player, dataOutput);
    }
    
    public void sendPluginMsg(Player player, ByteArrayDataOutput byteArrayDataOutput) {
        Scheduler.runTaskLater(this.pl, () -> {
        	pl.utils.debug("Sending plugin message:");
            player.sendPluginMessage(pl, "playerservers:core", byteArrayDataOutput.toByteArray());
        }, 5L);
    }
}
