package net.cakemine.playerservers.bukkit.commands;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.permissions.*;
import org.bukkit.*;
import org.bukkit.BanList.Type;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.*;
import java.util.*;

public class MyServer implements CommandExecutor
{
    PlayerServers pl;
    
    public MyServer(PlayerServers pl) {
        this.pl = pl;
    }
    
    public void sendHelp(CommandSender commandSender) {
        if (commandSender instanceof Player) {
            Player player = (Player)commandSender;
            boolean hasPermission = player.hasPermission("playerservers.myserver");
            this.pl.utils.sendMsg(player, this.pl.messages.get("help-mys-header"));
            this.pl.utils.helpMessage(player, "/mys settings", this.pl.messages.get("help-mys-settings"));
            if (hasPermission || player.hasPermission("playerservers.myserver.ban")) {
                this.pl.utils.helpMessage(player, "/mys ban/unban <player>", this.pl.messages.get("help-mys-ban"));
            }
            if (hasPermission || player.hasPermission("playerservers.myserver.kick")) {
                this.pl.utils.helpMessage(player, "/mys kick <player>", this.pl.messages.get("help-mys-kick"));
            }
            if (hasPermission || player.hasPermission("playerservers.myserver.whitelist")) {
                this.pl.utils.helpMessage(player, "/mys whitelist <on/off/list/add/remove> [player]", this.pl.messages.get("help-mys-whitelist"));
            }
            this.pl.utils.helpMessage(player, "/mys op/deop <player>", this.pl.messages.get("help-mys-op"));
            if (this.pl.utils.getOwnerId().equals(player.getUniqueId().toString())) {
                this.pl.utils.helpMessage(player, "/mys regain", this.pl.messages.get("help-mys-regain"));
            }
            if (hasPermission || player.hasPermission("playerservers.myserver.stop")) {
                this.pl.utils.helpMessage(player, "/mys stop", this.pl.messages.get("help-mys-stop"));
            }
        }
        else {
            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("only-player-use"));
        }
    }
    
    private boolean canUse(CommandSender commandSender) {
        Iterator<PermissionAttachmentInfo> iterator = commandSender.getEffectivePermissions().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getPermission().matches("(?i)(playerservers.myserver)(.+)?")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean useGUI(CommandSender commandSender) {
        String[] array = { "myserver", "myserver.gamemode", "myserver.difficulty", "myserver.pvp", "myserver.whitelist", "myserver.ban", "myserver.kick", "myserver.gamemode", "myserver.worldsettings" };
        for (int length = array.length, i = 0; i < length; ++i) {
            if (commandSender.hasPermission("playerservers." + array[i])) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] array) {
        if (!(commandSender instanceof Player)) {
            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("only-player-use"));
            return true;
        }
        Player player = (Player)commandSender;
        if ((!this.pl.getOPCheck() || !this.pl.utils.getOwnerId().equalsIgnoreCase(((Player)commandSender).getUniqueId().toString())) && !this.canUse(commandSender)) {
            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
            return true;
        }
        if (array.length < 1) {
            if (!this.useGUI(commandSender)) {
                this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
            }
            else {
                this.pl.gui.getGUI("settings").open(player, null);
            }
            return true;
        }
        String s2 = array[0];
        switch (s2) {
            case "h":
            case "help": {
                this.sendHelp(commandSender);
                break;
            }
            case "set":
            case "settings": {
                if (array.length != 1) {
                    String s3 = array[1];
                    int n2 = -1;
                    switch (s3.hashCode()) {
                        case -1768407915: {
                            if (s3.equals("gamemode")) {
                                n2 = 0;
                                break;
                            }
                            break;
                        }
                        case 1829500859: {
                            if (s3.equals("difficulty")) {
                                n2 = 1;
                                break;
                            }
                            break;
                        }
                        case 111402: {
                            if (s3.equals("pvp")) {
                                n2 = 2;
                                break;
                            }
                            break;
                        }
                        case -1653850041: {
                            if (s3.equals("whitelist")) {
                                n2 = 3;
                                break;
                            }
                            break;
                        }
                    }
                    switch (n2) {
	                    case 0: {
	                        if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.gamemode")) {
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("s") || array[2].equalsIgnoreCase("surv") || array[2].equalsIgnoreCase("survival") || array[2].equalsIgnoreCase("0")) {
	                            this.pl.settingsManager.setGamemode(3);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("c") || array[2].equalsIgnoreCase("crea") || array[2].equalsIgnoreCase("creative") || array[2].equalsIgnoreCase("1")) {
	                            this.pl.settingsManager.setGamemode(1);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("a") || array[2].equalsIgnoreCase("adve") || array[2].equalsIgnoreCase("adventure") || array[2].equalsIgnoreCase("2")) {
	                            this.pl.settingsManager.setGamemode(2);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("sp") || array[2].equalsIgnoreCase("spec") || array[2].equalsIgnoreCase("spectate") || array[2].equalsIgnoreCase("spectator") || array[2].equalsIgnoreCase("3")) {
	                            this.pl.settingsManager.setGamemode(3);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("gamemode-changed").replaceAll("%gamemode%", this.pl.getServer().getDefaultGameMode().name()));
	                            break;
	                        }
	                        this.pl.utils.sendMsg(commandSender, "&cInvalid gamemode option. &eValid options:");
	                        this.pl.utils.sendMsg(commandSender, "&esurvival, creative, adventure, spectate");
	                        break;
	                    }
	                    case 1: {
	                        if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.difficulty")) {
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("peaceful") || array[2].equalsIgnoreCase("0") || array[2].equalsIgnoreCase("peace")) {
	                            this.pl.settingsManager.setDifficulty(0);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.PEACEFUL.name()));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("easy") || array[2].equalsIgnoreCase("1") || array[2].equalsIgnoreCase("ez")) {
	                            this.pl.settingsManager.setDifficulty(1);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.EASY.name()));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("normal") || array[2].equalsIgnoreCase("2") || array[2].equalsIgnoreCase("norm")) {
	                            this.pl.settingsManager.setDifficulty(2);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.NORMAL.name()));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("hard") || array[2].equalsIgnoreCase("3") || array[2].equalsIgnoreCase("hd")) {
	                            this.pl.settingsManager.setDifficulty(3);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("difficulty-changed").replaceAll("%difficulty%", Difficulty.HARD.name()));
	                            break;
	                        }
	                        this.pl.utils.sendMsg(commandSender, "&cInvalid difficulty option. &eValid options:");
	                        this.pl.utils.sendMsg(commandSender, "&epeaceful, easy, normal, hard");
	                        break;
	                    }
	                    case 2: {
	                        if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.pvp")) {
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("true") || array[2].equalsIgnoreCase("t") || array[2].equalsIgnoreCase("yes") || array[2].equalsIgnoreCase("allow")) {
	                            this.pl.settingsManager.setPvP(true);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("pvp-enabled"));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("false") || array[2].equalsIgnoreCase("f") || array[2].equalsIgnoreCase("no") || array[2].equalsIgnoreCase("deny")) {
	                            this.pl.settingsManager.setPvP(false);
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("pvp-disabled"));
	                            break;
	                        }
	                        this.pl.utils.sendMsg(commandSender, "&cInvalid pvp option. &eValid options:");
	                        this.pl.utils.sendMsg(commandSender, "&etrue or false");
	                        break;
	                    }
	                    case 3: {
	                        if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.whitelist")) {
	                            this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
	                            break;
	                        }
	                        if (array.length < 3) {
	                            this.pl.utils.sendMsg(player, "&cYou didn't specify any whitelist arguments!");
	                            this.pl.utils.sendMsg(player, "&eValid arguments: &olist, add, remove, on, off");
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("on")) {
	                            Bukkit.setWhitelist(true);
	                            this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-enabled"));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("off")) {
	                            Bukkit.setWhitelist(false);
	                            this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-disabled"));
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("list")) {
	                            Set<OfflinePlayer> whitelistedPlayers = Bukkit.getWhitelistedPlayers();
	                            this.pl.utils.sendMsg(player, "&a&lWhitelisted players on your server:");
	                            Iterator<OfflinePlayer> iterator = whitelistedPlayers.iterator();
	                            while (iterator.hasNext()) {
	                                this.pl.utils.sendMsg(player, "&8� &e" + iterator.next().getName());
	                            }
	                            break;
	                        }
	                        if (array[2].equalsIgnoreCase("add")) {
	                            if (array.length >= 4) {
	                                Bukkit.getOfflinePlayer(array[2]).setWhitelisted(true);
	                                this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-added").replaceAll("%player%", array[2]));
	                                break;
	                            }
	                            this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
	                            break;
	                        }
	                        else {
	                            if (!array[2].equalsIgnoreCase("remove")) {
	                                break;
	                            }
	                            if (array.length >= 4 && array[2].equalsIgnoreCase(player.toString())) {
	                                this.pl.utils.sendMsg(player, "&cYou cannot remove yourself from the server whitelist!");
	                                break;
	                            }
	                            if (array.length >= 4 && !array[2].equalsIgnoreCase(player.toString())) {
	                                Bukkit.getOfflinePlayer(array[2]).setWhitelisted(false);
	                                this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-removed").replaceAll("%player%", array[2]));
	                                break;
	                            }
	                            this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
	                            break;
	                        }
	                    }
	                }
	                this.pl.utils.sendMsg(commandSender, "&cInvalid setting! &aValid settings: &e&osetting (valid options)");
	                this.pl.utils.sendMsg(commandSender, "&egamemode (0-3), difficulty (0-3), pvp (true/false), whitelist (on/off/list/add/remove)");
	                this.pl.utils.sendMsg(commandSender, "&bExample: &6&l/psa setting gamemode 1 &bwill set the server to Creative.");
                        
                    break;
                }
                if (!this.useGUI(commandSender)) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                this.pl.gui.getGUI("settings").open(player, null);
                break;
            }
            case "evict":
            case "ban": {
                if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.ban")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                if (array.length >= 2) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(array[1]);
                    if (offlinePlayer.getName().equalsIgnoreCase(player.getName())) {
                        this.pl.utils.sendMsg(player, "&c&lYou can't ban yourself!");
                    }
                    else if (array.length >= 3) {
                        String s4 = array[2];
                        if (offlinePlayer.isOnline()) {
                            Player player2 = (Player)offlinePlayer;
                            this.pl.utils.sendMsg(player2, this.pl.messages.get("got-banned").replaceAll("%player%", player.getName()).replaceAll("%reason%", s4));
                            this.pl.utils.movePlayer(player2, this.pl.fallbackSrv);
                        }
                        ProfileBanList banList = Bukkit.getServer().getBanList(Type.PROFILE);
                        banList.addBan(player.getPlayerProfile(), "Unspecified reason", Date.from(null), null);
                    }
                    else {
                        if (offlinePlayer.isOnline()) {
                            Player player3 = (Player)offlinePlayer;
                            this.pl.utils.sendMsg(player3, this.pl.messages.get("got-banned").replaceAll("%player%", player.getName()).replaceAll("%reason%", "Unspecified reason"));
                            this.pl.utils.movePlayer(player3, this.pl.fallbackSrv);
                        }
                        ProfileBanList banList = Bukkit.getServer().getBanList(Type.PROFILE);
                        banList.addBan(player.getPlayerProfile(), "Unspecified reason", Date.from(null), null);
                    }
                    this.pl.utils.sendMsg(player, this.pl.messages.get("banned-player").replaceAll("%player%", offlinePlayer.getName()));
                    break;
                }
                this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                break;
            }
            case "unevict":
            case "unban": {
                if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.ban")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                if (array.length >= 2) {
                    String s5 = array[1];
                    ProfileBanList banList = Bukkit.getServer().getBanList(Type.PROFILE);
                    banList.pardon(player.getPlayerProfile());
                    this.pl.utils.sendMsg(player, this.pl.messages.get("unbanned-player").replaceAll("%player%", s5));
                    break;
                }
                this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                break;
            }
            case "kick": {
                if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.kick")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                if (array.length == 2) {
                    for (Player player4 : this.pl.getServer().getOnlinePlayers()) {
                        if (player4.getName().equalsIgnoreCase(array[1])) {
                            if (player4.equals(player)) {
                                this.pl.utils.sendMsg(player, "&c&lYou can't kick yourself!");
                            }
                            else {
                                this.pl.utils.sendMsg(player4, this.pl.messages.get("got-kicked").replaceAll("%player%", player.getName()));
                                this.pl.utils.movePlayer(player4, this.pl.fallbackSrv);
                                this.pl.utils.sendMsg(player, this.pl.messages.get("kicked-player").replaceAll("%player%", player4.getName()));
                            }
                        }
                    }
                    break;
                }
                this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                break;
            }
            case "whitelist": {
                if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.whitelist")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                if (array.length < 2) {
                    this.pl.utils.sendMsg(player, "&cYou didn't specify any whitelist arguments!");
                    this.pl.utils.sendMsg(player, "&eValid arguments: &olist, add, remove, on, off");
                    break;
                }
                if (array[1].equalsIgnoreCase("on")) {
                    Bukkit.setWhitelist(true);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-enabled"));
                    break;
                }
                if (array[1].equalsIgnoreCase("off")) {
                    Bukkit.setWhitelist(false);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-enabled"));
                    break;
                }
                if (array[1].equalsIgnoreCase("list")) {
                    Set<OfflinePlayer> whitelistedPlayers2 = Bukkit.getWhitelistedPlayers();
                    this.pl.utils.sendMsg(player, "&a&lWhitelist:");
                    Iterator<OfflinePlayer> iterator3 = whitelistedPlayers2.iterator();
                    while (iterator3.hasNext()) {
                        this.pl.utils.sendMsg(player, "&8� &e" + iterator3.next().getName());
                    }
                    break;
                }
                if (array[1].equalsIgnoreCase("add")) {
                    if (array.length >= 3) {
                        Bukkit.getOfflinePlayer(array[2]).setWhitelisted(true);
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-added").replaceAll("%player%", array[2]));
                        break;
                    }
                    this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                    break;
                }
                else {
                    if (!array[1].equalsIgnoreCase("remove")) {
                        break;
                    }
                    if (array.length >= 3 && array[2].equalsIgnoreCase(player.toString())) {
                        this.pl.utils.sendMsg(player, "&cYou cannot remove yourself from the server whitelist!");
                        break;
                    }
                    if (array.length >= 3 && !array[2].equalsIgnoreCase(player.toString())) {
                        Bukkit.getOfflinePlayer(array[2]).setWhitelisted(false);
                        this.pl.utils.sendMsg(player, this.pl.messages.get("whitelist-removed").replaceAll("%player%", array[2]));
                        break;
                    }
                    this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                    break;
                }
            }
            case "op": {
                if (commandSender.isOp() || commandSender instanceof ConsoleCommandSender) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                if (array.length >= 2) {
                    for (Player player5 : this.pl.getServer().getOnlinePlayers()) {
                        if (player5.getName().equalsIgnoreCase(array[1])) {
                            if (player5.equals(player)) {
                                this.pl.utils.sendMsg(player, this.pl.messages.get("regain-info"));
                                return true;
                            }
                            player5.setOp(true);
                            this.pl.utils.sendMsg(player, this.pl.messages.get("opped-player").replaceAll("%player%", player5.getName()));
                            return true;
                        }
                    }
                    this.pl.utils.sendMsg(player, this.pl.messages.get("must-be-online"));
                    break;
                }
                this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                break;
            }
            case "deop": {
                if (commandSender.isOp() || commandSender instanceof ConsoleCommandSender) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                if (array.length >= 2) {
                    OfflinePlayer offlinePlayer2 = Bukkit.getOfflinePlayer(array[1]);
                    offlinePlayer2.setOp(false);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("deopped-player").replaceAll("%player%", offlinePlayer2.getName()));
                    break;
                }
                this.pl.utils.sendMsg(player, this.pl.messages.get("no-player-specified"));
                break;
            }
            case "regain": {
                if (this.pl.utils.getOwnerId().equals(player.getUniqueId().toString())) {
                    this.pl.utils.sendMsg(player, "&d&oRegaining control of your Server...");
                    for (OfflinePlayer offlinePlayer3 : Bukkit.getOperators()) {
                        offlinePlayer3.setOp(false);
                        this.pl.utils.sendMsg(player, "&7&oDeopped " + offlinePlayer3.getName());
                    }
                    this.pl.utils.sendMsg(player, "&e&oDone, all players de-opped. OPing you now...");
                    player.setOp(true);
                    break;
                }
                this.pl.utils.sendMsg(player, "&c&lOnly the owner can regain control!");
                break;
            }
            case "bye":
            case "leave": {
                this.pl.utils.sendMsg(player, this.pl.messages.get("leave-message").replaceAll("%server%", this.pl.fallbackSrv));
                this.pl.utils.movePlayer(player, this.pl.fallbackSrv);
                break;
            }
            case "shutdown":
            case "stop": {
                if (!commandSender.hasPermission("playerservers.myserver") && !commandSender.hasPermission("playerservers.myserver.stop")) {
                    this.pl.utils.sendMsg(commandSender, this.pl.messages.get("no-permissions"));
                    break;
                }
                this.pl.utils.shutdown(5);
                break;
            }
            default: {
                this.pl.utils.sendMsg(commandSender, "&c&oInvalid Argument!");
                this.sendHelp(commandSender);
                break;
            }
        }
        return true;
    }
}
