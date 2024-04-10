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
    PlayerServers pl;
    private String title;
    private ItemStack backButton;
    private ItemStack fillItem;
    private Material fillItemMaterial;
    private short fillItemData;
    private HashMap<String, ItemStack> items;
    private HashMap<String, ItemStack> listedPlayers;
    
    public CustomGUI() {
        this.title = "";
        this.backButton = null;
        this.fillItem = null;
        this.fillItemMaterial = Material.WHITE_STAINED_GLASS_PANE;
        this.fillItemData = 0;
        this.items = new HashMap<String, ItemStack>();
        this.listedPlayers = new HashMap<String, ItemStack>();
        this.pl = PlayerServers.getApi().getInstance();
        this.backButton = this.item(0, Material.RED_STAINED_GLASS_PANE, 14, "&c&lGo Back.", null);
        this.fillItem = this.item(0, Material.WHITE_STAINED_GLASS_PANE, 0, " ", null);
        this.pl.getServer().getPluginManager().registerEvents(this, this.pl);
    }
    
    public CustomGUI(PlayerServers pl) {
    	this.title = "";
        this.backButton = null;
        this.fillItem = null;
        this.fillItemMaterial = Material.WHITE_STAINED_GLASS_PANE;
        this.fillItemData = 0;
        this.items = new HashMap<String, ItemStack>();
        this.listedPlayers = new HashMap<String, ItemStack>();
        this.pl = pl;
        this.backButton = this.item(0, Material.RED_STAINED_GLASS_PANE, 14, "&c&lGo Back.", null);
        this.fillItem = this.item(0, Material.WHITE_STAINED_GLASS_PANE, 0, " ", null);
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Inventory newInv(int n, String s) {
        int n2 = n * 9;
        if (n2 > 54) {
            n2 = 54;
        }
        return Bukkit.createInventory(null, n2, this.pl.utils.color(s));
    }
    
    public ItemStack item(int n, Material material, int n2, String s, String s2) {
        ItemStack itemStack = new ItemStack(material, n);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemStack.setDurability((short)n2);
        if (s != null) {
            itemMeta.setDisplayName(this.pl.utils.color(s));
        }
        if (s2 != null) {
            itemMeta.setLore(Arrays.asList(this.pl.utils.color(s2).split("(\\|\\||\n|\\n|\\{n\\})")));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    
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
    
    public Inventory reopenGUI(Player player, Inventory inventory, int slots, String s) {
        String stripColor = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
        if (inventory == null) {
            inventory = this.newInv(slots, s);
            GuiOpenEvent guiOpenEvent = new GuiOpenEvent(this.pl, player, inventory, slots, s);
            Bukkit.getPluginManager().callEvent((Event)guiOpenEvent);
            this.pl.utils.debug("GuiOpenEvent Fired. (nulled)");
            if (!guiOpenEvent.isCancelled()) {
                player.closeInventory();
                if (!inventory.equals(guiOpenEvent.getInventory())) {
                    inventory = guiOpenEvent.getInventory();
                }
                player.openInventory(inventory);
            }
        }
        else if (!ChatColor.stripColor(inventory.getType().name()).equalsIgnoreCase(stripColor)) {
            inventory = this.newInv(slots, s);
            GuiOpenEvent guiOpenEvent = new GuiOpenEvent(this.pl, player, inventory, slots, s);
            Bukkit.getPluginManager().callEvent(guiOpenEvent);
            this.pl.utils.debug("GuiOpenEvent Fired.");
            if (!guiOpenEvent.isCancelled()) {
                player.closeInventory();
                if (!inventory.equals(guiOpenEvent.getInventory())) {
                    inventory = guiOpenEvent.getInventory();
                }
                player.openInventory(inventory);
            }
        }
        return inventory;
    }
    
    public void open(Player player, Inventory inventory) {
    }
    
    public void open(Player player, Inventory inventory, int n) {
    }
    
    public void open(Player player, Inventory inventory, Player player2) {
    }
    
    public void open(Player player, Inventory inventory, int n, String[] array) {
    }
    
    public void open(Player player, Inventory inventory, int n, HashMap hashMap) {
    }
    
    public void close(Player player) {
        player.closeInventory();
    }
    
    public void fill(Inventory inventory) {
        int size = inventory.getSize();
        int n = size / 9;
        int i = 0;
        for (Long n2 = 2L; i < size; ++i, ++n2) {
        	for (int slot = i, rows = n; slot < size && rows > 0; slot += 9, --rows) {
                if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {
                    inventory.setItem(slot, CustomGUI.this.getFillItem());
                }
            }
        }
    }
    
    public void fill(Player player, Inventory inventory, int n) {
        int size = inventory.getSize();
        int n2 = size / 9;
        int i = 0;
        for (Long n3 = 2L; i < size; ++i, ++n3) {
        	for (int slot = i, rows = n2; slot < size && rows > 0; slot += 9, --rows) {
                if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {
                    inventory.setItem(slot, CustomGUI.this.item(1, Material.LEGACY_STAINED_GLASS_PANE, n, " ", null));
                }
            }
        }
    }
    
    public void simpleFill(Player player, Inventory inventory, int n) {
        int size = inventory.getSize();
        int i = 0;
        for (Long n2 = 1L; i < size; ++i, ++n2) {
            if (n2 >= 9L) {
                n2 = 0L;
            }
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, CustomGUI.this.item(1, Material.LEGACY_STAINED_GLASS_PANE, n, " ", null));
            }
        }
    }
    
    public void addBackButtons(Inventory inventory) {
        for (int size = inventory.getSize(), i = 0; i < size; i += 9) {
            inventory.setItem(i, this.backButton);
        }
    }
    
    public void setBackButton(ItemStack backButton) {
        this.backButton = backButton;
    }
    
    public ItemStack getBackButton() {
        return this.backButton;
    }
    
    public void setFillItem(Material material, short n) {
        this.fillItem = this.item(1, material, n, " ", null);
    }
    
    public ItemStack getFillItem() {
        return this.fillItem;
    }
    
    public void addItem(String s, ItemStack itemStack) {
        this.items.put(s, itemStack);
    }
    
    public ItemStack getItem(String s) {
        return this.items.get(s);
    }
    
    public ItemStack getFormattedItem(String s, String player) {
    	ItemStack item = this.items.get(s);
    	ItemMeta meta = item.getItemMeta();
    	meta.setDisplayName(meta.getDisplayName().replaceAll("%player%", player));
    	item.setItemMeta(meta);
    	this.items.remove(s);
    	this.items.put(s, item);
        return this.items.get(s);
    }
    
    public HashMap<String, ItemStack> getItems() {
        return this.items;
    }
    
    public void removeItem(String s) {
        this.items.remove(s);
    }
    
    public void setSelected(ItemStack itemStack) {
        itemStack.addUnsafeEnchantment(Enchantment.LUCK, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);
        this.pl.utils.debug("current enchants: " + itemStack.getEnchantments().toString());
    }
    
    public HashMap<String, ItemStack> getListedPlayers() {
        return this.listedPlayers;
    }
    
    public ItemStack buildArrow(boolean b, int n) {
        ItemStack itemStack = b ? this.getItem("page-forward") : this.getItem("page-back");
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore((List)Collections.singletonList(String.valueOf(n)));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    
    public ItemStack buildPlayer(Player player) {
        ItemStack itemStack = new ItemStack(this.getItem("player"));
        itemStack.setType(Material.LEGACY_SKULL_ITEM);
        itemStack.setDurability((short)3);
        SkullMeta itemMeta = (SkullMeta)itemStack.getItemMeta();
        itemMeta.setDisplayName(itemMeta.getDisplayName().replaceAll("%player%", player.getName()));
        itemMeta.setOwner(player.getName());
        List lore = itemMeta.getLore();
        ArrayList<String> lore2 = new ArrayList<String>();
        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            lore2.add(iterator.next().replaceAll("%player%", player.getName()));
        }
        itemMeta.setLore((List)lore2);
        itemStack.setItemMeta((ItemMeta)itemMeta);
        this.listedPlayers.put(player.getName(), itemStack);
        return itemStack;
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        this.pl.utils.debug("Empty InventoryClickEvent called by " + this.getClass().getSimpleName());
    }
}
