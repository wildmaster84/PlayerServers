package net.cakemine.playerservers.velocity.commands;

import net.cakemine.playerservers.velocity.*;
import net.cakemine.playerservers.velocity.sync.*;
import net.kyori.adventure.pointer.Pointered;

import java.util.concurrent.*;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.*;
import java.io.*;

public class PlayerServer implements SimpleCommand {
    PlayerServers pl;
    ProxyServer proxy;
    PluginSender pSend;
    public static ArrayList<String> chill;
    
    public PlayerServer(PlayerServers pl, String s) {
        //super(s, null, new String[] { "pserver", "psrv", "ps" });
        this.proxy = pl.proxy;
        this.pl = pl;
        this.pSend = new PluginSender(this.pl);
    }
    
    public void startCooldown(String s) {
        PlayerServer.chill.add(s);
        this.proxy.getScheduler()
        .buildTask(this.pl, () -> {
        	PlayerServer.chill.remove(s);
        })
        .delay(30L, TimeUnit.SECONDS)
        .schedule();
    }
    
    private void addQueue(Player proxiedPlayer, String s) {
        if (this.pl.useQueue) {
            if (this.pl.serverManager.startQueue.containsKey(proxiedPlayer.getUniqueId().toString())) {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(s, this.pl.msgMap.get("removed-from-queue-title")));
                }
                else {
                    this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(s, this.pl.msgMap.get("removed-from-queue")));
                }
                this.pl.serverManager.startQueue.remove(proxiedPlayer.getUniqueId().toString());
            }
            else {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(s, this.pl.msgMap.get("added-to-queue-title")));
                }
                else {
                    this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(s, this.pl.msgMap.get("added-to-queue")));
                }
                this.pl.serverManager.startQueue.put(proxiedPlayer.getUniqueId().toString(), s);
            }
        }
    }
    
    private void sendHelp(CommandSource commandSender) {
        if (commandSender instanceof Player) {
            Player proxiedPlayer = (Player)commandSender;
            String string = proxiedPlayer.getUniqueId().toString();
            boolean hasPerm = this.pl.utils.hasPerm(string, "playerservers.player");
            this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("help-ps-header").replaceAll("%ps-command%", this.pl.psCommand));
            if (hasPerm || this.pl.utils.hasPerm(string, "playerservers.ps.join")) {
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps join <player>", this.pl.msgMap.get("help-ps-join"));
            }
            this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps leave", this.pl.msgMap.get("help-ps-leave"));
            if (hasPerm || this.pl.utils.hasPerm(string, "playerservers.ps.create")) {
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps create [world]", this.pl.msgMap.get("help-ps-create"));
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps home", this.pl.msgMap.get("help-ps-home"));
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps stop", this.pl.msgMap.get("help-ps-stop"));
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps delete", this.pl.msgMap.get("help-ps-delete"));
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps worlds", this.pl.msgMap.get("help-ps-worlds"));
            }
            if (hasPerm || this.pl.utils.hasPerm(string, "playerservers.ps.motd")) {
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps motd [message]", this.pl.msgMap.get("help-ps-motd"));
            }
            if (this.pl.useExpiry && !this.pl.utils.hasPerm(string, "playerservers.bypassexpire") && (hasPerm || this.pl.utils.hasPerm(string, "playerservers.ps.checktime"))) {
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps time", this.pl.msgMap.get("help-ps-time"));
            }
            if (this.pl.useExpiry && !this.pl.utils.hasPerm(string, "playerservers.bypassexpire") && (hasPerm || this.pl.utils.hasPerm(string, "playerservers.ps.sharetime"))) {
                this.pl.utils.helpMessage((CommandSource)proxiedPlayer, "/ps sharetime", this.pl.msgMap.get("help-ps-sharetime"));
            }
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("only-player-use"));
        }
    }
    
    @Override
    public void execute(final Invocation invocation) {
    	CommandSource commandSender = invocation.source();
    	String[] array = invocation.arguments();
    	
        if (array.length < 1) {
            if (commandSender instanceof Player && this.pl.usingHelper.containsKey(commandSender)) {
                this.pl.sender.controlGUI((Player)commandSender);
            }
            else {
                this.sendHelp(commandSender);
            }
        }
        else if (!(commandSender instanceof Player)) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("only-player-use"));
        }
        else {
            Player proxiedPlayer = (Player)commandSender;
            proxiedPlayer.getUniqueId().toString();
            String lowerCase = array[0].toLowerCase();
            switch (lowerCase) {
                case "j":
                case "join": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.join")) {
                        this.joinCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "bye":
                case "hub":
                case "l":
                case "leave": {
                    this.leaveCommand(commandSender, array);
                    break;
                }
                case "new":
                case "create": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                        this.createCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
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
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                        this.worldsCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "del":
                case "remove":
                case "rem":
                case "delete": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.delete")) {
                        this.deleteCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "start":
                case "s":
                case "mine":
                case "home": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                        this.startCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "end":
                case "stop": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                    	this.pl.utils.log("Received stop command");
                        this.stopCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "checktime":
                case "time":
                case "timeleft":
                case "expiretime":
                case "servertime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.checktime")) {
                        this.checkTimeCommand(proxiedPlayer);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "motd":
                case "message": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.motd")) {
                        this.motdCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "sharetime":
                case "sendtime":
                case "donatetime":
                case "donate":
                case "share":
                case "stime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.sharetime")) {
                        this.shareTimeCommand(proxiedPlayer, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                default: {
                    this.sendHelp(commandSender);
                    break;
                }
            }
        }
    }
    
    public void joinCommand(CommandSource commandSender, String[] array) {
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        if (array.length > 1) {
            String s = array[1];
            String uuid = this.pl.utils.getUUID(s);
            this.pl.utils.debug("joinCommand: name = " + s + " | targetuuid = " + uuid);
            if (this.pl.serverManager.hasServer(uuid) && !this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(string), this.pl.utils.getSrvPort(uuid)) && this.pl.serverManager.playerServers.contains(this.pl.utils.getSrvName(uuid))) {
                this.pl.utils.movePlayer(proxiedPlayer, this.pl.utils.getSrvName(uuid), 1);
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-online-guest-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-online-guest")));
                }
            }
            else if (this.pl.serverManager.hasServer(uuid)) {
                if (this.pl.utils.hasPerm(commandSender, "playerservers.ps.startother")) {
                    String s2 = ((HashMap) this.pl.serverManager.serverMap.get(uuid)).get("memory").toString().split("\\/")[0];
                    if (this.pl.useExpiry && this.pl.expiryTracker.msLeft(uuid) < 0L && !this.pl.utils.hasPerm(uuid, "playerservers.bypassexpire")) {
                        if (this.pl.useTitles) {
                            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-expired-title")));
                        }
                        else {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-expired")));
                        }
                    }
                    else if (this.pl.globalMaxRam > 0 && this.pl.serverManager.allocatedRam + this.pl.utils.memStringToInt(s2) > this.pl.globalMaxRam) {
                        if (this.pl.useTitles) {
                            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-memory-reached-title")));
                        }
                        else {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-memory-reached")));
                        }
                        this.addQueue(proxiedPlayer, uuid);
                    }
                    else if (this.pl.globalMaxServers > 0 && this.pl.serverManager.playerServers.size() >= this.pl.globalMaxServers && (!this.pl.utils.hasPerm(commandSender, "playerservers.bypassmaxservers") || !this.pl.utils.hasPerm(uuid, "playerservers.bypassmaxservers"))) {
                        if (this.pl.useTitles) {
                            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-servers-reached-title")));
                        }
                        else {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-servers-reached")));
                        }
                        this.addQueue(proxiedPlayer, uuid);
                    }
                    else if (this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(uuid), this.pl.utils.getSrvPort(uuid))) {
                        this.pl.serverManager.startupSrv(uuid, commandSender);
                        this.startCooldown(string);
                        if (this.pl.useTitles) {
                            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-offline-guest-title")));
                        }
                        else {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-offline-guest")));
                        }
                        this.pl.utils.movePlayer(proxiedPlayer, this.pl.utils.getSrvName(uuid), this.pl.joinDelay);
                    }
                    else {
                        if (this.pl.useTitles) {
                            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-online-guest-title")));
                        }
                        else {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-online-guest")));
                        }
                        this.pl.utils.movePlayer(proxiedPlayer, this.pl.utils.getSrvName(uuid), this.pl.onlineJoinDelay);
                    }
                }
                else if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-server-not-online-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-server-not-online")));
                }
            }
            else if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server-title")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
            }
        }
        else if (this.pl.usingHelper.containsKey(proxiedPlayer) && (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.join.selector"))) {
            this.pSend.serverSelector(proxiedPlayer);
        }
        else if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-player-specified-title")));
        }
        else {
            this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-player-specified")));
        }
    }
    
    public void leaveCommand(CommandSource commandSender, String[] array) {
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        if (!proxiedPlayer.getCurrentServer().get().getServerInfo().getName().equals(this.pl.fallbackSrv)) {
            if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("sent-fallback-title")));
            }
            else {
                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("sent-fallback")));
            }
            this.pl.utils.movePlayer(proxiedPlayer, this.pl.fallbackSrv, 1);
        }
        else if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("not-player-server-title")));
        }
        else {
            this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("not-player-server")));
        }
    }
    
    public void createCommand(CommandSource commandSender, String[] array) {
    	this.pl.utils.debug("createCommand Fired");
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        File templateFile = null;
        if (this.pl.useExpiry && !this.pl.resetExpiry && this.pl.serverManager.hasServer(string) && this.pl.expiryTracker.msLeft(string) < 0L && !this.pl.utils.hasPerm(commandSender, "playerservers.bypassexpire")) {
            if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-expired-title")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-expired")));
            }
            return;
        }
        
        if (array.length > 1) {
        	//Command
            templateFile = this.pl.templateManager.getTemplateFile(array[1]);
            this.pl.utils.debug("Selected template: " + templateFile + " from arg: " + array[1]);
            if (!this.pl.templateManager.templates.containsKey(templateFile)) {
              if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, (String)this.pl.msgMap.get("template-doesnt-exist-title"));
              } else {
                this.pl.utils.sendMsg(commandSender, (String)this.pl.msgMap.get("template-doesnt-exist"));
              } 
              return;
            } 
          } else {
        	//GUI
            if (this.pl.templateManager.templates.keySet().size() > 1 && this.pl.usingHelper.containsKey(proxiedPlayer)) {
              this.pl.utils.debug("Using world selector from helper plugin.");
              this.pSend.worldSelector(proxiedPlayer);
              return;
            } 
            if (this.pl.templateManager.templates.keySet().size() != 1 && (this.pl.templateManager.templates.keySet().size() <= 0 || this.pl.usingHelper.containsKey(proxiedPlayer))) {
              this.pl.utils.sendMsg(commandSender, "&c&lNo templates defined, Default template not found!||&e&oMake a template before trying to make a server.");
              return;
            } 
            this.pl.utils.debug("No helper found or only one template. Picking first template.");
            Iterator<File> iterator = this.pl.templateManager.templates.keySet().iterator();
            if (iterator.hasNext())
              templateFile = iterator.next(); 
        }
        
        if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("create-start-title")));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("create-start")));
        }
        this.pl.serverManager.createServer(proxiedPlayer, templateFile);
        this.proxy.getScheduler()
        .buildTask(this.pl, () -> {
        	PlayerServer.this.startCommand(commandSender, new String[0]);
        })
        .delay(5L, TimeUnit.SECONDS)
        .schedule();
    }
    
    public void worldsCommand(CommandSource commandSender, String[] array) {
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        if (this.pl.templateManager.templates.size() < 1) {
            if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-templates-found-title")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-templates-found"));
            }
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("available-templates"));
            Iterator<File> iterator = this.pl.templateManager.templates.keySet().iterator();
            while (iterator.hasNext()) {
                this.pl.utils.sendMsg(commandSender, "&7� &e&o" + iterator.next().getName());
            }
        }
    }
    
    public void deleteCommand(CommandSource commandSender, String[] array) {
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        if (this.pl.serverManager.hasServer(string)) {
            if (array.length < 2 || !array[1].equalsIgnoreCase("confirm")) {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("delete-warning-title")));
                }
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("delete-warning")));
            }
            else {
                if (!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(string), this.pl.utils.getSrvPort(string))) {
                    if (this.pl.useTitles) {
                        this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-stop-online-owner-title")));
                    }
                    else {
                        this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("server-stop-online-owner"));
                    }
                    this.pl.serverManager.deleteServer((CommandSource)proxiedPlayer, string);
                }
                else {
                    this.pl.serverManager.deleteServer((CommandSource)proxiedPlayer, string);
                }
                this.startCooldown(string);
            }
        }
        else if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server-title")));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server")));
        }
    }
    
    public void startCommand(CommandSource commandSender, String[] array) {
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        if (PlayerServer.chill != null && PlayerServer.chill.contains(string)) {
            if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("recently-started-title")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("recently-started")));
            }
        }
        else if (this.pl.serverManager.hasServer(string) && this.pl.serverManager.serverFilesExist(string)) {
            String s = ((HashMap) this.pl.serverManager.serverMap.get(string)).get("memory").toString().split("\\/")[0];
            if (this.pl.useExpiry && this.pl.expiryTracker.msLeft(string) < 0L && !this.pl.utils.hasPerm(commandSender, "playerservers.bypassexpire")) {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-expired-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-expired")));
                }
            }
            else if (this.pl.globalMaxRam > 0 && this.pl.serverManager.allocatedRam + this.pl.utils.memStringToInt(s) > this.pl.globalMaxRam) {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("max-memory-reached-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("max-memory-reached"));
                }
                this.addQueue(proxiedPlayer, string);
            }
            else if (this.pl.globalMaxServers > 0 && this.pl.serverManager.playerServers.size() >= this.pl.globalMaxServers && !this.pl.utils.hasPerm(commandSender, "playerservers.bypassmaxservers")) {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("max-servers-reached-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("max-servers-reached")));
                }
                this.addQueue(proxiedPlayer, string);
            }
            else if (this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(string), this.pl.utils.getSrvPort(string))) {
                this.pl.serverManager.startupSrv(string, commandSender);
                this.startCooldown(string);
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-join-offline-owner-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-join-offline-owner")));
                }
                this.pl.utils.movePlayer(proxiedPlayer, this.pl.utils.getSrvName(string), this.pl.joinDelay);
            }
            else {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-join-online-owner-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-join-online-owner")));
                }
                this.pl.utils.movePlayer(proxiedPlayer, this.pl.utils.getSrvName(string), this.pl.onlineJoinDelay);
            }
        }
        else if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server-title")));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server")));
        }
    }
    
    public void stopCommand(CommandSource commandSender, String[] array) {
        Player proxiedPlayer = (Player)commandSender;
        String string = proxiedPlayer.getUniqueId().toString();
        if (PlayerServer.chill != null && PlayerServer.chill.contains(string)) {
            if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("recently-started-title")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("recently-started")));
            }
        }
        else if (this.pl.serverManager.hasServer(string)) {
            if (this.pl.useExpiry && this.pl.expiryTracker.msLeft(string) < 0L && !this.pl.utils.hasPerm(commandSender, "playerservers.bypassexpire")) {
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-expired-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-expired")));
                }
            }
            else if (array.length > 1) {
                String uuid = this.pl.utils.getUUID(array[1]);
                if (this.pl.serverManager.hasServer(uuid)) {
                    if (!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(uuid), this.pl.utils.getSrvPort(uuid))) {
                        this.pl.serverManager.stopSrv(uuid);
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-stop-online-guest")));
                    }
                    else {
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-stop-offline-guest")));
                    }
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
                }
            }
            else if (!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(string), this.pl.utils.getSrvPort(string))) {
                this.pl.serverManager.stopSrv(string);
                if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-stop-online-owner-title")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-stop-online-owner")));
                }
                this.startCooldown(string);
            }
            else if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-stop-offline-owner-title")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("server-stop-offline-owner")));
            }
        }
        else if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server-title")));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server")));
        }
    }
    
    public void motdCommand(CommandSource commandSender, String[] array) {
        String string = ((Player)commandSender).getUniqueId().toString();
        if (array.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < array.length; ++i) {
                sb.append(array[i]).append(" ");
            }
            if (sb.toString().endsWith(" ")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            if (this.pl.utils.stripColor(this.pl.utils.color(sb.toString())).content().length() > 20) {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("motd-too-long"));
                return;
            }
            this.pl.serverManager.setServerInfo(string, "motd", sb.toString());
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("motd-changed")));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("motd-display")));
        }
    }
    
    public void checkTimeCommand(Player proxiedPlayer) {
        if (this.pl.serverManager.hasServer(proxiedPlayer.getUniqueId().toString())) {
            if (!this.pl.useExpiry || proxiedPlayer.hasPermission("playerservers.bypassexpire")) {
                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(proxiedPlayer.getUniqueId().toString(), this.pl.msgMap.get("check-expire-unlimited")));
            }
            else {
                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(proxiedPlayer.getUniqueId().toString(), this.pl.msgMap.get("check-expire-times")));
            }
        }
        else {
            this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(proxiedPlayer.getUniqueId().toString(), this.pl.msgMap.get("no-server")));
        }
    }
    
    public void shareTimeCommand(Player proxiedPlayer, String[] array) {
        if (!this.pl.useExpiry) {
            this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("no-share-unlimited"));
            return;
        }
        String string = proxiedPlayer.getUniqueId().toString();
        if (this.pl.serverManager.hasServer(string)) {
            if (array.length > 1) {
                String uuid = this.pl.utils.getUUID(array[1]);
                if (this.pl.serverManager.hasServer(uuid)) {
                    if (this.pl.utils.hasPerm(string, "playerservers.bypassexpire") || this.pl.utils.hasPerm(uuid, "playerservers.bypassexpire")) {
                        this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("no-share-unlimited"));
                        return;
                    }
                    if (array.length > 2) {
                        String s = "days";
                        if (array.length > 3) {
                            if (!this.pl.expiryTracker.validUnit(array[3])) {
                                this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("invalid-time-unit"));
                                return;
                            }
                            s = array[3];
                        }
                        if (array[2].matches("([0-9]+)")) {
                            if (this.pl.expiryTracker.msLeft(string) - this.pl.expiryTracker.stringToMillis(array[2] + " " + s) > 0L) {
                                this.pl.expiryTracker.addTime(uuid, Integer.valueOf(array[2]), s);
                                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-added-days").replaceAll("(%days-changed%)", array[2]).replaceAll("%time-changed%", array[2] + " " + s)));
                                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-days-left")));
                                this.pl.expiryTracker.removeTime(string, Integer.valueOf(array[2]), s);
                                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("other-removed-days").replaceAll("(%days-changed%)", array[2]).replaceAll("%time-changed%", array[2] + " " + s)));
                                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("other-days-left")));
                            }
                            else {
                                this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("not-enough-time"));
                            }
                        }
                        else {
                            this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("no-value-specified"));
                        }
                    }
                    else {
                        this.pl.utils.sendMsg(proxiedPlayer, this.pl.msgMap.get("no-value-specified"));
                    }
                }
                else if (this.pl.useTitles) {
                    this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server-title")));
                }
                else {
                    this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
                }
            }
            else if (this.pl.useTitles) {
                this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-player-specified-title")));
            }
            else {
                this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-player-specified")));
            }
        }
        else if (this.pl.useTitles) {
            this.pl.utils.sendTitle(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server")));
        }
        else {
            this.pl.utils.sendMsg(proxiedPlayer, this.pl.utils.doPlaceholders(string, this.pl.msgMap.get("no-server")));
        }
    }
    
    static {
        PlayerServer.chill = new ArrayList<String>();
    }

	public void execute(CommandSource commandSender, String[] array) {
		if (array.length < 1) {
            if (commandSender instanceof Player && this.pl.usingHelper.containsKey(commandSender)) {
                this.pl.sender.controlGUI((Player)commandSender);
            }
            else {
                this.sendHelp(commandSender);
            }
        }
        else if (!(commandSender instanceof Player)) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("only-player-use"));
        }
        else {
            Player proxiedPlayer = (Player)commandSender;
            proxiedPlayer.getUniqueId().toString();
            String lowerCase = array[0].toLowerCase();
            switch (lowerCase) {
                case "j":
                case "join": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.join")) {
                        this.joinCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "bye":
                case "hub":
                case "l":
                case "leave": {
                    this.leaveCommand(commandSender, array);
                    break;
                }
                case "new":
                case "create": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                        this.createCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
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
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                        this.worldsCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "del":
                case "remove":
                case "rem":
                case "delete": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.delete")) {
                        this.deleteCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "start":
                case "s":
                case "mine":
                case "home": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                        this.startCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "end":
                case "stop": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.create")) {
                    	this.pl.utils.log("Received stop command");
                        this.stopCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "checktime":
                case "time":
                case "timeleft":
                case "expiretime":
                case "servertime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.checktime")) {
                        this.checkTimeCommand(proxiedPlayer);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "motd":
                case "message": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.motd")) {
                        this.motdCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "sharetime":
                case "sendtime":
                case "donatetime":
                case "donate":
                case "share":
                case "stime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.player") || this.pl.utils.hasPerm(commandSender, "playerservers.ps.sharetime")) {
                        this.shareTimeCommand(proxiedPlayer, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                default: {
                    this.sendHelp(commandSender);
                    break;
                }
            }
        }
		
	}
}
