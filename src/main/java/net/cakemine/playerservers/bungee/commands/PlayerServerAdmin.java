package net.cakemine.playerservers.bungee.commands;

import net.cakemine.playerservers.bungee.*;
import net.cakemine.playerservers.bungee.events.PSReloadEvent;
import net.md_5.bungee.api.*;
import java.util.concurrent.*;
import net.md_5.bungee.api.plugin.*;
import net.md_5.bungee.api.config.*;
import net.md_5.bungee.api.connection.*;
import net.md_5.bungee.config.*;
import java.util.logging.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class PlayerServerAdmin extends Command
{
    PlayerServers pl;
    ProxyServer proxy;
    
    public PlayerServerAdmin(PlayerServers pl) {
        super("playerserveradmin", (String)null, new String[] { "pserveradmin", "psrvadmin", "psa" });
        this.proxy = ProxyServer.getInstance();
        this.pl = pl;
    }
    
    private void sendHelp(CommandSender commandSender) {
        boolean hasPerm = this.pl.utils.hasPerm(commandSender, "playerservers.admin");
        this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("help-psa-header"));
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.create")) {
            this.pl.utils.helpMessage(commandSender, "/psa create <player> [template]", this.pl.msgMap.get("help-psa-create"));
            this.pl.utils.helpMessage(commandSender, "/psa templates", this.pl.msgMap.get("help-psa-templates"));
        }
        if (commandSender instanceof ProxiedPlayer) {
            this.pl.utils.helpMessage(commandSender, "/psa join <player>", this.pl.msgMap.get("help-psa-join"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.start")) {
            this.pl.utils.helpMessage(commandSender, "/psa start <player>", this.pl.msgMap.get("help-psa-start"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.stop")) {
            this.pl.utils.helpMessage(commandSender, "/psa stop <player>", this.pl.msgMap.get("help-psa-stop"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.delete")) {
            this.pl.utils.helpMessage(commandSender, "/psa delete <player> [confirm]", this.pl.msgMap.get("help-psa-delete"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.stopall")) {
            this.pl.utils.helpMessage(commandSender, "/psa stopall", this.pl.msgMap.get("help-psa-stopall"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.addtime")) {
            this.pl.utils.helpMessage(commandSender, "/psa addtime <player> <value> <mins/hrs/days/weeks/months>", this.pl.msgMap.get("help-psa-addtime"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.removetime")) {
            this.pl.utils.helpMessage(commandSender, "/psa removetime <player> <days> <mins/hrs/days/weeks/months>", this.pl.msgMap.get("help-psa-removetime"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.checktime")) {
            this.pl.utils.helpMessage(commandSender, "/psa checktime <player>", this.pl.msgMap.get("help-psa-checktime"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.memory")) {
            this.pl.utils.helpMessage(commandSender, "/psa maxmem <player> <value>", this.pl.msgMap.get("help-psa-maxmem"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.memory")) {
            this.pl.utils.helpMessage(commandSender, "/psa startmem <player> <value>", this.pl.msgMap.get("help-psa-startmem"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.slots")) {
            this.pl.utils.helpMessage(commandSender, "/psa slots <player> <number>", this.pl.msgMap.get("help-psa-slots"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.motd")) {
            this.pl.utils.helpMessage(commandSender, "/psa motd <player> [message]", this.pl.msgMap.get("help-psa-motd"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.reload")) {
            this.pl.utils.helpMessage(commandSender, "/psa reload", this.pl.msgMap.get("help-psa-reload"));
        }
        if (hasPerm || this.pl.utils.hasPerm(commandSender, "playerservers.psa.kill")) {
            this.pl.utils.helpMessage(commandSender, "/psa kill", this.pl.msgMap.get("help-psa-kill"));
        }
    }
    
    public void execute(CommandSender commandSender, String[] array) {
        if (array.length < 1) {
            this.sendHelp(commandSender);
        }
        else {
            String lowerCase = array[0].toLowerCase();
            switch (lowerCase) {
                case "create": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.create")) {
                        this.createCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "start": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.start")) {
                        this.startCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "stop": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.stop")) {
                        this.stopCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "stopall": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.stopall")) {
                        this.stopAllCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "join": {
                    this.joinCommand(commandSender, array);
                    break;
                }
                case "addtime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.addtime")) {
                        this.addTimeCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "removetime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.removetime")) {
                        this.removeTimeCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "checktime": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.checktime")) {
                        this.checkTimeCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "maxmem":
                case "xmx":
                case "setxmx":
                case "memmax": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.memory")) {
                        this.maxMemCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "startmem":
                case "xms":
                case "setxms":
                case "memstart": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.memory")) {
                        this.startMemCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "slots":
                case "players":
                case "player":
                case "maxplayers":
                case "slotcount":
                case "slot":
                case "playercount": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.slots")) {
                        this.slotsCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "delete":
                case "remove":
                case "del":
                case "rem":
                case "destroy": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.delete")) {
                        this.deleteCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "templates": {
                    this.templatesCommand(commandSender, array);
                    break;
                }
                case "reload": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.reload")) {
                        this.reloadCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "message":
                case "motd": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.motd")) {
                        this.motdCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "kill":
                case "killserver":
                case "forcestop": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.kill")) {
                        this.killCommand(commandSender, array);
                        break;
                    }
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-permissions"));
                    break;
                }
                case "debug": {
                    if (this.pl.utils.hasPerm(commandSender, "playerservers.admin") || this.pl.utils.hasPerm(commandSender, "playerservers.psa.debug")) {
                        this.debugCommand(commandSender, array);
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
    
    public void createCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else {
            String s = array[1];
            String uuid = this.pl.utils.getUUID(s);
            File templateFile = null;
            if (array.length > 2) {
                templateFile = this.pl.templateManager.getTemplateFile(array[2]);
                if (!this.pl.templateManager.templates.containsKey(templateFile)) {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("template-doesnt-exist"));
                    return;
                }
            }
            else {
                if (this.pl.templateManager.templates.keySet().size() <= 0) {
                    this.pl.utils.sendMsg(commandSender, "&c&lNo templates defined & Default template not found!||&e&oMake a template before trying to make a server.");
                    return;
                }
                Iterator<File> iterator = this.pl.templateManager.templates.keySet().iterator();
                if (iterator.hasNext()) {
                    templateFile = iterator.next();
                }
            }
            this.pl.serverManager.createServer(commandSender, s, uuid, templateFile);
        }
    }
    
    public void deleteCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                if (array.length < 3 || !array[2].equalsIgnoreCase("confirm")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("delete-warning")));
                }
                else if (!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(uuid), this.pl.utils.getSrvPort(uuid))) {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("server-stop-online-owner"));
                    this.pl.serverManager.stopSrv(uuid);
                    this.proxy.getScheduler().schedule((Plugin)this.pl, (Runnable)new Runnable() {
                        @Override
                        public void run() {
                            pl.serverManager.deleteServer(commandSender, uuid);
                        }
                    }, 3L, TimeUnit.SECONDS);
                }
                else {
                    this.pl.serverManager.deleteServer(commandSender, uuid);
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("no-server")));
            }
        }
    }
    
    public void startCommand(CommandSender commandSender, String[] array) {
        if (array.length > 1) {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                if (this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(uuid), this.pl.utils.getSrvPort(uuid))) {
                    this.pl.serverManager.startupSrv(uuid, commandSender);
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-started-server")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-server-already-online")));
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
            }
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
    }
    
    public void stopCommand(CommandSender commandSender, String[] array) {
        if (array.length > 1) {
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
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
    }
    
    public void stopAllCommand(CommandSender commandSender, String[] array) {
        this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("stopping-all-servers"));
        this.pl.serverManager.stopAll(commandSender);
        this.pl.utils.sendMsg(commandSender, "&7&o&m==============");
    }
    
    public void joinCommand(CommandSender commandSender, String[] array) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("only-player-use"));
        }
        else if (array.length > 1) {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (!this.pl.utils.isPortOpen(this.pl.utils.getSrvIp(uuid), this.pl.utils.getSrvPort(uuid)) && this.pl.serverManager.playerServers.contains(this.pl.utils.getSrvName(uuid))) {
                this.pl.utils.movePlayer((ProxiedPlayer)commandSender, this.pl.utils.getSrvName(uuid), 1);
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("server-join-online-guest")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-server-not-online")));
            }
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
    }
    
    public void addTimeCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else if (array.length < 3) {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-value-specified"));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
            }
        }
        else {
            String uuid2 = this.pl.utils.getUUID(array[1]);
            String s = "days";
            if (array.length > 3) {
                if (!this.pl.expiryTracker.validUnit(array[3])) {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("invalid-time-unit"));
                    return;
                }
                s = array[3];
            }
            if (this.pl.serverManager.hasServer(uuid2)) {
                if (array[2].matches("([0-9]+)")) {
                    this.pl.expiryTracker.addTime(uuid2, Integer.valueOf(array[2]), s);
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("other-added-days").replaceAll("(%days-changed%)", array[2]).replaceAll("%time-changed%", array[2] + " " + s)));
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("other-days-left")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-value-specified"));
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("other-no-server")));
            }
        }
    }
    
    public void removeTimeCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else if (array.length < 3) {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-value-specified"));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
            }
        }
        else {
            String uuid2 = this.pl.utils.getUUID(array[1]);
            String s = "days";
            if (array.length > 3) {
                if (!this.pl.expiryTracker.validUnit(array[3])) {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("invalid-date-unit"));
                    return;
                }
                s = array[3];
            }
            if (this.pl.serverManager.hasServer(uuid2)) {
                if (array[2].matches("([0-9]+)")) {
                    this.pl.expiryTracker.removeTime(uuid2, Integer.valueOf(array[2]), s);
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("other-removed-days").replaceAll("(%days-changed%)", array[2]).replaceAll("%time-changed%", array[2] + " " + s)));
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("other-days-left")));
                }
                else {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-value-specified"));
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("other-no-server")));
            }
        }
    }
    
    public void checkTimeCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("others-expire-times")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
            }
        }
    }
    
    public void slotsCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else if (array.length < 3) {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-players-count")));
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("other-no-server"));
            }
        }
        else {
            String uuid2 = this.pl.utils.getUUID(array[1]);
            if (!this.pl.utils.hasJoined(array[1])) {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("player-never-joined"));
            }
            else if (this.pl.serverManager.hasServer(uuid2)) {
                if (!array[2].matches("(\\+|-)?[0-9]+")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("invalid-slot-count"));
                }
                else if (array[2].matches("(\\+|-)[0-9]+")) {
                    String replaceAll = array[2].replaceAll("[0-9]+", "");
                    int intValue = Integer.valueOf(array[2].replaceAll("\\+", ""));
                    int intValue2 = Integer.valueOf(this.pl.serverManager.getServerInfo(uuid2, "max-players"));
                    if (replaceAll.contains("+")) {
                        intValue2 += intValue;
                    }
                    else if (replaceAll.contains("-")) {
                        intValue2 -= intValue;
                    }
                    else {
                        this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("invalid-slot-count"));
                    }
                    if (intValue2 < 1) {
                        this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("invalid-slot-count"));
                    }
                    else {
                        this.pl.serverManager.setServerInfo(uuid2, "max-players", String.valueOf(intValue2));
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("max-players-count")));
                    }
                }
                else if (Integer.valueOf(array[2]) < 1) {
                    this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("invalid-slot-count"));
                }
                else {
                    this.pl.serverManager.setServerInfo(uuid2, "max-players", array[2]);
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid2, this.pl.msgMap.get("max-players-count")));
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("other-no-server"));
            }
        }
    }
    
    public void maxMemCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else if (array.length < 3) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-value-specified"));
        }
        else {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (!this.pl.utils.hasJoined(array[1])) {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("player-never-joined"));
            }
            else if (this.pl.serverManager.hasServer(uuid)) {
                if (!array[2].matches("(\\+|-)?[0-9]+([Mm]|[Gg])")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("invalid-memory-format")));
                }
                else {
                    int memStringToInt = this.pl.utils.memStringToInt(this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[0]);
                    if (array[2].matches("(\\+|-)[0-9]+([Mm]|[Gg])")) {
                        String replaceAll = array[2].replaceAll("[A-Za-z0-9]", "");
                        String s;
                        if (replaceAll.matches("\\+")) {
                            s = String.valueOf(memStringToInt + this.pl.utils.memStringToInt(array[2].replaceAll("\\+", ""))) + "M";
                        }
                        else {
                            if (!replaceAll.matches("-")) {
                                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("invalid-memory-format")));
                                return;
                            }
                            s = String.valueOf(memStringToInt - this.pl.utils.memStringToInt(array[2].replaceAll("-", ""))) + "M";
                        }
                        if (this.pl.utils.memStringToInt(s) < this.pl.utils.memStringToInt(this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[1])) {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-lessthan-start")));
                            return;
                        }
                        this.pl.serverManager.setServerInfo(uuid, "memory", s + "/" + this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[1]);
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-memory-changed")));
                    }
                    else {
                        this.pl.serverManager.setServerInfo(uuid, "memory", array[2] + "/" + this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[1]);
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("max-memory-changed")));
                    }
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("other-no-server"));
            }
        }
    }
    
    public void startMemCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else if (array.length < 3) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-value-specified"));
        }
        else {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                if (!array[2].matches("(\\+|-)?[0-9]+([Mm]|[Gg])")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("invalid-memory-format")));
                }
                else {
                    int memStringToInt = this.pl.utils.memStringToInt(this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[1]);
                    if (array[2].matches("(\\+|-)[0-9]+([Mm]|[Gg])")) {
                        String replaceAll = array[2].replaceAll("[A-Za-z0-9]", "");
                        String s;
                        if (replaceAll.matches("\\+")) {
                            s = String.valueOf(memStringToInt + this.pl.utils.memStringToInt(array[2].replaceAll("\\+", ""))) + "M";
                        }
                        else {
                            if (!replaceAll.matches("-")) {
                                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("invalid-memory-format")));
                                return;
                            }
                            s = String.valueOf(memStringToInt - this.pl.utils.memStringToInt(array[2].replaceAll("-", ""))) + "M";
                        }
                        if (this.pl.utils.memStringToInt(s) > this.pl.utils.memStringToInt(this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[0])) {
                            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("start-greater-max")));
                            return;
                        }
                        this.pl.serverManager.setServerInfo(uuid, "memory", this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[1] + "/" + s);
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("start-memory-changed")));
                    }
                    else {
                        this.pl.serverManager.setServerInfo(uuid, "memory", this.pl.serverManager.serverMap.get(uuid).fromHashMap().get("memory").split("\\/")[0] + "/" + array[2]);
                        this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("start-memory-changed")));
                    }
                }
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("other-no-server"));
            }
        }
    }
    
    public void templatesCommand(CommandSender commandSender, String[] array) {
        if (this.pl.templateManager.templates.size() < 1) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-templates-found"));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("available-templates"));
            Iterator<File> iterator = this.pl.templateManager.templates.keySet().iterator();
            while (iterator.hasNext()) {
                this.pl.utils.sendMsg(commandSender, "&e&o" + this.pl.templateManager.getTemplateSetting(iterator.next(), "template-name"));
            }
        }
    }
    
    public void reloadCommand(CommandSender commandSender, String[] array) {
    	PSReloadEvent event = new PSReloadEvent();
    	this.pl.proxy.getPluginManager().callEvent(event);
        this.pl.reload();
        this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("config-reloaded"));
    }
    
    public void killCommand(CommandSender commandSender, String[] array) {
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else {
            String uuid = this.pl.utils.getUUID(array[1]);
            if (this.pl.serverManager.hasServer(uuid)) {
                this.pl.serverManager.killServer(uuid);
            }
            else {
                this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("other-no-server")));
            }
        }
    }
    
    public void motdCommand(CommandSender commandSender, String[] array) {
        String uuid = this.pl.utils.getUUID(array[1]);
        if (!this.pl.serverManager.hasServer(uuid)) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("other-no-server"));
            return;
        }
        if (array.length < 2) {
            this.pl.utils.sendMsg(commandSender, this.pl.msgMap.get("no-player-specified"));
        }
        else if (array.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < array.length; ++i) {
                sb.append(array[i]).append(" ");
            }
            if (sb.toString().endsWith(" ")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            this.pl.serverManager.setServerInfo(uuid, "motd", sb.toString());
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("motd-changed")));
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.utils.doPlaceholders(uuid, this.pl.msgMap.get("motd-display")));
        }
    }
    
    public void debugCommand(CommandSender commandSender, String[] array) {
        if (array.length > 1 && array[1].equalsIgnoreCase("toggle")) {
            if (!this.pl.debug) {
                this.pl.utils.sendMsg(commandSender, "&aEnabled debug mode");
                this.pl.debug = true;
            }
            else {
                this.pl.utils.sendMsg(commandSender, "&cDisabled debug mode");
                this.pl.debug = false;
            }
            return;
        }
        this.pl.proxy.getScheduler().runAsync((Plugin)this.pl, (Runnable)new Runnable() {
            @Override
            public void run() {
                pl.utils.sendMsg(commandSender, "&aGathering Server Info...");
                long currentTimeMillis = System.currentTimeMillis();
                String string = "Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version");
                String property = System.getProperty("line.separator");
                String s = "============================================";
                String string2 = "Bungee Version: " + pl.proxy.getVersion();
                String string3 = "Java Version: " + System.getProperty("java.version");
                String string4 = "PlayerServers Version: " + pl.getDescription().getVersion();
                Writer writer = null;
                StringBuilder sb = new StringBuilder();
                sb.append("PlayerServers Debugger Output").append(property).append(property);
                sb.append("Time: " + currentTimeMillis).append(property);
                try {
                    File file = new File(pl.getDataFolder() + File.separator + "debug");
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    File file2 = new File(file, "debug-" + currentTimeMillis + ".txt");
                    if (!file2.exists()) {
                        file2.createNewFile();
                    }
                    writer = new BufferedWriter(new FileWriter(file2.getAbsoluteFile(), true));
                    writer.write("PlayerServers Debugger Output" + property + property);
                    writer.write("Time: " + currentTimeMillis + property);
                    writer.write(string4 + property);
                    writer.write(string3 + property);
                    writer.write(string + property);
                    writer.write(string2 + " " + pl.vers + property);
                    writer.write("Bungee Online Mode: " + pl.proxy.getConfig().isOnlineMode() + property);
                    writer.write("External Proxy Address: " + pl.proxyAddress + property + property);
                    writer.write("Proxy Listeners:" + property);
                    for (ListenerInfo listenerInfo : pl.proxy.getConfig().getListeners()) {
                        writer.write(" +++-Hostname: " + listenerInfo.getHost().getHostName() + property);
                        writer.write(" | |-Host Address: " + listenerInfo.getHost().getHostString() + property);
                        writer.write(" | |-Bind to Local:" + listenerInfo.isSetLocalAddress() + property);
                        writer.write(" | |-Query Port: " + listenerInfo.getQueryPort() + property);
                        writer.write(" | |-Query Enabled: " + listenerInfo.isQueryEnabled() + property);
                        writer.write(" | |-MOTD: " + listenerInfo.getMotd() + property);
                        writer.write(" | |-Default Server: " + listenerInfo.getDefaultServer() + property);
                        writer.write(" | |-Fallback Server: " + listenerInfo.getFallbackServer() + property);
                        writer.write(" | |-Force Default Server: " + listenerInfo.isForceDefault() + property);
                        writer.write(" | |-Ping Passthrough: " + listenerInfo.isPingPassthrough() + property);
                        writer.write(" | |-Forced Servers:" + property);
                        for (Map.Entry<String, String> entry : listenerInfo.getForcedHosts().entrySet()) {
                            writer.write(" | | |-" + entry.getKey() + " : " + (String)entry.getValue() + property);
                        }
                        writer.write(" |===" + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write(" +-BungeeCord Plugins: " + property);
                    for (Plugin plugin : pl.proxy.getPluginManager().getPlugins()) {
                        writer.write(" | |-" + plugin.getDescription().getName() + " VERSION " + plugin.getDescription().getVersion() + " BY " + plugin.getDescription().getAuthor() + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write(s + property + property);
                    writer.write("Config Values:" + property);
                    writer.write("Debug Mode: " + pl.debug + property);
                    writer.write("Prefix: " + pl.prefix + property);
                    writer.write("PS custom command: " + pl.psCommand + property);
                    writer.write("Wrapper: " + pl.wrapper + property);
                    writer.write("Wrapper Address: " + pl.wrapperAddress + " : " + pl.wrapperPort + property);
                    writer.write("'Hub' server: " + pl.fallbackSrv + property);
                    writer.write("Servers Folder: " + pl.serversFolder + property);
                    writer.write("Startup join delay: " + pl.joinDelay + property);
                    writer.write("Online join delay: " + pl.onlineJoinDelay + property);
                    writer.write("Next Port: " + pl.utils.getNextPort() + property);
                    writer.write("Use Expiry: " + pl.useExpiry + property);
                    writer.write("Reset Expiry On Create: " + pl.resetExpiry + property);
                    writer.write("Use Titles: " + pl.useTitles + property);
                    writer.write("Use Startup Queue: " + pl.useQueue + property);
                    writer.write("Global Max Ram: " + pl.globalMaxRam + property);
                    writer.write("Global Max Servers: " + pl.globalMaxServers + property);
                    writer.write("Purge Servers: " + pl.autoPurge + " | Purge Check: " + pl.autoPurgeInterval + "ms | Purge After: " + pl.autoPurgeTime + "ms" + property + property);
                    writer.write("Blocked Commands:" + property);
                    Iterator<String> iterator4 = pl.blockedCmds.iterator();
                    while (iterator4.hasNext()) {
                        writer.write(iterator4.next() + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write("Always OPed:" + property);
                    Iterator<String> iterator5 = pl.alwaysOP.iterator();
                    while (iterator5.hasNext()) {
                        writer.write(iterator5.next() + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write(s + property + property);
                    writer.write("Other Info:" + property);
                    writer.write("Number of stored servers: " + pl.serverManager.serverMap.size() + property);
                    writer.write("Number of stored players: " + pl.playerMap.size() + property + property);
                    writer.write(s + property + property);
                    writer.write("Templates: " + property);
                    for (Map.Entry<File, Configuration> entry2 : pl.templateManager.templates.entrySet()) {
                        File file3 = entry2.getKey();
                        Configuration configuration = entry2.getValue();
                        String string5 = configuration.getString("template-name");
                        String string6 = configuration.getString("icon-material");
                        String string7 = configuration.getString("description");
                        String value = String.valueOf(configuration.getBoolean("creator-gets-op"));
                        boolean boolean1 = configuration.getBoolean("shutdown-on-expire", false);
                        writer.write(" +++-" + string5 + property);
                        writer.write(" | |-Template File: " + file3.getAbsolutePath() + property);
                        writer.write(" | |-Icon: " + string6 + property);
                        writer.write(" | |-Creator OP: " + value + property);
                        writer.write(" | |-Description: " + string7 + property);
                        writer.write(" | |-Default Expiry Time: " + pl.templateManager.getTemplateSetting(file3, "default-expiry-time") + property);
                        writer.write(" | |-Shutdown On Expire: " + boolean1 + property);
                        writer.write(" | |-Creator Join Message: " + pl.templateManager.getTemplateSetting(file3, "creator-join-message") + property);
                        writer.write(" | |-Default Xmx: " + pl.templateManager.getTemplateSetting(file3, "default-Xmx") + property);
                        writer.write(" | |-Default Xms: " + pl.templateManager.getTemplateSetting(file3, "default-Xms") + property);
                        writer.write(" | |-Creator Join Commands:" + property);
                        Iterator<String> iterator7 = pl.templateManager.getTemplateSettingList(file3, "creator-join-commands").iterator();
                        while (iterator7.hasNext()) {
                            writer.write(" | | |- '" + iterator7.next() + "'" + property);
                        }
                        writer.write(" |===" + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write(s + property + property);
                    writer.write("All Servers:" + property);
                    for (Entry<String, ServerInfo> entry3 : pl.proxy.getServers().entrySet()) {
                        ServerInfo serverInfo = entry3.getValue();
                        String name;
                        if (serverInfo == null) {
                            name = (String)entry3.getKey();
                            writer.write(" +++-" + name + property);
                            writer.write(" | |-WARNING: " + name + "'s ServerInfo value is null!" + property);
                            writer.write(" | |-PlayerServer: " + pl.serverManager.isPlayerServer(name) + property);
                        }
                        else {
                            name = serverInfo.getName();
                            writer.write(" +++-" + name + property);
                            writer.write(" | |-PlayerServer: " + pl.serverManager.isPlayerServer(name) + property);
                            writer.write(" | |-Address: " + serverInfo.getAddress() + property);
                            writer.write(" | |-MOTD: " + serverInfo.getMotd() + property);
                            writer.write(" | |-Players: " + serverInfo.getPlayers() + property);
                        }
                        boolean b = false;
                        Iterator<Server> iterator9 = pl.sender.syncedServers.iterator();
                        while (iterator9.hasNext()) {
                            if (iterator9.next().getInfo().equals(serverInfo)) {
                                b = true;
                            }
                        }
                        writer.write(" | |-Synced: " + b + property);
                        if (pl.serverManager.isPlayerServer(name)) {
                            String serverUUID = pl.utils.getServerUUID(name);
                            ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
                            File file4 = new File(pl.serversFolder + File.separator + serverUUID + File.separator + "spigot.yml");
                            if (file4.exists()) {
                                Configuration load = null;
                                try {
                                    load = provider.load(file4);
                                }
                                catch (IOException ex) {
                                    pl.utils.log(Level.SEVERE, "Failed to load spigot.yml file even though it exists! Please send this stack trace to the developer.");
                                    writer.write(" | |-BungeeCord: SPIGOT.YML FAILED TO LOAD.");
                                    ex.printStackTrace();
                                }
                                if (load != null) {
                                    writer.write(" | |-BungeeCord: " + load.getBoolean("settings.bungeecord"));
                                }
                            }
                            else {
                                writer.write(" | |-BungeeCord: SPIGOT.YML NOT FOUND");
                            }
                            ((BufferedWriter)writer).newLine();
                            File file5 = new File(pl.serversFolder + File.separator + serverUUID + File.separator + "bukkit.yml");
                            if (file5.exists()) {
                                Configuration load2 = null;
                                try {
                                    load2 = provider.load(file5);
                                }
                                catch (IOException ex2) {
                                    pl.utils.log(Level.SEVERE, "Failed to load bukkit.yml file even though it exists! Please send this stack trace to the developer.");
                                    writer.write(" | |-Connection Throttle: BUKKIT.YML FAILED TO LOAD.");
                                    ex2.printStackTrace();
                                }
                                if (load2 != null) {
                                    writer.write(" | |-Connection Throttle: " + load2.getInt("settings.connection throttle"));
                                }
                            }
                            else {
                                writer.write(" | |-Connection Throttle: BUKKIT.YML NOT FOUND");
                            }
                        }
                        else {
                            writer.write(" | |-BungeeCord: Unknown (not a player server)" + property);
                            writer.write(" | |-Connection Throttle: Unknown (not a player server)");
                        }
                        ((BufferedWriter)writer).newLine();
                        if (pl.serverManager.isPlayerServer(name)) {
                            writer.write(" | |" + property);
                            File file6 = new File(pl.serversFolder + File.separator + pl.utils.getServerUUID(serverInfo.getName()) + File.separator + "logs" + File.separator + "latest.log");
                            if (!file6.exists()) {
                                writer.write(" | |-WARNING: Spigot latest.log file not found!" + property + property);
                            }
                            else {
                                BufferedReader bufferedReader = new BufferedReader(new FileReader(file6));
                                ArrayList<String> list = new ArrayList<String>();
                                String line;
                                while ((line = bufferedReader.readLine()) != null) {
                                    list.add(line);
                                }
                                bufferedReader.close();
                                writer.write(" | |-First 250 lines of latest.log:" + property + " | | |" + property);
                                for (int n = 0; n <= list.size() - 1 && n <= 250; ++n) {
                                    writer.write(" | | |-" + list.get(n) + property);
                                }
                                writer.write(" | |" + property);
                                if (list.size() > 150) {
                                    writer.write(" | |-Last 150 lines of latest.log:" + property + " | | |" + property);
                                    for (int n2 = list.size() - 151; n2 > -1 && n2 <= list.size() - 1; ++n2) {
                                        writer.write(" | | |-" + list.get(n2) + property);
                                    }
                                    writer.write(" | |" + property);
                                }
                            }
                        }
                        writer.write(" |===" + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write(s + property + property);
                    writer.write("Online Players: " + property);
                    for (ProxiedPlayer proxiedPlayer : proxy.getPlayers()) {
                        writer.write(" +++-" + proxiedPlayer.getName() + " | " + proxiedPlayer.getUniqueId() + " | Server: " + proxiedPlayer.getServer().getInfo().getName() + property);
                        writer.write(" |-Permissions:" + property);
                        if (pl.permMap.containsKey(proxiedPlayer.getUniqueId().toString())) {
                            for (Map.Entry<String, String> entry4 : pl.permMap.get(proxiedPlayer.getUniqueId().toString()).entrySet()) {
                                writer.write(" | |-" + entry4.getKey() + " : " + entry4.getValue() + property);
                            }
                        }
                        else {
                            writer.write(" |-WARNING: Player has no stored permissions! Is the server they're on configured correctly for BungeeCord?" + property);
                        }
                        writer.write(" |===" + property);
                    }
                    ((BufferedWriter)writer).newLine();
                    writer.write(s + property + property);
                    writer.write("Log File Snippets:" + property + property);
                    File file7 = new File("proxy.log.0");
                    if (!file7.exists()) {
                        writer.write("BungeeCord log file not found!" + property + property);
                    }
                    else {
                        BufferedReader bufferedReader2 = new BufferedReader(new FileReader(file7));
                        ArrayList<String> list2 = new ArrayList<String>();
                        ArrayList<Integer> list3 = new ArrayList<Integer>();
                        String line2;
                        while ((line2 = bufferedReader2.readLine()) != null) {
                            list2.add(line2);
                            if (line2.matches("(.*)?Enabled BungeeCord version(.*)?")) {
                                int n3 = list2.size() - 1;
                                if (list3.size() > 0 && n3 > list3.get(0)) {
                                    list3.add(0, n3);
                                }
                                else {
                                    list3.add(n3);
                                }
                            }
                        }
                        bufferedReader2.close();
                        if (list3.size() > 0) {
                            writer.write("BungeeCord proxy.log.0, first 250 lines from latest startup: " + property + property);
                        }
                        else {
                            writer.write("BungeeCord proxy.log.0, first 100 lines from beginning of file: " + property + property);
                        }
                        for (int n4 = (list3.size() > 0) ? list3.get(0) : 0; n4 <= list2.size() - 1 && n4 <= ((list3.size() > 0) ? (list3.get(0) + 250) : 100); ++n4) {
                            writer.write(list2.get(n4) + property);
                        }
                        ((BufferedWriter)writer).newLine();
                        ((BufferedWriter)writer).newLine();
                        if (list2.size() > 201) {
                            writer.write("BungeeCord proxy.log.0, last 200 lines: " + property + property);
                            for (int n5 = list2.size() - 201; n5 > -1 && n5 <= list2.size() - 1; ++n5) {
                                writer.write(list2.get(n5) + property);
                            }
                            ((BufferedWriter)writer).newLine();
                        }
                        writer.write(s + property + property);
                    }
                    pl.utils.sendMsg(commandSender, "&aDone! File saved to " + file2.getAbsolutePath());
                    pl.utils.sendMsg(commandSender, "&aHastbin: " + uploadToHastebin(file2));
                }
                catch (IOException ex3) {
                    pl.utils.sendMsg(commandSender, "&cFailed while creating/writing debug file!");
                    pl.utils.log(Level.SEVERE, "Failed while generating debug file at " + pl.getDataFolder() + File.separator + "debug" + File.separator + "debug-" + currentTimeMillis + ".txt");
                    ex3.printStackTrace();
                }
                finally {
                    try {
                        if (writer != null) {
                            ((BufferedWriter)writer).flush();
                            ((BufferedWriter)writer).close();
                        }
                    }
                    catch (IOException ex4) {}
                }
            }
        });
    }
    public static String uploadToHastebin(File file) {
    	String hastebinURL;
        try {
            // Read file contents
            StringBuilder fileContent = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(new String(fileContent.toString()));

            URL url = new URL("https://hastebin.com/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain");
            //YzFiY2FlMGJiZjgyMWE4MGVmMmI2YjVkMzA3OTk4OTZhMGQ1OGVkZWU0YjI2NTA5MDc4MGU5MjBiMDVjODBkZTAzYTNhM2ViNWYzNGY0MDVlYTQzOTU2NDFkMDBmMDQ2NzVkMjI0ZTQzNDc5N2ZjMDFmMGJiNDRjZGVhMGJlOTI=
            connection.setRequestProperty("Authorization", "Bearer c1bcae0bbf821a80ef2b6b5d30799896a0d58edee4b265090780e920b05c80de03a3a3eb5f34f405ea4395641d00f04675d224e434797fc01f0bb44cdea0be92");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(jsonBody.getBytes());
            os.flush();

            // Read response
            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            // Parse response JSON
            String key = mapper.readTree(response.toString()).get("key").asText();
            hastebinURL = "https://hastebin.com/share/" + key;

            // Clean up
            os.close();
            br.close();
            connection.disconnect();
        } catch (Exception e) {
        	hastebinURL = null;
            e.printStackTrace();
        }
        return hastebinURL;
    }
}
