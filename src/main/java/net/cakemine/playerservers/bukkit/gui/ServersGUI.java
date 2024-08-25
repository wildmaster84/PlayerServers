package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import java.util.logging.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import java.util.*;
import org.bukkit.event.*;

public class ServersGUI extends CustomGUI {
    private final PlayerServers pl;

    public ServersGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    public String getValue(String uuid, String key) {
        if (this.pl.servers.containsKey(uuid) && this.pl.servers.get(uuid).containsKey(key)) {
            return this.pl.servers.get(uuid).get(key);
        }
        return "???";
    }

    public String doPlaceholders(OfflinePlayer offlinePlayer, String message) {
        String uuid = offlinePlayer.getUniqueId().toString();
        try {
            message = message
                    .replace("%player-uuid%", uuid)
                    .replace("%player%", this.getValue(uuid, "owner-name"))
                    .replace("%current-players%", this.getValue(uuid, "current-players"))
                    .replace("%max-players%", this.getValue(uuid, "max-players"))
                    .replace("%motd%", this.getValue(uuid, "motd"))
                    .replace("%template-name%", this.getValue(uuid, "template-name"))
                    .replace("%expire-date%", this.getValue(uuid, "expire-date"))
                    .replace("%time-left%", this.getValue(uuid, "time-left"))
                    .replace("%whitelist%", this.getValue(uuid, "white-listed").equalsIgnoreCase("true") ? "on" : "off");
        } catch (NullPointerException ex) {
            this.pl.utils.log(Level.WARNING, "Failed to replace placeholder in message: " + message + " Exception: " + ex);
        }
        return message;
    }

    public ItemStack buildServer(OfflinePlayer offlinePlayer) {
        ItemStack itemStack = new ItemStack(this.getItem("server"));
        itemStack.setType(Material.LEGACY_SKULL_ITEM);
        itemStack.setDurability((short) 3);
        SkullMeta itemMeta = (SkullMeta) itemStack.getItemMeta();
        String uuid = offlinePlayer.getUniqueId().toString();
        this.pl.utils.debug("Getting server info for '" + uuid + "' from servers map: " + this.pl.servers);

        itemMeta.setDisplayName(this.pl.utils.color("&e&l&o" + this.getValue(uuid, "server-name")));
        itemMeta.setOwner(offlinePlayer.getName());
        
        List<String> lore = new ArrayList<>();
        if (itemMeta.getLore() != null) {
            for (String line : itemMeta.getLore()) {
                lore.add(this.pl.utils.color(this.doPlaceholders(offlinePlayer, line)));
            }
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        this.getListedPlayers().put(offlinePlayer.getName(), itemStack);
        return itemStack;
    }

    @Override
    public void openWithExtras(Player player, Inventory inventory, int rows, HashMap<String, HashMap<String, String>> servers) {
        this.pl.servers = servers;
        int size = servers.size();
        this.pl.utils.debug("Server count = " + size + " | Servers = " + servers.values());
        
        int slots = 9 * Math.min((int) Math.ceil(size / 9.0), 6);  // Calculate inventory size
        boolean paginated = size > 54;

        inventory = reopenInventory(player, inventory, slots / 9, this.getTitle());
        this.fillInventory(inventory);
        
        if (size < 1) {
            inventory.setItem(4, this.getItem("none-online"));
            return;
        }

        List<String> serverKeys = new ArrayList<>(servers.keySet());
        int start = paginated ? (rows - 1) * 44 : 0;
        int end = paginated ? Math.min(start + 44, size) : size;

        for (int i = start, slot = 0; i < end; i++, slot++) {
            OfflinePlayer serverOwner = Bukkit.getOfflinePlayer(UUID.fromString(serverKeys.get(i)));
            inventory.setItem(slot, this.buildServer(serverOwner));
        }

        if (paginated) {
            if (rows > 1) {
                inventory.setItem(45, this.buildArrow(false, rows - 1));
            }
            if (end < size) {
                inventory.setItem(53, this.buildArrow(true, rows + 1));
            }
        }
        
        player.openInventory(inventory);
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        String inventoryTitle = this.pl.utils.stripColor(event.getView().getTitle());
        Inventory inventory = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        GuiClickEvent guiClickEvent = new GuiClickEvent(this.pl, player, inventory, inventoryTitle, currentItem);

        if (inventoryTitle.equalsIgnoreCase(this.pl.utils.stripColor(this.getTitle()))) {
            Bukkit.getPluginManager().callEvent(guiClickEvent);
            if (!guiClickEvent.isCancelled()) {
                event.setCancelled(true);
                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    this.closeInventory(player);
                } else {
                    String clickedName = this.pl.utils.stripColor(currentItem.getItemMeta().getDisplayName());
                    if (!this.getFillItem().equals(currentItem)) {
                        for (Map.Entry<String, HashMap<String, String>> entry : this.pl.servers.entrySet()) {
                            if (this.pl.utils.stripColor(entry.getValue().get("server-name")).equals(clickedName)) {
                                this.pl.psCmd.psJoin(player.getUniqueId().toString(), entry.getValue().get("owner-name"));
                                break;
                            }
                        }
                        this.closeInventory(player);
                    }
                }
            }
        }
    }
}
