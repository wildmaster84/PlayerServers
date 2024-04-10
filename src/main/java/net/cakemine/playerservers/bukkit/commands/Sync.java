package net.cakemine.playerservers.bukkit.commands;

import net.cakemine.playerservers.bukkit.*;
import net.cakemine.playerservers.bukkit.sync.*;
import org.bukkit.command.*;
import org.bukkit.*;

public class Sync implements CommandExecutor
{
    PlayerServers pl;
    PluginSender pSend;
    
    public Sync(PlayerServers pl) {
        this.pl = pl;
        this.pSend = new PluginSender(this.pl);
    }
    
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] array) {
        if (commandSender instanceof ConsoleCommandSender || commandSender.isOp()) {
            if (Bukkit.getOnlinePlayers().size() > 0) {
                this.pl.templates.clear();
                this.pl.messages.clear();
                this.pSend.doSync(Bukkit.getOnlinePlayers().iterator().next());
            }
            else {
                this.pl.utils.sendMsg(commandSender, "&c&lThere must be at least one player on this server to forward commands!||&c&lThis is a limitation of plugin messaging channels.");
                this.pl.listener.syncDone = false;
            }
        }
        return true;
    }
}
