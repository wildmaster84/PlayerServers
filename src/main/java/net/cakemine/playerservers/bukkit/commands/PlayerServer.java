package net.cakemine.playerservers.bukkit.commands;

import net.cakemine.playerservers.bukkit.*;
import net.cakemine.playerservers.bukkit.sync.*;
import org.bukkit.command.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import com.google.common.io.*;
import java.util.*;

public class PlayerServer implements CommandExecutor {
    PlayerServers pl;
    PluginSender pSend;
    
    public PlayerServer(PlayerServers pl) {
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
            if (commandSender instanceof Player) {
                this.psGUI(((Player)commandSender).getUniqueId().toString());
            }
            else {
                this.sendHelp(commandSender);
            }
        }
        else {
            String string;
            if (commandSender instanceof Player) {
                string = ((Player)commandSender).getUniqueId().toString();
            }
            else {
                string = "console";
            }
            String lowerCase = array[0].toLowerCase();
            switch (lowerCase) {
                case "j":
                case "join": {
                    this.psJoin(string, (array.length > 1) ? array[1] : "");
                    break;
                }
                case "bye":
                case "hub":
                case "l":
                case "leave": {
                    this.psLeave(string);
                    break;
                }
                case "new":
                case "create": {
                    this.psCreate(string, (array.length > 1) ? array[1] : "");
                    break;
                }
                case "templates":
                case "temps":
                case "setups":
                case "games":
                case "world":
                case "list":
                case "worlds":
                case "minigames":
                case "configs": {
                    this.psWorlds(string);
                    break;
                }
                case "del":
                case "remove":
                case "rem":
                case "delete": {
                    this.psDelete(string, (array.length > 1) ? array[1] : "");
                    break;
                }
                case "start":
                case "s":
                case "mine":
                case "home": {
                    this.psHome(string);
                    break;
                }
                case "end":
                case "stop": {
                    this.psStop(string);
                    break;
                }
                case "checktime":
                case "time":
                case "timeleft":
                case "expiretime":
                case "servertime": {
                    this.psChecktime(string);
                    break;
                }
                case "motd":
                case "message": {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < array.length; ++i) {
                        sb.append(array[i]).append(" ");
                    }
                    if (sb.toString().endsWith(" ")) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    this.psMotd(string, (sb.length() > 0) ? sb.toString() : "");
                    break;
                }
                case "sharetime":
                case "sendtime":
                case "donatetime":
                case "donate":
                case "share":
                case "stime": {
                    this.psShareTime(string, (array.length > 1) ? array[1] : "", (array.length > 2) ? array[2] : "", (array.length > 3) ? array[3] : "");
                    break;
                }
                default: {
                    this.sendHelp(commandSender);
                    break;
                }
            }
        }
        return true;
    }
    
    public void psJoin(String s, String s2) {
        this.pl.utils.debug("Forwarding /ps join " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psJoin", s, s2 }));
    }
    
    public void psLeave(String s) {
        this.pl.utils.debug("Forwarding /ps leave");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psLeave", s }));
    }
    
    public void psCreate(String s, String s2) {
        this.pl.utils.debug("Forwarding /ps create " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psCreate", s, s2 }));
    }
    
    public void psWorlds(String s) {
        this.pl.utils.debug("Forwarding /ps worlds");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psWorlds", s }));
    }
    
    public void psDelete(String s, String s2) {
        this.pl.utils.debug("Forwarding /ps delete " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psDelete", s, s2 }));
    }
    
    public void psHome(String s) {
        this.pl.utils.debug("Forwarding /ps home");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psHome", s }));
    }
    
    public void psStop(String s) {
        this.pl.utils.debug("Forwarding /ps stop");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psStop", s }));
    }
    
    public void psChecktime(String s) {
        this.pl.utils.debug("Forwarding /ps checktime");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psChecktime", s }));
    }
    
    public void psMotd(String s, String s2) {
        this.pl.utils.debug("Forwarding /ps motd " + s2);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psMotd", s, s2 }));
    }
    
    public void psShareTime(String s, String s2, String s3, String s4) {
        this.pl.utils.debug("Forwarding /ps sharetime " + s2 + " " + s3 + " " + s4);
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psShareTime", s, s2, s3, s4 }));
    }
    
    private void psGUI(String s) {
        this.pl.utils.debug("Forwarding /ps");
        this.pSend.sendPluginMsg(this.getSender(s), this.makeByteArray(new String[] { "psGUI", s }));
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
}
