package net.cakemine.playerservers.bukkit.commands;

import net.cakemine.playerservers.bukkit.*;
import net.cakemine.playerservers.bukkit.sync.*;
import org.bukkit.command.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import com.google.common.io.*;
import java.util.*;

public class PlayerServerAdmin implements CommandExecutor
{
    PlayerServers pl;
    PluginSender pSend;
    
    public PlayerServerAdmin(PlayerServers pl) {
        this.pl = pl;
        this.pSend = new PluginSender(this.pl);
    }
    
    private void sendHelp(CommandSender commandSender) {
        this.pl.utils.sendMsg(commandSender, "&b&oValid Commands for /playerserveradmin (/psa):");
    }
    
    
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] array) {
        if (Bukkit.getOnlinePlayers().size() < 1) {
            this.pl.utils.sendMsg(commandSender, "&c&lThere must be at least one player on this server to forward commands!||&c&lThis is a limitation of plugin messaging channels.");
            return true;
        }
        if (array.length < 1) {
            this.sendHelp(commandSender);
            return false;
        }
        String string;
        if (commandSender instanceof Player) {
            string = ((Player)commandSender).getUniqueId().toString();
        }
        else {
            string = "console";
        }
        String lowerCase = array[0].toLowerCase();
        switch (lowerCase) {
            case "create": {
                this.psaCreate(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "");
                return true;
            }
            case "start": {
                this.psaStart(string, (array.length > 1) ? array[1] : "");
                return true;
            }
            case "stop": {
                this.psaStop(string, (array.length > 1) ? array[1] : "");
                return true;
            }
            case "stopall": {
                this.psaStopAll(string);
                return true;
            }
            case "join": {
                this.psaJoin(string, (array.length > 1) ? array[1] : "");
                return true;
            }
            case "addtime": {
                this.psaAddTime(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "", (array.length > 3) ? array[3] : "");
                return true;
            }
            case "removetime": {
                this.psaRemoveTime(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "", (array.length > 3) ? array[3] : "");
                return true;
            }
            case "checktime": {
                this.psaCheckTime(string, (array.length > 1) ? array[1] : "");
                return true;
            }
            case "maxmem":
            case "xmx":
            case "setxmx":
            case "memmax": {
                this.psaMaxMem(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "");
                return true;
            }
            case "startmem":
            case "xms":
            case "setxms":
            case "memstart": {
                this.psaStartMem(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "");
                return true;
            }
            case "reload": {
                this.psaReload(string);
                return true;
            }
            case "templates": {
                this.psaTemplates(string);
                return true;
            }
            case "slots":
            case "players":
            case "player":
            case "maxplayers":
            case "slotcount":
            case "slot":
            case "playercount": {
                this.psaSlots(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "");
                return true;
            }
            case "delete":
            case "remove":
            case "del":
            case "rem":
            case "destroy": {
                this.psaDelete(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "");
                return true;
            }
            case "motd":
            case "message": {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < array.length; ++i) {
                    sb.append(array[i]).append(" ");
                }
                if (sb.toString().endsWith(" ")) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                this.psaMotd(string, (array.length > 1) ? array[1] : "", (sb.length() > 0) ? sb.toString() : "");
                break;
            }
        }
        this.sendHelp(commandSender);
        return true;
    }
    
    private ByteArrayDataOutput makeByteArray(String[] array) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        for (int length = array.length, i = 0; i < length; ++i) {
            dataOutput.writeUTF(array[i]);
        }
        return dataOutput;
    }
    
    private Player getSender(String s) {
        Player player = Bukkit.getOnlinePlayers().iterator().next();
        if (!s.equalsIgnoreCase("console")) {
            player = Bukkit.getPlayer(UUID.fromString(s));
        }
        return player;
    }
    
    public void psaCreate(String s, String s2, String s3) {
        this.pl.utils.debug("Forwarding /psa create " + s2 + " " + s3);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaCreate", s, s2, s3 }));
    }
    
    public void psaDelete(String s, String s2, String s3) {
        this.pl.utils.debug("Forwarding /psa delete " + s2 + " " + s3);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaDelete", s, s2, s3 }));
    }
    
    public void psaStart(String s, String s2) {
        this.pl.utils.debug("Forwarding /psa start " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaStart", s, s2 }));
    }
    
    public void psaStop(String s, String s2) {
        this.pl.utils.debug("Forwarding /psa stop " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaStop", s, s2 }));
    }
    
    public void psaStopAll(String s) {
        this.pl.utils.debug("Forwarding /psa stopall");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaStopall", s }));
    }
    
    public void psaJoin(String s, String s2) {
        this.pl.utils.debug("Forwarding /psa join " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaJoin", s, s2 }));
    }
    
    public void psaAddTime(String s, String s2, String s3, String s4) {
        this.pl.utils.debug("Forwarding /psa addtime " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaAddTime", s, s2, s3, s4 }));
    }
    
    public void psaRemoveTime(String s, String s2, String s3, String s4) {
        this.pl.utils.debug("Forwarding /psa removetime " + s2 + " " + s3);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaRemoveTime", s, s2, s3, s4 }));
    }
    
    public void psaCheckTime(String s, String s2) {
        this.pl.utils.debug("Forwarding /psa checktime " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaCheckTime", s, s2 }));
    }
    
    public void psaMaxMem(String s, String s2, String s3) {
        this.pl.utils.debug("Forwarding /psa maxmem " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaMaxMem", s, s2, s3 }));
    }
    
    public void psaStartMem(String s, String s2, String s3) {
        this.pl.utils.debug("Forwarding /psa startmem " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaStartMem", s, s2, s3 }));
    }
    
    public void psaTemplates(String s) {
        this.pl.utils.debug("Forwarding /psa templates");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaTemplates", s }));
    }
    
    public void psaReload(String s) {
        this.pl.utils.debug("Forwarding /psa reload");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaReload", s }));
    }
    
    public void psaSlots(String s, String s2, String s3) {
        this.pl.utils.debug("Forwarding /psa slots");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaSlots", s, s2, s3 }));
    }
    
    public void psaMotd(String s, String s2, String s3) {
        this.pl.utils.debug("Forwarding /psa motd " + s2 + " " + s3);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psaMotd", s, s2, s3 }));
    }
}
