package net.cakemine.playerservers.bukkit.gui;

import net.cakemine.playerservers.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.*;
import net.cakemine.playerservers.bukkit.events.*;
import org.bukkit.enchantments.*;
import java.util.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.*;

public class CustomGUI implements Listener
{
    protected final PlayerServers pl;
    private String title;
    private ItemStack backButton;
    private ItemStack fillItem;
    private final HashMap<String, ItemStack> items;
    private final HashMap<String, ItemStack> listedPlayers;

    public CustomGUI() {
        this(PlayerServers.getApi().getInstance());
    }

    public CustomGUI(PlayerServers pl) {
        this.title = "";
        this.pl = pl;
        this.backButton = this.createItem(1, (Material.matchMaterial("RED_STAINED_GLASS_PANE") == null ? Material.matchMaterial("STAINED_GLASS_PANE") : Material.matchMaterial("RED_STAINED_GLASS_PANE")), 14, "&c&lGo Back.", null);
        this.fillItem = this.createItem(1, (Material.matchMaterial("WHITE_STAINED_GLASS_PANE") == null ? Material.matchMaterial("STAINED_GLASS_PANE") : Material.matchMaterial("WHITE_STAINED_GLASS_PANE")), 0, " ", null);
        this.items = new HashMap<>();
        this.listedPlayers = new HashMap<>();        
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Inventory createInventory(int rows, String title) {
    	if (rows == 0) rows = 1;
        int size = Math.min(rows * 9, 54); // Limit to 54 slots
        return Bukkit.createInventory(null, size, this.pl.utils.color(title));
    }

    public ItemStack createItem(int amount, Material material, int durability, String name, String lore) {
    	if (amount == 0) amount = 1;
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        item.setDurability((short) durability);
        if (name != null) meta.setDisplayName(this.pl.utils.color(name));
        if (lore != null) meta.setLore(Arrays.asList(this.pl.utils.color(lore).split("\\|\\|")));
        item.setItemMeta(meta);
        return item;
    }

    public void fillInventory(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, this.fillItem);
            }
        }
    }
    
    public void fillInventory(Player player, Inventory inventory, int colorDurability) {
        ItemStack fillItem = createItem(1, (Material.matchMaterial("LEGACY_STAINED_GLASS_PANE") == null ? Material.matchMaterial("STAINED_GLASS_PANE") : Material.matchMaterial("LEGACY_STAINED_GLASS_PANE")), colorDurability, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, fillItem);
            }
        }
    }

    public void addBackButtons(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i += 9) {
            inventory.setItem(i, this.backButton);
        }
    }

    public void setBackButton(ItemStack backButton) {
        this.backButton = backButton;
    }

    public ItemStack getBackButton() {
        return this.backButton;
    }

    public void setFillItem(Material material, int durability) {
        this.fillItem = this.createItem(1, material, durability, " ", null);
    }

    public ItemStack getFillItem() {
        return this.fillItem;
    }

    public void addItem(String key, ItemStack item) {
        this.items.put(key, item);
    }

    public ItemStack getItem(String key) {
        return this.items.get(key);
    }

    public void removeItem(String key) {
        this.items.remove(key);
    }

    public HashMap<String, ItemStack> getItems() {
        return this.items;
    }

    public void setSelected(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.THORNS, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    public void closeInventory(Player player) {
        player.closeInventory();
    }

    public Inventory reopenInventory(Player player, Inventory inventory, int rows, String title) {
        if (inventory == null || !ChatColor.stripColor(player.getOpenInventory().getTitle()).equals(ChatColor.stripColor(title))) {
            inventory = createInventory(rows, title);
            GuiOpenEvent event = new GuiOpenEvent(this.pl, player, inventory, rows, title);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                player.openInventory(event.getInventory());
                return event.getInventory();
            }
        }
        return inventory;
    }

    public void open(Player player, Inventory inventory) {
    }

    public void open(Player player, Inventory inventory, int page) {
    }

    public void open(Player player, Inventory inventory, Player target) {
    }

    public void open(Player player, Inventory inventory, int page, HashMap<String, String> data) {
    }
    public void openWithExtras(Player player, Inventory inventory, int rows, HashMap<String, HashMap<String, String>> dataMap) {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        this.pl.utils.debug("Empty InventoryClickEvent called by " + this.getClass().getSimpleName());
    }
    
    public ItemStack buildArrow(boolean forward, int page) {
        ItemStack item = forward ? this.getItem("page-forward") : this.getItem("page-back");
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Collections.singletonList(String.valueOf(page)));
        item.setItemMeta(meta);
        return item;
    }

    // Method to build player skulls for the GUI
    public ItemStack buildPlayer(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.GREEN + player.getName());
        item.setItemMeta(meta);
        listedPlayers.put(player.getName(), item);
        return item;
    }
    
    public ItemStack buildPlayer(Player player, String name, String lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.GREEN + player.getName());
        if (name != null) {
            meta.setDisplayName(this.pl.utils.color(name));
        }
        if (lore != null) {
            meta.setLore(Arrays.asList(this.pl.utils.color(lore).split("(\\|\\||\n|\\n|\\{n\\})")));
        }
        item.setItemMeta(meta);
        listedPlayers.put(player.getName(), item);
        return item;
    }

    // Method to get listed players
    public Map<String, ItemStack> getListedPlayers() {
        return this.listedPlayers;
    }
    
    public ItemStack getFormattedItem(String itemKey, String playerName) {
        // Retrieve the item from the item map
        ItemStack item = this.items.get(itemKey);
        if (item == null) {
            return null;
        }

        // Get the item meta (which contains the display name and lore)
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Replace placeholder in the display name
        if (meta.hasDisplayName()) {
            meta.setDisplayName(meta.getDisplayName().replace("%player%", playerName));
        }

        // Replace placeholders in the lore
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(line.replace("%player%", playerName));
            }
            meta.setLore(formattedLore);
        }

        // Set the modified meta back to the item
        item.setItemMeta(meta);
        return item;
    }
    
    
    // Deprecated methods
    @Deprecated
    public ItemStack playerSkull(int n, Player player, String s, String s2) {
        ItemStack itemStack = new ItemStack(Material.LEGACY_SKULL_ITEM);
        SkullMeta itemMeta = (SkullMeta)itemStack.getItemMeta();
        itemStack.setDurability((short)3);
        itemMeta.setOwner(player.getName());
        if (s != null) {
            itemMeta.setDisplayName(this.pl.utils.color(s));
        }
        if (s2 != null) {
            itemMeta.setLore(Arrays.asList(this.pl.utils.color(s2).split("(\\|\\||\n|\\n|\\{n\\})")));
        }
        itemStack.setItemMeta((ItemMeta)itemMeta);
        return itemStack;
    }
    
    @Deprecated
    public void close(Player player) {
    	player.closeInventory();
    }
    
    @Deprecated
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        this.pl.utils.debug("Empty InventoryClickEvent called by " + this.getClass().getSimpleName());
    }
    
    @Deprecated
    public Inventory reopenGUI(Player player, Inventory inventory, int rows, String title) {
    	return reopenInventory(player, inventory, rows, title);
    }
    
    @Deprecated
    public void simpleFill(Player player, Inventory inventory, int n) {
    	fillInventory(player, inventory, n);
    }
    
    @Deprecated
    public void fill(Inventory inventory) {
    	fillInventory(inventory);
    }
    
    @Deprecated
    public void fill(Player player, Inventory inventory, int colorDurability) {
    	fillInventory(player, inventory, colorDurability);
    }
    
    @Deprecated
    public ItemStack item(int amount, Material material, int durrability, String name, String lore) {
        return createItem(amount, material, durrability, name, lore);
    }
}
