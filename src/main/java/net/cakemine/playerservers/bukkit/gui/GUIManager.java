package net.cakemine.playerservers.bukkit.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

public class GUIManager
{
    PlayerServers pl;
    public HashMap<String, CustomGUI> customGUIs;
    
    public GUIManager(PlayerServers pl) {
        this.customGUIs = new HashMap<String, CustomGUI>();
        this.pl = pl;
    }
    
    public void openAnvil(Player player, String s) {
        player.openInventory(Bukkit.createInventory(null, InventoryType.ANVIL, this.pl.utils.color(s)));
    }
    
    public CustomGUI getGUI(String s) {
        if (this.customGUIs.containsKey(s)) {
            return this.customGUIs.get(s);
        }
        return this.customGUIs.entrySet().iterator().next().getValue();
    }
    
    public void deserializeGUIs(String s) {
    	ObjectMapper objectMapper = new ObjectMapper();
    	JsonNode jsonMap;
		try {
			jsonMap = objectMapper.readTree(s);
			
			// GUI Title
			this.getGUI("settings").setTitle(jsonMap.get("settings-title").asText());
			this.getGUI("gamemode").setTitle(jsonMap.get("gamemode-title").asText());
			this.getGUI("difficulty").setTitle(jsonMap.get("difficulty-title").asText());
			this.getGUI("whitelist").setTitle(jsonMap.get("whitelist-title").asText());
			this.getGUI("player-manager").setTitle(jsonMap.get("player-manager-title").asText());
			this.getGUI("player").setTitle(jsonMap.get("player-title").asText());
			this.getGUI("world-settings").setTitle(jsonMap.get("world-settings-title").asText());
			this.getGUI("mob-settings").setTitle(jsonMap.get("mob-settings-title").asText());
			this.getGUI("servers").setTitle(jsonMap.get("servers-title").asText());
			this.getGUI("templates").setTitle(jsonMap.get("templates-title").asText());
			this.getGUI("control").setTitle(jsonMap.get("control-title").asText());
			
			
			// back Button and Fill item
			short n = 0;
			short n2 = 0;
			String backBtn = jsonMap.get("go-back-item").asText();
			String fillItem = jsonMap.get("fill-item").asText();
			if (jsonMap.get("go-back-item").asText().contains(":")) {
				n = Short.valueOf(jsonMap.get("go-back-item").asText().split(":")[1]);
				backBtn = jsonMap.get("go-back-item").asText().split(":")[0];
			}
			
			if (jsonMap.get("fill-item").asText().contains(":")) {
				n = Short.valueOf(jsonMap.get("fill-item").asText().split(":")[1]);
				fillItem = jsonMap.get("fill-item").asText().split(":")[0];
			}
			
			Iterator<Map.Entry<String, CustomGUI>> iterator = this.customGUIs.entrySet().iterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().setBackButton(this.item(1, Material.matchMaterial(backBtn), n, "&c&lGo Back.", null));
            }
			
            Iterator<Map.Entry<String, CustomGUI>> iterator2 = this.customGUIs.entrySet().iterator();
            while (iterator2.hasNext()) {
                iterator2.next().getValue().setFillItem(Material.matchMaterial(fillItem), n2);
            }
			
            // Inventory Icons
            
            this.getGUI("settings").addItem("expire-tracker", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("expire-tracker").get("item-id").asText()), (jsonMap.path("settings-icons").path("expire-tracker").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("expire-tracker").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("expire-tracker").get("item-name").asText(), jsonMap.path("settings-icons").path("expire-tracker").get("item-lore").asText()));
            this.getGUI("settings").addItem("gamemode", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("gamemode").get("item-id").asText()), (jsonMap.path("settings-icons").path("gamemode").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("gamemode").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("gamemode").get("item-name").asText(), jsonMap.path("settings-icons").path("gamemode").get("item-lore").asText()));
            this.getGUI("settings").addItem("difficulty", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("difficulty").get("item-id").asText()), (jsonMap.path("settings-icons").path("difficulty").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("difficulty").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("difficulty").get("item-name").asText(), jsonMap.path("settings-icons").path("difficulty").get("item-lore").asText()));
            this.getGUI("settings").addItem("pvp-off", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("pvp-off").get("item-id").asText()), (jsonMap.path("settings-icons").path("pvp-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("pvp-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("pvp-off").get("item-name").asText(), jsonMap.path("settings-icons").path("pvp-off").get("item-lore").asText()));
            this.getGUI("settings").addItem("pvp-on", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("pvp-on").get("item-id").asText()), (jsonMap.path("settings-icons").path("pvp-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("pvp-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("pvp-on").get("item-name").asText(), jsonMap.path("settings-icons").path("pvp-on").get("item-lore").asText()));
            this.getGUI("settings").addItem("whitelist", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("whitelist").get("item-id").asText()), (jsonMap.path("settings-icons").path("whitelist").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("whitelist").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("whitelist").get("item-name").asText(), jsonMap.path("settings-icons").path("whitelist").get("item-lore").asText()));
            this.getGUI("settings").addItem("player-manager", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("player-manager").get("item-id").asText()), (jsonMap.path("settings-icons").path("player-manager").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("player-manager").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("player-manager").get("item-name").asText(), jsonMap.path("settings-icons").path("player-manager").get("item-lore").asText()));
            this.getGUI("settings").addItem("world-settings", this.item(1, itemMaterial(jsonMap.path("settings-icons").path("world-settings").get("item-id").asText()), (jsonMap.path("settings-icons").path("world-settings").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("settings-icons").path("world-settings").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("settings-icons").path("world-settings").get("item-name").asText(), jsonMap.path("settings-icons").path("world-settings").get("item-lore").asText()));
            
            
            this.getGUI("gamemode").addItem("force-gamemode-on", this.item(1, itemMaterial(jsonMap.path("gamemode-icons").path("force-gamemode-on").get("item-id").asText()), (jsonMap.path("gamemode-icons").path("force-gamemode-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("gamemode-icons").path("force-gamemode-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("gamemode-icons").path("force-gamemode-on").get("item-name").asText(), jsonMap.path("gamemode-icons").path("force-gamemode-on").get("item-lore").asText()));
            this.getGUI("gamemode").addItem("force-gamemode-off", this.item(1, itemMaterial(jsonMap.path("gamemode-icons").path("force-gamemode-off").get("item-id").asText()), (jsonMap.path("gamemode-icons").path("force-gamemode-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("gamemode-icons").path("force-gamemode-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("gamemode-icons").path("force-gamemode-off").get("item-name").asText(), jsonMap.path("gamemode-icons").path("force-gamemode-off").get("item-lore").asText()));
            this.getGUI("gamemode").addItem("survival", this.item(1, itemMaterial(jsonMap.path("gamemode-icons").path("survival").get("item-id").asText()), (jsonMap.path("gamemode-icons").path("survival").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("gamemode-icons").path("survival").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("gamemode-icons").path("survival").get("item-name").asText(), jsonMap.path("gamemode-icons").path("survival").get("item-lore").asText()));
            this.getGUI("gamemode").addItem("creative", this.item(1, itemMaterial(jsonMap.path("gamemode-icons").path("creative").get("item-id").asText()), (jsonMap.path("gamemode-icons").path("creative").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("gamemode-icons").path("creative").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("gamemode-icons").path("creative").get("item-name").asText(), jsonMap.path("gamemode-icons").path("creative").get("item-lore").asText()));
            this.getGUI("gamemode").addItem("adventure", this.item(1, itemMaterial(jsonMap.path("gamemode-icons").path("adventure").get("item-id").asText()), (jsonMap.path("gamemode-icons").path("adventure").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("gamemode-icons").path("adventure").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("gamemode-icons").path("adventure").get("item-name").asText(), jsonMap.path("gamemode-icons").path("adventure").get("item-lore").asText()));
            this.getGUI("gamemode").addItem("spectator", this.item(1, itemMaterial(jsonMap.path("gamemode-icons").path("spectator").get("item-id").asText()), (jsonMap.path("gamemode-icons").path("spectator").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("gamemode-icons").path("spectator").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("gamemode-icons").path("spectator").get("item-name").asText(), jsonMap.path("gamemode-icons").path("spectator").get("item-lore").asText()));
            
            
            this.getGUI("difficulty").addItem("peaceful", this.item(1, itemMaterial(jsonMap.path("difficulty-icons").path("peaceful").get("item-id").asText()), (jsonMap.path("difficulty-icons").path("peaceful").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("difficulty-icons").path("peaceful").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("difficulty-icons").path("peaceful").get("item-name").asText(), jsonMap.path("difficulty-icons").path("peaceful").get("item-lore").asText()));
            this.getGUI("difficulty").addItem("easy", this.item(1, itemMaterial(jsonMap.path("difficulty-icons").path("easy").get("item-id").asText()), (jsonMap.path("difficulty-icons").path("easy").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("difficulty-icons").path("easy").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("difficulty-icons").path("easy").get("item-name").asText(), jsonMap.path("difficulty-icons").path("easy").get("item-lore").asText()));
            this.getGUI("difficulty").addItem("normal", this.item(1, itemMaterial(jsonMap.path("difficulty-icons").path("normal").get("item-id").asText()), (jsonMap.path("difficulty-icons").path("normal").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("difficulty-icons").path("normal").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("difficulty-icons").path("normal").get("item-name").asText(), jsonMap.path("difficulty-icons").path("normal").get("item-lore").asText()));
            this.getGUI("difficulty").addItem("hard", this.item(1, itemMaterial(jsonMap.path("difficulty-icons").path("hard").get("item-id").asText()), (jsonMap.path("difficulty-icons").path("hard").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("difficulty-icons").path("hard").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("difficulty-icons").path("hard").get("item-name").asText(), jsonMap.path("difficulty-icons").path("hard").get("item-lore").asText()));
            
            
            this.getGUI("world-settings").addItem("allow-nether-on", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("allow-nether-on").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("allow-nether-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("allow-nether-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("allow-nether-on").get("item-name").asText(), jsonMap.path("world-settings-icons").path("allow-nether-on").get("item-lore").asText()));
            this.getGUI("world-settings").addItem("allow-nether-off", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("allow-nether-off").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("allow-nether-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("allow-nether-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("allow-nether-off").get("item-name").asText(), jsonMap.path("world-settings-icons").path("allow-nether-off").get("item-lore").asText()));
            this.getGUI("world-settings").addItem("allow-flight-on", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("allow-flight-on").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("allow-flight-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("allow-flight-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("allow-flight-on").get("item-name").asText(), jsonMap.path("world-settings-icons").path("allow-flight-on").get("item-lore").asText()));
            this.getGUI("world-settings").addItem("allow-flight-off", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("allow-flight-off").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("allow-flight-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("allow-flight-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("allow-flight-off").get("item-name").asText(), jsonMap.path("world-settings-icons").path("allow-flight-off").get("item-lore").asText()));
            this.getGUI("world-settings").addItem("generate-structures-on", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("generate-structures-on").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("generate-structures-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("generate-structures-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("generate-structures-on").get("item-name").asText(), jsonMap.path("world-settings-icons").path("generate-structures-on").get("item-lore").asText()));
            this.getGUI("world-settings").addItem("generate-structures-off", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("generate-structures-off").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("generate-structures-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("generate-structures-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("generate-structures-off").get("item-name").asText(), jsonMap.path("world-settings-icons").path("generate-structures-off").get("item-lore").asText()));
            this.getGUI("world-settings").addItem("mob-settings", this.item(1, itemMaterial(jsonMap.path("world-settings-icons").path("mob-settings").get("item-id").asText()), (jsonMap.path("world-settings-icons").path("mob-settings").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("world-settings-icons").path("mob-settings").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("world-settings-icons").path("mob-settings").get("item-name").asText(), jsonMap.path("world-settings-icons").path("mob-settings").get("item-lore").asText()));
            
            
            this.getGUI("whitelist").addItem("whitelist-off", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("whitelist-off").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("whitelist-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("whitelist-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("whitelist-off").get("item-name").asText(), jsonMap.path("whitelist-icons").path("whitelist-off").get("item-lore").asText()));
            this.getGUI("whitelist").addItem("whitelist-on", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("whitelist-on").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("whitelist-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("whitelist-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("whitelist-on").get("item-name").asText(), jsonMap.path("whitelist-icons").path("whitelist-on").get("item-lore").asText()));
            this.getGUI("whitelist").addItem("current-whitelist", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("current-whitelist").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("current-whitelist").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("current-whitelist").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("current-whitelist").get("item-name").asText(), null));
            this.getGUI("whitelist").addItem("add-player", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("add-player").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("add-player").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("add-player").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("add-player").get("item-name").asText(), jsonMap.path("whitelist-icons").path("add-player").get("item-lore").asText()));
            this.getGUI("whitelist").addItem("remove-player", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("remove-player").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("remove-player").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("remove-player").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("remove-player").get("item-name").asText(), jsonMap.path("whitelist-icons").path("remove-player").get("item-lore").asText()));
            this.getGUI("whitelist").addItem("clear-whitelist", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("clear-whitelist").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("clear-whitelist").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("clear-whitelist").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("clear-whitelist").get("item-name").asText(), jsonMap.path("whitelist-icons").path("clear-whitelist").get("item-lore").asText()));
            this.getGUI("whitelist").addItem("clear-confirm", this.item(1, itemMaterial(jsonMap.path("whitelist-icons").path("clear-confirm").get("item-id").asText()), (jsonMap.path("whitelist-icons").path("clear-confirm").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("whitelist-icons").path("clear-confirm").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("whitelist-icons").path("clear-confirm").get("item-name").asText(), jsonMap.path("whitelist-icons").path("clear-confirm").get("item-lore").asText()));
            
            
            this.getGUI("player-manager").addItem("nobody-online", this.item(1, itemMaterial(jsonMap.path("player-manager-icons").path("nobody-online").get("item-id").asText()), (jsonMap.path("player-manager-icons").path("nobody-online").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-manager-icons").path("nobody-online").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-manager-icons").path("nobody-online").get("item-name").asText(), jsonMap.path("player-manager-icons").path("nobody-online").get("item-lore").asText()));
            this.getGUI("player-manager").addItem("player", this.item(1, itemMaterial(jsonMap.path("player-manager-icons").path("player").get("item-id").asText()), (jsonMap.path("player-manager-icons").path("player").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-manager-icons").path("player").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-manager-icons").path("player").get("item-name").asText(), jsonMap.path("player-manager-icons").path("player").get("item-lore").asText()));
            this.getGUI("player-manager").addItem("page-back", this.item(1, itemMaterial(jsonMap.path("player-manager-icons").path("page-back").get("item-id").asText()), (jsonMap.path("player-manager-icons").path("page-back").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-manager-icons").path("page-back").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-manager-icons").path("page-back").get("item-name").asText(), null));
            this.getGUI("player-manager").addItem("page-forward", this.item(1, itemMaterial(jsonMap.path("player-manager-icons").path("page-forward").get("item-id").asText()), (jsonMap.path("player-manager-icons").path("page-forward").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-manager-icons").path("page-forward").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-manager-icons").path("page-forward").get("item-name").asText(), null));
            
            
            
            this.getGUI("player").addItem("player", this.item(1, itemMaterial(jsonMap.path("player-icons").path("player").get("item-id").asText()), (jsonMap.path("player-icons").path("player").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-icons").path("player").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-icons").path("player").get("item-name").asText(), jsonMap.path("player-icons").path("player").get("item-lore").asText()));
            this.getGUI("player").addItem("kick", this.item(1, itemMaterial(jsonMap.path("player-icons").path("kick").get("item-id").asText()), (jsonMap.path("player-icons").path("kick").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-icons").path("kick").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-icons").path("kick").get("item-name").asText(), jsonMap.path("player-icons").path("kick").get("item-lore").asText()));
            this.getGUI("player").addItem("ban", this.item(1, itemMaterial(jsonMap.path("player-icons").path("ban").get("item-id").asText()), (jsonMap.path("player-icons").path("ban").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-icons").path("ban").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-icons").path("ban").get("item-name").asText(), jsonMap.path("player-icons").path("ban").get("item-lore").asText()));
            this.getGUI("player").addItem("ban-confirm", this.item(1, itemMaterial(jsonMap.path("player-icons").path("ban-confirm").get("item-id").asText()), (jsonMap.path("player-icons").path("ban-confirm").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-icons").path("ban-confirm").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-icons").path("ban-confirm").get("item-name").asText(), jsonMap.path("player-icons").path("ban-confirm").get("item-lore").asText()));
            this.getGUI("player").addItem("player-is-whitelisted", this.item(1, itemMaterial(jsonMap.path("player-icons").path("player-is-whitelisted").get("item-id").asText()), (jsonMap.path("player-icons").path("player-is-whitelisted").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-icons").path("player-is-whitelisted").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-icons").path("player-is-whitelisted").get("item-name").asText(), jsonMap.path("player-icons").path("player-is-whitelisted").get("item-lore").asText()));
            this.getGUI("player").addItem("player-not-whitelisted", this.item(1, itemMaterial(jsonMap.path("player-icons").path("player-not-whitelisted").get("item-id").asText()), (jsonMap.path("player-icons").path("player-not-whitelisted").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("player-icons").path("player-not-whitelisted").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("player-icons").path("player-not-whitelisted").get("item-name").asText(), jsonMap.path("player-icons").path("player-not-whitelisted").get("item-lore").asText()));
            
            
            
            this.getGUI("mob-settings").addItem("monster-spawns-on", this.item(1, itemMaterial(jsonMap.path("mob-settings-icons").path("monster-spawns-on").get("item-id").asText()), (jsonMap.path("mob-settings-icons").path("monster-spawns-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("mob-settings-icons").path("monster-spawns-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("mob-settings-icons").path("monster-spawns-on").get("item-name").asText(), jsonMap.path("mob-settings-icons").path("monster-spawns-on").get("item-lore").asText()));
            this.getGUI("mob-settings").addItem("monster-spawns-off", this.item(1, itemMaterial(jsonMap.path("mob-settings-icons").path("monster-spawns-off").get("item-id").asText()), (jsonMap.path("mob-settings-icons").path("monster-spawns-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("mob-settings-icons").path("monster-spawns-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("mob-settings-icons").path("monster-spawns-off").get("item-name").asText(), jsonMap.path("mob-settings-icons").path("monster-spawns-off").get("item-lore").asText()));
            this.getGUI("mob-settings").addItem("animal-spawns-on", this.item(1, itemMaterial(jsonMap.path("mob-settings-icons").path("animal-spawns-on").get("item-id").asText()), (jsonMap.path("mob-settings-icons").path("animal-spawns-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("mob-settings-icons").path("animal-spawns-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("mob-settings-icons").path("animal-spawns-on").get("item-name").asText(), jsonMap.path("mob-settings-icons").path("animal-spawns-on").get("item-lore").asText()));
            this.getGUI("mob-settings").addItem("animal-spawns-off", this.item(1, itemMaterial(jsonMap.path("mob-settings-icons").path("animal-spawns-off").get("item-id").asText()), (jsonMap.path("mob-settings-icons").path("animal-spawns-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("mob-settings-icons").path("animal-spawns-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("mob-settings-icons").path("animal-spawns-off").get("item-name").asText(), jsonMap.path("mob-settings-icons").path("animal-spawns-off").get("item-lore").asText()));
            this.getGUI("mob-settings").addItem("npc-spawns-on", this.item(1, itemMaterial(jsonMap.path("mob-settings-icons").path("npc-spawns-on").get("item-id").asText()), (jsonMap.path("mob-settings-icons").path("npc-spawns-on").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("mob-settings-icons").path("npc-spawns-on").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("mob-settings-icons").path("npc-spawns-on").get("item-name").asText(), jsonMap.path("mob-settings-icons").path("npc-spawns-on").get("item-lore").asText()));
            this.getGUI("mob-settings").addItem("npc-spawns-off", this.item(1, itemMaterial(jsonMap.path("mob-settings-icons").path("npc-spawns-off").get("item-id").asText()), (jsonMap.path("mob-settings-icons").path("npc-spawns-off").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("mob-settings-icons").path("npc-spawns-off").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("mob-settings-icons").path("npc-spawns-off").get("item-name").asText(), jsonMap.path("mob-settings-icons").path("npc-spawns-off").get("item-lore").asText()));
            
            
            
            this.getGUI("servers").addItem("none-online", this.item(1, itemMaterial(jsonMap.path("servers-icons").path("none-online").get("item-id").asText()), (jsonMap.path("servers-icons").path("none-online").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("servers-icons").path("none-online").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("servers-icons").path("none-online").get("item-name").asText(), jsonMap.path("servers-icons").path("none-online").get("item-lore").asText()));
            this.getGUI("servers").addItem("server", this.item(1, itemMaterial(jsonMap.path("servers-icons").path("server").get("item-id").asText()), (jsonMap.path("servers-icons").path("server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("servers-icons").path("server").get("item-id").asText().split(":")[1]) : 0), null, jsonMap.path("servers-icons").path("server").get("item-lore").asText()));
            this.getGUI("servers").addItem("page-back", this.item(1, itemMaterial(jsonMap.path("servers-icons").path("page-back").get("item-id").asText()), (jsonMap.path("servers-icons").path("page-back").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("servers-icons").path("page-back").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("servers-icons").path("page-back").get("item-name").asText(), null));
            this.getGUI("servers").addItem("page-forward", this.item(1, itemMaterial(jsonMap.path("servers-icons").path("page-forward").get("item-id").asText()), (jsonMap.path("servers-icons").path("page-forward").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("servers-icons").path("page-forward").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("servers-icons").path("page-forward").get("item-name").asText(), null));
            
            
            
            this.getGUI("templates").addItem("none-found", this.item(1, itemMaterial(jsonMap.path("templates-icons").path("none-found").get("item-id").asText()), (jsonMap.path("templates-icons").path("none-found").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("templates-icons").path("none-found").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("templates-icons").path("none-found").get("item-name").asText(), jsonMap.path("templates-icons").path("none-found").get("item-lore").asText()));
            this.getGUI("templates").addItem("page-back", this.item(1, itemMaterial(jsonMap.path("templates-icons").path("page-back").get("item-id").asText()), (jsonMap.path("templates-icons").path("page-back").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("templates-icons").path("page-back").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("templates-icons").path("page-back").get("item-name").asText(), null));
            this.getGUI("templates").addItem("page-forward", this.item(1, itemMaterial(jsonMap.path("templates-icons").path("page-forward").get("item-id").asText()), (jsonMap.path("templates-icons").path("page-forward").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("templates-icons").path("page-forward").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("templates-icons").path("page-forward").get("item-name").asText(), null));
            
            
            
            this.getGUI("control").addItem("expire-tracker", this.item(1, itemMaterial(jsonMap.path("control-icons").path("expire-tracker").get("item-id").asText()), (jsonMap.path("control-icons").path("expire-tracker").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("expire-tracker").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("expire-tracker").get("item-name").asText(), jsonMap.path("control-icons").path("expire-tracker").get("item-lore").asText()));
            this.getGUI("control").addItem("create-server", this.item(1, itemMaterial(jsonMap.path("control-icons").path("create-server").get("item-id").asText()), (jsonMap.path("control-icons").path("create-server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("create-server").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("create-server").get("item-name").asText(), jsonMap.path("control-icons").path("create-server").get("item-lore").asText()));
            this.getGUI("control").addItem("delete-server", this.item(1, itemMaterial(jsonMap.path("control-icons").path("delete-server").get("item-id").asText()), (jsonMap.path("control-icons").path("delete-server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("delete-server").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("delete-server").get("item-name").asText(), jsonMap.path("control-icons").path("delete-server").get("item-lore").asText()));
            this.getGUI("control").addItem("delete-confirm", this.item(1, itemMaterial(jsonMap.path("control-icons").path("delete-confirm").get("item-id").asText()), (jsonMap.path("control-icons").path("delete-confirm").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("delete-confirm").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("delete-confirm").get("item-name").asText(), jsonMap.path("control-icons").path("delete-confirm").get("item-lore").asText()));
            this.getGUI("control").addItem("join-server", this.item(1, itemMaterial(jsonMap.path("control-icons").path("join-server").get("item-id").asText()), (jsonMap.path("control-icons").path("join-server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("join-server").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("join-server").get("item-name").asText(), jsonMap.path("control-icons").path("join-server").get("item-lore").asText()));
            this.getGUI("control").addItem("leave-server", this.item(1, itemMaterial(jsonMap.path("control-icons").path("leave-server").get("item-id").asText()), (jsonMap.path("control-icons").path("leave-server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("leave-server").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("leave-server").get("item-name").asText(), jsonMap.path("control-icons").path("leave-server").get("item-lore").asText()));
            this.getGUI("control").addItem("start-server", this.item(1, itemMaterial(jsonMap.path("control-icons").path("start-server").get("item-id").asText()), (jsonMap.path("control-icons").path("start-server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("start-server").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("start-server").get("item-name").asText(), jsonMap.path("control-icons").path("start-server").get("item-lore").asText()));
            this.getGUI("control").addItem("stop-server", this.item(1, itemMaterial(jsonMap.path("control-icons").path("stop-server").get("item-id").asText()), (jsonMap.path("control-icons").path("stop-server").get("item-id").asText().contains(":") ? Short.valueOf(jsonMap.path("control-icons").path("stop-server").get("item-id").asText().split(":")[1]) : 0), jsonMap.path("control-icons").path("stop-server").get("item-name").asText(), jsonMap.path("control-icons").path("stop-server").get("item-lore").asText()));
	        
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private Material itemMaterial(String s) {
		if (s.contains(":")) {
			return Material.matchMaterial(s.split(":")[0]);
		}
		return Material.matchMaterial(s);
	}

	public void registerGUIs() {
        this.putGUI("settings", new SettingsGUI(this.pl));
        this.putGUI("gamemode", new GamemodeGUI(this.pl));
        this.putGUI("difficulty", new DifficultyGUI(this.pl));
        this.putGUI("whitelist", new WhitelistGUI(this.pl));
        this.putGUI("player-manager", new PlayerManageGUI(this.pl));
        this.putGUI("player", new PlayerGUI(this.pl));
        this.putGUI("mob-settings", new MobGUI(this.pl));
        this.putGUI("world-settings", new WorldGUI(this.pl));
        this.putGUI("templates", new TemplatesGUI(this.pl));
        this.putGUI("servers", new ServersGUI(this.pl));
        this.putGUI("control", new ControlGUI(this.pl));
    }
    
    public void putGUI(String s, CustomGUI customGUI) {
        this.customGUIs.put(s, customGUI);
    }
    
    public ItemStack item(int n, Material material, short n2, String s, String s2) {
        ItemStack itemStack = new ItemStack(material, n);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemStack.setDurability(n2);
        if (s != null) {
            itemMeta.setDisplayName(this.pl.utils.color(s));
        }
        if (s2 != null) {
            itemMeta.setLore(Arrays.asList(this.pl.utils.color(s2).split("(\\|\\||\n|\\n|\\{n\\})")));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
