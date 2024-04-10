package net.cakemine.playerservers.bukkit;

import org.bukkit.*;
import org.bukkit.entity.*;
import net.cakemine.playerservers.bukkit.gui.*;
import net.cakemine.playerservers.bungee.objects.PlayerServer;

import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import org.bukkit.enchantments.*;
import java.io.*;
import com.google.common.io.*;

public class PlayerServersAPI
{
    PlayerServers pl;
    public List<String> customGuis;
    
    PlayerServersAPI(PlayerServers pl) {
        this.customGuis = new ArrayList<String>();
        this.pl = pl;
    }
    
    public void reSync() {
        this.pl.sender.doSync();
    }
    
    public boolean getDebugMode() {
        return this.pl.debug;
    }
    
    public void debugLog(String s) {
        this.pl.utils.debug(s);
    }

    public void debugLog(JavaPlugin plugin, String s) {
        this.pl.utils.debug(plugin, s);
    }
    
    public String getPluginPrefix() {
        return this.pl.prefix;
    }
    
    public void setPluginPrefix(String prefix) {
        this.pl.prefix = prefix;
    }
    
    public UUID getServerOwnerId() {
        return UUID.fromString(this.pl.utils.getOwnerId());
    }
    
    public String getServerOwnerName() {
        return Bukkit.getOfflinePlayer(this.getServerOwnerId()).getName();
    }
    
    public String getPlayerServerName() {
        return this.pl.getServer().getName();
    }
    
    public int getPlayerServerPort() {
        return this.pl.getServer().getPort();
    }
    
    public boolean isOwnerOP() {
        return this.pl.getOPCheck();
    }
    
    public void setOwnerOP(boolean b) {
        this.pl.psrvCfg.set("creator-gets-op", (Object)b);
        this.pl.saveConfig(this.pl.psrvCfg, this.pl.psrv);
    }
    
    public boolean isExpireShutdown() {
        return this.pl.expireShutdown;
    }
    
    public void setExpireShutdown(boolean b) {
        this.pl.psrvCfg.set("shutdown-on-expire", (Object)true);
        this.pl.saveConfig(this.pl.psrvCfg, this.pl.psrv);
    }
    
    public String getTemplateName() {
        return this.pl.psrvCfg.getString("template-name");
    }
    
    public String getTemplateDescription() {
        return this.pl.psrvCfg.getString("description");
    }
    
    public List<String> getTemplateDescriptionList() {
        return Arrays.asList(this.pl.psrvCfg.getString("description").split("||"));
    }
    
    public Material getTemplateIcon() {
        Material material = Material.matchMaterial(this.pl.psrvCfg.getString("icon-material"));
        if (material == null) {
            material = Material.LEGACY_GRASS;
        }
        return material;
    }
    
    public String getTemplateDefaultExpiry() {
        return this.pl.psrvCfg.getString("default-expiry-time");
    }
    
    public void addTime(int n, String s) {
        this.pl.sender.changeExpireTime(true, n, s);
    }
    
    public void removeTime(int n, String s) {
        this.pl.sender.changeExpireTime(false, n, s);
    }
    
    public long getMillisLeft() {
        return this.pl.msLeft;
    }
    
    public String getTimeLeft() {
        return this.pl.timeLeft;
    }
    
    public String getExpireDate() {
        return this.pl.expireDate;
    }
    
    public void openSettingsGUI(Player player) {
        this.pl.gui.getGUI("settings").open(player, null);
    }
    public void openGamemodeGUI(Player player) {
        this.pl.gui.getGUI("gamemode").open(player, null);
    }
    
    public void openDifficultyGUI(Player player) {
        this.pl.gui.getGUI("difficulty").open(player, null);
    }
    
    public void openWhitelistGUI(Player player) {
        this.pl.gui.getGUI("whitelist").open(player, null);
    }
    
    public void openPlayerManagerGUI(Player player, int n) {
        if (n < 1) {
            n = 1;
        }
        this.pl.gui.getGUI("player-manager").open(player, null, n);
    }
    
    public void openPlayerGUI(Player player, Player player2) {
        this.pl.gui.getGUI("player").open(player, null, player2);
    }
    
    public void openMobGUI(Player player) {
        this.pl.gui.getGUI("mob-settings").open(player, null);
    }
    
    public void openWorldGUI(Player player) {
        this.pl.gui.getGUI("world-settings").open(player, null);
    }
    
    public HashMap<String, CustomGUI> getGUIMap() {
        return this.pl.gui.customGUIs;
    }
    
    public CustomGUI getCustomGUI(String s) {
        if (this.pl.gui.customGUIs.containsKey(s)) {
            return this.pl.gui.customGUIs.get(s);
        }
        return null;
    }
    
    public void putCustomGUI(String s, CustomGUI customGUI) {
    	this.pl.gui.putGUI(s, customGUI);
        this.pl.gui.customGUIs.put(s, customGUI);
    }
    
    public void removeCustomGUI(String s) {
        if (this.pl.gui.customGUIs.containsKey(s)) {
            this.pl.gui.customGUIs.remove(s);
        }
    }
    
    public CustomGUI newCustomGUI(String s) {
        CustomGUI customGUI = new CustomGUI(this.pl);
        this.pl.gui.customGUIs.put(s, customGUI);
        return customGUI;
    }
    
    public Inventory openCustomGUI(Player player, String replaceAll, int n, Inventory inventory) {
        replaceAll = replaceAll.replaceAll("(&|§)[0-9aA-fFkK-oORr]", "");
        if (!this.customGuis.contains(replaceAll)) {
            this.customGuis.add(replaceAll);
        }
        return new CustomGUI(this.pl).reopenGUI(player, inventory, n, replaceAll);
    }
    
    public ItemStack customItemStack(int n, Material material, short n2, String s, String s2) {
        return this.pl.gui.item(n, material, n2, s, s2);
    }
    
    public ItemStack customItemStack(int n, Material material, short n2, String s, List<String> list) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            sb.append("||");
        }
        return this.pl.gui.item(n, material, n2, s, sb.toString());
    }
    
    public ItemStack setSelected(ItemStack itemStack) {
        itemStack.addUnsafeEnchantment(Enchantment.LUCK, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    
    public String getServerPropSetting(String s) {
        return this.pl.settingsManager.getSetting(s);
    }
    
    public void setServerPropSetting(String s, String s2) {
        this.pl.settingsManager.changeSetting(s, s2);
    }
    
    public void setServerGamemode(int gamemode) {
        this.pl.settingsManager.setGamemode(gamemode);
    }
    
    public void setServerDifficulty(int difficulty) {
        this.pl.settingsManager.setDifficulty(difficulty);
    }
    
    public void setServerPvP(boolean pvP) {
        this.pl.settingsManager.setPvP(pvP);
    }
    
    public File[] listFiles(File file) {
        if (file.isDirectory()) {
            return file.listFiles();
        }
        return null;
    }
    
    public void copyFileSoft(File file, File file2) {
        this.pl.utils.doSoftCopy(file, file2);
    }
    
    public void copyFileHard(File file, File file2) {
        this.pl.utils.copyFile(file, file2);
    }
    
    public void deleteFile(File file) {
        this.pl.utils.doDelete(file);
    }
    
    public void startServerName(String s) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("startServerName");
        dataOutput.writeUTF(s);
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void startServerUUID(UUID uuid) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("startServerUUID");
        dataOutput.writeUTF(uuid.toString());
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void startServerPlayer(String s) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("startServerPlayer");
        dataOutput.writeUTF(s);
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void stopServerName(String s) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("stopServerName");
        dataOutput.writeUTF(s);
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void stopServerUUID(UUID uuid) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("stopServerUUID");
        dataOutput.writeUTF(uuid.toString());
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void stopServerPlayer(String s) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("stopServerPlayer");
        dataOutput.writeUTF(s);
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void stopAllServers() {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("stopAllServers");
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void addBungeeServer(String s, String s2, Integer n, String s3, int n2) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("addBungeeServer");
        dataOutput.writeUTF(s);
        dataOutput.writeUTF(s2);
        dataOutput.writeUTF(String.valueOf(n));
        dataOutput.writeUTF(s3);
        dataOutput.writeUTF(String.valueOf(n2));
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void removeBungeeServer(String s) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("removeBungeeServer");
        dataOutput.writeUTF(s);
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void createServer(UUID uuid, String s) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("createServer");
        dataOutput.writeUTF(uuid.toString());
        dataOutput.writeUTF(s);
        this.pl.sender.apiCall(dataOutput);
    }
    
    public void deleteServer(UUID uuid) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("APIcall");
        dataOutput.writeUTF("deleteServer");
        dataOutput.writeUTF(uuid.toString());
        this.pl.sender.apiCall(dataOutput);
    }
    
    public PlayerServers getInstance() {
        return this.pl;
    }
    
    /**
     * @deprecated This method might be removed unless requested to stay.
     */
    @Deprecated
    public int[] getPluginVersion() {    	
    	String[] version = this.pl.getDescription().getVersion().split("-")[0].split("\\.");

        int[] intArray = new int[version.length];
        for (int i = 0; i < version.length; i++) {
            intArray[i] = Integer.parseInt(version[i]);
        }
        return intArray;
    }
}
