package net.cakemine.playerservers.bukkit;

import net.cakemine.playerservers.bukkit.sync.*;
import org.bukkit.event.inventory.*;
import org.bukkit.entity.*;
import net.cakemine.playerservers.bukkit.gui.*;
import org.bukkit.inventory.*;
import org.bukkit.*;
import org.bukkit.BanList.Type;

import java.util.logging.*;
import org.bukkit.event.*;
import org.bukkit.plugin.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.command.*;
import org.bukkit.event.player.*;
import java.util.regex.*;
import org.bukkit.event.server.*;
import org.bukkit.event.block.*;
import org.bukkit.block.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.event.vehicle.*;
import java.util.*;

public class PlayerListener implements Listener
{
    PlayerServers pl;
    PluginSender pSend;
    public boolean syncDone;
    public static List<String> blockedCmds;
    public static List<String> alwaysOps;
    public static List<String> consoleCmds = new ArrayList<>();;
    
    public PlayerListener(PlayerServers pl) {
        this.syncDone = false;
        this.pl = pl;
        this.pSend = new PluginSender(this.pl);
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        String stripColor = this.pl.utils.stripColor(inventoryClickEvent.getView().getTitle());
        Inventory inventory = inventoryClickEvent.getInventory();
        ItemStack currentItem = inventoryClickEvent.getCurrentItem();
        Player player = (Player)inventoryClickEvent.getWhoClicked();
        Iterator<CustomGUI> iterator = this.pl.gui.getGUIS().values().iterator();
        while (iterator.hasNext()) {
            if (this.pl.utils.stripColor(iterator.next().getTitle()).equalsIgnoreCase(stripColor)) {
                CustomGuiClickEvent customGuiClickEvent = new CustomGuiClickEvent(this.pl, player, inventory, stripColor, currentItem);
                Bukkit.getPluginManager().callEvent((Event)customGuiClickEvent);
                if (customGuiClickEvent.isCancelled()) {
                    inventoryClickEvent.setCancelled(true);
                    return;
                }
                continue;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void join(PlayerLoginEvent playerLoginEvent) {
        if (this.pl.isSlave()) {
            Player player = playerLoginEvent.getPlayer();
            if (this.pl.utils.getOwnerId().equals(player.getUniqueId().toString()) && player.isBanned()) {
                BanList banList = Bukkit.getServer().getBanList(Type.NAME);
                banList.pardon(player.getName());
                this.pl.utils.log(Level.WARNING, "Server creator was banned, automatically unbanned them!");
            }
            
            player.setGameMode(player.getServer().getDefaultGameMode());
        }
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        if (!this.syncDone) {
            Scheduler.runTaskLater((Plugin)this.pl, () -> {
            	PlayerListener.this.pSend.doSync(player);
            }, 20L);
        }
        if (this.pl.isSlave()) {
            if (this.pl.utils.getOwnerId().equals(player.getUniqueId().toString())) {
                CreatorJoinEvent creatorJoinEvent = new CreatorJoinEvent(this.pl, player);
                Bukkit.getPluginManager().callEvent((Event)creatorJoinEvent);
                if (!creatorJoinEvent.isCancelled()) {
                    player.setWhitelisted(true);
                    if (this.pl.getOPCheck()) {
                        playerJoinEvent.setJoinMessage((String)null);
                        this.pl.utils.broadcast(this.pl.ownerJoinMsg.replaceAll("%player%", player.getDisplayName()));
                        player.setOp(true);
                    }
                    for (String s : this.pl.ownerJoinCmds) {
                        if (s.startsWith("#")) {
                            continue;
                        }
                        if (s.contains("%owner-name%")) {
                            s = s.replaceAll("%owner-name%", player.getPlayer().getName());
                        }
                        if (s.contains("%owner-uuid%")) {
                            s = s.replaceAll("%owner-uuid%", player.getPlayer().getUniqueId().toString());
                        }
                        if (s.contains("%server-name%")) {
                            s = s.replaceAll("%server-name%", this.pl.getServer().getName());
                        }
                        if (s.contains("%server-port%")) {
                            s = s.replaceAll("%server-port%", String.valueOf(this.pl.getServer().getPort()));
                        }
                        if (s.contains("%template-name%")) {
                            s = s.replaceAll("%template-name%", this.pl.psrvCfg.getString("template-name"));
                        }
                        if (s.startsWith("/")) {
                            s = s.replaceAll("^\\/", "");
                        }
                        this.pl.getServer().dispatchCommand((CommandSender)this.pl.getServer().getConsoleSender(), s);
                    }
                }
            }
        }
        else {
            this.pSend.sendPerms(player);
        }
        Scheduler.runTaskLater((Plugin)this.pl, () -> {
        	PlayerListener.this.pSend.helperAnnounce(player);
        }, 20L);
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        if (this.pl.isSlave()) {
            this.pl.checkUseless();
        }
    }
    
    @EventHandler
    public void onKick(PlayerKickEvent playerKickEvent) {
        if (this.pl.isSlave()) {
            this.pl.checkUseless();
        }
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        if (this.pl.isSlave()) {
            Player player = playerCommandPreprocessEvent.getPlayer();
            String string = playerCommandPreprocessEvent.getMessage().toString();
            for (String s : PlayerListener.blockedCmds) {
                Pattern pattern;
                if (s.contains("(?i)")) {
                    pattern = Pattern.compile(s);
                }
                else {
                    pattern = Pattern.compile("(?i)" + s);
                }
                Matcher matcher = pattern.matcher(string);
                if (matcher.find()) {
                    this.pl.utils.log("Blocked a command from " + player.getName() + " »" + string);
                    this.pl.utils.sendMsg(player, this.pl.messages.get("blocked-cmd").replaceAll("(%blocked-command%)", matcher.group(0)));
                    playerCommandPreprocessEvent.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onServercommand(ServerCommandEvent serverCommandEvent) {
        if (this.pl.isSlave()) {
            String string = serverCommandEvent.getCommand().toString();
            for (String s : PlayerListener.blockedCmds) {
                Pattern pattern;
                if (s.contains("(?i)")) {
                    pattern = Pattern.compile(s);
                }
                else {
                    pattern = Pattern.compile("(?i)" + s);
                }
                if (pattern.matcher(string).find() && !serverCommandEvent.getSender().getName().equals("CONSOLE")) {
                	this.pl.utils.log("Blocked server command » " + string);
                    serverCommandEvent.setCommand("/say @p Command Blocked!");
                }
            }
        }
    }
    
    @EventHandler
    public void redstoneChanges(BlockRedstoneEvent blockRedstoneEvent) {
        if (this.pl.isSlave()) {
            Block block = blockRedstoneEvent.getBlock();
            if (blockRedstoneEvent.getOldCurrent() < blockRedstoneEvent.getNewCurrent() && block != null) {
                BlockState state = block.getState();
                if (state != null && state instanceof CommandBlock) {
                    CommandBlock commandBlock = (CommandBlock)state;
                    for (String s : PlayerListener.blockedCmds) {
                        Pattern pattern;
                        if (s.contains("(?i)")) {
                            pattern = Pattern.compile(s);
                        }
                        else {
                            pattern = Pattern.compile("(?i)" + s);
                        }
                        Matcher matcher = pattern.matcher(commandBlock.getCommand());
                        if (matcher.find()) {
                            this.pl.utils.log("Command " + matcher.group(0) + " attempted from CommandBlock. Blocked.");
                            commandBlock.setCommand("/say @p Command Blocked!");
                            commandBlock.setName("Blocked Command!");
                            commandBlock.update();
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent vehicleMoveEvent) {
        if (this.pl.isSlave() && vehicleMoveEvent.getVehicle() instanceof CommandMinecart) {
            CommandMinecart commandMinecart = (CommandMinecart)vehicleMoveEvent.getVehicle();
            for (String s : PlayerListener.blockedCmds) {
                Pattern pattern;
                if (s.contains("(?i)")) {
                    pattern = Pattern.compile(s);
                }
                else {
                    pattern = Pattern.compile("(?i)" + s);
                }
                Matcher matcher = pattern.matcher(commandMinecart.getCommand());
                if (matcher.find()) {
                    this.pl.utils.log("Command " + matcher.group(0) + " attempted from CommandMinecart. Blocked.");
                    commandMinecart.setCommand("/say @p Command Blocked!");
                    commandMinecart.setName("Blocked Command!");
                }
            }
        }
    }
    
    @EventHandler
    public void onVehicleUpdate(VehicleUpdateEvent vehicleUpdateEvent) {
        if (this.pl.isSlave() && vehicleUpdateEvent.getVehicle() instanceof CommandMinecart) {
            CommandMinecart commandMinecart = (CommandMinecart)vehicleUpdateEvent.getVehicle();
            if (!commandMinecart.getCommand().contains("/say @p Command Blocked!")) {
                for (String s : PlayerListener.blockedCmds) {
                    Pattern pattern;
                    if (s.contains("(?i)")) {
                        pattern = Pattern.compile(s);
                    }
                    else {
                        pattern = Pattern.compile("(?i)" + s);
                    }
                    Matcher matcher = pattern.matcher(commandMinecart.getCommand());
                    if (matcher.find()) {
                        this.pl.utils.log("Command " + matcher.group(0) + " attempted from CommandMinecart. Blocked.");
                        commandMinecart.setCommand("/say @p Command Blocked!");
                        commandMinecart.setName("Blocked Command!");
                    }
                }
            }
        }
    }
    
    static {
        PlayerListener.blockedCmds = new ArrayList<String>();
        PlayerListener.alwaysOps = new ArrayList<String>();
    }
}
