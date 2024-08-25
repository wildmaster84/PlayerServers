package net.cakemine.playerservers.bukkit.gui;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.cakemine.playerservers.bukkit.PlayerServers;

public class GUIManager {
    private final PlayerServers pl;
    private final Map<String, CustomGUI> customGUIs;
    private final Map<String, Material> materialCache = new ConcurrentHashMap<>();

    public GUIManager(PlayerServers pl) {
        this.customGUIs = new ConcurrentHashMap<>();
        this.pl = pl;
    }

    public void openAnvil(Player player, String title) {
        player.openInventory(Bukkit.createInventory(null, InventoryType.ANVIL, this.pl.utils.color(title)));
    }

    public CustomGUI getGUI(String guiName) {
        return this.customGUIs.get(guiName);
    }

    public void deserializeGUIs(String jsonData) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonMap = objectMapper.readTree(jsonData);
            Material backBtnMaterial = getCachedMaterial(jsonMap.get("go-back-item").asText());
            Material fillItemMaterial = getCachedMaterial(jsonMap.get("fill-item").asText());

            customGUIs.forEach((name, gui) -> {
                gui.setBackButton(createItem(1, backBtnMaterial, (short) 0, "&c&lGo Back.", null));
                gui.setFillItem(fillItemMaterial, (short) 0);
            });

            updateGUIs(jsonMap);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void updateGUIs(JsonNode jsonMap) {
        updateGUIItems("settings", jsonMap.path("settings-icons"));
        updateGUIItems("gamemode", jsonMap.path("gamemode-icons"));
        updateGUIItems("difficulty", jsonMap.path("difficulty-icons"));
        updateGUIItems("whitelist", jsonMap.path("whitelist-icons"));
        updateGUIItems("player-manager", jsonMap.path("player-manager-icons"));
        updateGUIItems("player", jsonMap.path("player-icons"));
        updateGUIItems("world-settings", jsonMap.path("world-settings-icons"));
        updateGUIItems("mob-settings", jsonMap.path("mob-settings-icons"));
        updateGUIItems("servers", jsonMap.path("servers-icons"));
        updateGUIItems("templates", jsonMap.path("templates-icons"));
        updateGUIItems("control", jsonMap.path("control-icons"));
        
        this.getGUI("settings").setTitle(jsonMap.path("settings-title").asText());
		this.getGUI("gamemode").setTitle(jsonMap.path("gamemode-title").asText());
		this.getGUI("difficulty").setTitle(jsonMap.path("difficulty-title").asText());
		this.getGUI("whitelist").setTitle(jsonMap.path("whitelist-title").asText());
		this.getGUI("player-manager").setTitle(jsonMap.path("player-manager-title").asText());
		this.getGUI("player").setTitle(jsonMap.path("player-title").asText());
		this.getGUI("world-settings").setTitle(jsonMap.path("world-settings-title").asText());
		this.getGUI("mob-settings").setTitle(jsonMap.path("mob-settings-title").asText());
		this.getGUI("servers").setTitle(jsonMap.path("servers-title").asText());
		this.getGUI("templates").setTitle(jsonMap.path("templates-title").asText());
		this.getGUI("control").setTitle(jsonMap.path("control-title").asText());
    }

    private void updateGUIItems(String guiName, JsonNode iconNode) {
        CustomGUI gui = getGUI(guiName);
        if (gui == null || iconNode.isMissingNode()) return;

        iconNode.fields().forEachRemaining(field -> {
            JsonNode itemData = field.getValue();
            Material material = getCachedMaterial(itemData.get("item-id").asText());
            short durability = (short) (itemData.path("item-id").asText().contains(":") ? itemData.path("item-id").asText().split(":")[1].charAt(0) : 0);  // Simplified parsing
            String itemName = itemData.path("item-name").asText("");
            String itemLore = itemData.path("item-lore").asText(null);

            gui.addItem(field.getKey(), createItem(1, material, durability, itemName, itemLore));
        });
    }

    private Material getCachedMaterial(String itemName) {
        return materialCache.computeIfAbsent(itemName, this::itemMaterial);
    }

    private Material itemMaterial(String itemName) {
        return Material.matchMaterial(itemName.split(":")[0]);
    }

    public void registerGUIs() {
        putGUI("settings", new SettingsGUI(this.pl));
        putGUI("gamemode", new GamemodeGUI(this.pl));
        putGUI("difficulty", new DifficultyGUI(this.pl));
        putGUI("whitelist", new WhitelistGUI(this.pl));
        putGUI("player-manager", new PlayerManageGUI(this.pl));
        putGUI("player", new PlayerGUI(this.pl));
        putGUI("mob-settings", new MobGUI(this.pl));
        putGUI("world-settings", new WorldGUI(this.pl));
        putGUI("templates", new TemplatesGUI(this.pl));
        putGUI("servers", new ServersGUI(this.pl));
        putGUI("control", new ControlGUI(this.pl));
    }

    public void putGUI(String guiName, CustomGUI customGUI) {
        this.customGUIs.put(guiName, customGUI);
    }
    
    public void removeGUI(String guiName) {
        this.customGUIs.remove(guiName);
    }
    
    public Map<String, CustomGUI> getGUIS() {
    	return this.customGUIs;
    }

    public ItemStack createItem(int amount, Material material, short durability, String displayName, String lore) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemStack.setDurability(durability);

        if (displayName != null) {
            itemMeta.setDisplayName(this.pl.utils.color(displayName));
        }
        if (lore != null) {
            itemMeta.setLore(Arrays.asList(this.pl.utils.color(lore).split("(\\|\\||\n|\\n|\\{n\\})")));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
