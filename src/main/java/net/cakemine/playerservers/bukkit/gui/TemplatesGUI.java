package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.entity.*;
import java.util.logging.*;
import java.util.*;
import org.bukkit.event.inventory.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.*;

public class TemplatesGUI extends CustomGUI {
    private PlayerServers pl;

    public TemplatesGUI(PlayerServers pl) {
        super(pl);
        this.pl = pl;
    }

    @Override
    public void open(Player player, Inventory inventory, int page) {
        pl.utils.debug("opened TemplatesGUI");
        int size = pl.templates.size();
        pl.utils.debug("templateCount = " + size + " | templates = " + pl.templates);

        int rows = 1;
        boolean paginated = false;

        if (size <= 8) {
            rows = 1;
        } else if (size <= 18) {
            rows = 2;
        } else if (size <= 27) {
            rows = 3;
        } else if (size <= 36) {
            rows = 4;
        } else if (size <= 45) {
            rows = 5;
        } else if (size > 46) {
            rows = 6;
            paginated = true;
        }

        inventory = reopenInventory(player, inventory, rows, getTitle());
        fillInventory(player, inventory, 15);

        if (size <= 1) {
            inventory.setItem(4, getItem("none-found"));
            return;
        }

        int maxSlots = 53;
        int startIndex = (page - 1) * 44;

        if (paginated && rows == 6 && size > 54) {
            if (page > 1) {
                inventory.setItem(45, buildArrow(false, page - 1));
            }
            inventory.setItem(53, buildArrow(true, page + 1));
        }

        int slotIndex = 0;
        for (Map.Entry<String, HashMap<String, String>> entry : pl.templates.entrySet()) {
            if (slotIndex >= startIndex && slotIndex < maxSlots) {
                String templateName = entry.getKey();
                String iconMaterial = entry.getValue().get("icon");
                String description = entry.getValue().get("desc");

                pl.utils.debug("Processing template for GUI: " + templateName);

                int data = 0;
                Material material = Material.matchMaterial(iconMaterial.split(":")[0]);

                if (iconMaterial.contains(":")) {
                    try {
                        data = Integer.parseInt(iconMaterial.split(":")[1]);
                    } catch (NumberFormatException e) {
                        pl.utils.log(Level.WARNING, "Invalid data value for material: " + iconMaterial);
                    }
                }

                if (material == null) {
                    pl.utils.log(Level.WARNING, "Material \"" + iconMaterial + "\" not found! Defaulting to bedrock for \"" + templateName + "\" GUI item.");
                    material = Material.BEDROCK;
                }

                inventory.setItem(slotIndex, item(slotIndex + 1, material, data, "&e&l&o" + templateName, description));
                slotIndex++;
            }
        }

        player.openInventory(inventory);
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        pl.utils.debug("clicked TemplatesGUI");

        String inventoryTitle = pl.utils.stripColor(event.getView().getTitle());
        Inventory inventory = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        if (inventoryTitle.equalsIgnoreCase(pl.utils.stripColor(getTitle()))) {
            GuiClickEvent guiClickEvent = new GuiClickEvent(pl, player, inventory, inventoryTitle, currentItem);
            Bukkit.getPluginManager().callEvent(guiClickEvent);

            if (!guiClickEvent.isCancelled()) {
                event.setCancelled(true);

                if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) {
                    pl.utils.debug("item was null or no metadata");
                    closeInventory(player);
                } else {
                    String displayName = ChatColor.stripColor(pl.utils.color(currentItem.getItemMeta().getDisplayName()));
                    pl.utils.debug("clicked displayName = " + displayName);

                    if (pl.templates.containsKey(displayName)) {
                        pl.utils.debug("contains template");
                        pl.psCmd.psCreate(player.getUniqueId().toString(), displayName.replaceAll("(&|§)[a-fA-Fk-oK-O0-9]", ""));
                        pl.utils.debug("finished");
                        closeInventory(player);
                    }
                }
            }
        }
    }
    
    public ItemStack item(int amount, Material material, int data, String displayName, String lore) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (displayName != null) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        }

        if (lore != null) {
            itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', lore).split("\\|\\|")));
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
