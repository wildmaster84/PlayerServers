package net.cakemine.playerservers.bungee;

import net.md_5.bungee.config.*;
import java.util.logging.*;
import java.io.*;
import net.md_5.bungee.api.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.attribute.*;
import java.nio.file.*;

public class TemplateManager
{
    PlayerServers pl;
    public HashMap<File, Configuration> templates;
    
    public TemplateManager(PlayerServers pl) {
        this.templates = new HashMap<File, Configuration>();
        this.pl = pl;
    }
    
    public void loadTemplates() {
        File file = new File(this.pl.getDataFolder() + File.separator + "templates");
        if (!file.exists()) {
            this.defaultTemplate();
        }
        if (file.isDirectory()) {
            for (File templateFolder : file.listFiles()) {
                this.templates.put(templateFolder, this.loadTemplateConfig(templateFolder));
                this.updateConfig(templateFolder);
                this.linkPS(templateFolder.getName());
                this.pl.utils.log("Loaded template: " + this.pl.templateManager.getTemplateSetting(templateFolder, "template-name"));
            }
        }
    }
    
    public void updateConfig(File file) {
        if (this.templates.get(file).get("default-expiry-time") == null) {
            this.templates.get(file).set("default-expiry-time", "1 day");
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + "config with default-expiry-time option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("shutdown-on-expire") == null) {
            this.templates.get(file).set("shutdown-on-expire", false);
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + " config with shutdown-on-expire option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("owner-join-message") == null) {
            this.templates.get(file).set("owner-join-message", "&eWelcome the owner of the server!||&6&o%player%&e has joined!");
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + " config with owner-join-message option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("owner-join-commands") == null) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("#enter custom commands to run on join here.");
            list.add("#commands starting with # won't be ran.");
            list.add("#placeholders: %owner-name%, %owner-uuid%, %template-name%, %server-name%, %server-port%");
            this.templates.get(file).set("owner-join-commands", (Object)list);
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + " config with owner-join-commands option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("default-Xmx") == null) {
            this.templates.get(file).set("default-Xmx", (Object)this.pl.defaultMem.get("xmx"));
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + "config with default-Xmx option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("default-Xms") == null) {
            this.templates.get(file).set("default-Xms", (Object)this.pl.defaultMem.get("xms"));
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + "config with default-Xms option.");
            this.saveTemplateConfig(file);
        }
    }
    
    public Configuration loadTemplateConfig(File file) {
        if (file.isDirectory()) {
            File file2 = new File(file, "PlayerServers.yml");
            if (!file2.exists()) {
                this.pl.copyResource(file2);
            }
            try {
                return this.pl.cfg.load(file2);
            }
            catch (IOException ex) {
                this.pl.utils.log(Level.SEVERE, "Failed to load template config file for " + file.getName() + "! Please send this stack trace to the developer.");
                ex.printStackTrace();
            }
        }
        return null;
    }
    
    public void saveTemplateConfig(File file) {
        if (file.isDirectory()) {
            File file2 = new File(file, "PlayerServers.yml");
            try {
                this.pl.cfg.save((Configuration)this.templates.get(file), file2);
            }
            catch (IOException ex) {
                this.pl.utils.log(Level.SEVERE, "Failed to save the file " + file2.getPath() + ", please send this stack trace to the developer.");
                ex.printStackTrace();
            }
            this.loadTemplateConfig(file);
        }
    }
    
    public File getTemplateFile(String s) {
        this.pl.utils.debug("getTemplateFile input: " + s);
        String stripColor = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
        this.pl.utils.debug("stripped color: " + stripColor);
        for (Map.Entry<File, Configuration> entry : this.templates.entrySet()) {
            File file = entry.getKey();
            String stripColor2 = ChatColor.stripColor(this.pl.utils.color(entry.getValue().getString("template-name")));
            this.pl.utils.debug("tempFile: " + file + " | tempName: " + stripColor2);
            if (stripColor2.equalsIgnoreCase(stripColor) || file.getName().equalsIgnoreCase(s) || file.getName().equalsIgnoreCase(stripColor)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public String getTemplateSetting(File file, String s) {
        if (this.templates.containsKey(file)) {
            return this.templates.get(file).getString(s);
        }
        return null;
    }
    
    public List<String> getTemplateSettingList(File file, String s) {
        if (this.templates.containsKey(file)) {
            return (List<String>)this.templates.get(file).getStringList(s);
        }
        return null;
    }
    
    public int expireTime(File file) {
        if (this.templates.get(file).getString("default-expiry-time") == null || this.templates.get(file).getString("default-expiry-time").isEmpty()) {
            return 1;
        }
        return Integer.valueOf(this.templates.get(file).getString("default-expiry-time").replaceAll("(\\s|month|mnth|mnt|mth|mo|week|wk|wek|w|day|dy|d|hour|hr|h|minute|min|mi|m)(s)?", ""));
    }
    
    public String expireUnit(File file) {
        if (this.templates.get(file).getString("default-expiry-time") == null || this.templates.get(file).getString("default-expiry-time").isEmpty()) {
            return "day";
        }
        return this.templates.get(file).getString("default-expiry-time").replaceAll("[0-9]|\\s", "");
    }
    
    public void defaultTemplate() {
        File file = new File(this.pl.getDataFolder() + File.separator + "templates");
        File file2 = new File(this.pl.getDataFolder() + File.separator + "templates" + File.separator + "default");
        File file3 = new File(file2 + File.separator + "plugins");
        File file4 = new File(this.pl.getDataFolder() + File.separator + "template");
        if (file4.exists() && (!file.exists() || !file2.exists())) {
            this.pl.utils.log(Level.WARNING, "Converting existing template to new multi template format.");
            if (!file.exists()) {
                file.mkdir();
            }
            if (!file2.exists()) {
                file2.mkdir();
            }
            this.doConvertCopy(file4, file2);
            return;
        }
        if (!file.exists()) {
            file.mkdir();
        }
        file2.mkdir();
        if (!file3.exists()) {
            file3.mkdir();
        }
        this.linkPS("default");
        File file5 = new File(file2 + File.separator + "server.properties");
        this.pl.utils.debug("serverProp = " + file5.toString());
        if (!file5.exists()) {
            this.pl.copyResource(file5);
        }
        File file6 = new File(file2 + File.separator + "spigot.yml");
        this.pl.utils.debug("spigotYml = " + file6.toString());
        if (!file6.exists()) {
            this.pl.copyResource(file6);
        }
        this.templateDone("default");
    }
    
    public boolean templateDone(String template) {
        File file = new File(this.pl.getDataFolder(), "templates" + File.separator + template + File.separator + "add-spigot-jar-here.txt");
        File file2 = new File(this.pl.getDataFolder(), "templates" + File.separator + template);
        if (file2.isDirectory()) {
            String[] list = file2.list();
            Pattern compile = Pattern.compile("(?i)(spigot|folia|server|paperspigot|craftbukkit|cauldron|kcauldron|minecraft-server|minecraft_server|forge)(.+)?(\\.jar)");
            String[] array = list;
            for (int length = array.length, i = 0; i < length; ++i) {
                if (compile.matcher(array[i]).find()) {
                    if (file.exists()) {
                        file.delete();
                    }
                    return true;
                }
            }
        }
        this.pl.utils.log(Level.WARNING, "Spigot.jar file not in template folder! Please add your spigot.jar file (as well as any plugins and custom configs) to the 'template' directory in the PlayerServers bungee plugin folder.");
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException ex) {
                this.pl.utils.debug("Failed to create canary file.");
                ex.printStackTrace();
            }
        }
        if (file.exists()) {
            this.pl.utils.log("Loaded default template files. Please add your spigot jar, and any other custom files to the template folder, inside the PlayerServers bungee plugin directory.");
            this.pl.utils.log("Delete the 'add-spigot-jar-here.txt' file to hide this message.");
        }
        return false;
    }
    
    public void doConvertCopy(File file, File file2) {
        if (file.isDirectory()) {
            for (File file3 : file.listFiles()) {
                this.doConvertCopy(file3, new File(file2, file3.getName()));
            }
        }
        else if (file.getName().equalsIgnoreCase("PlayerServers.jar")) {
            this.linkPS("default");
        }
        else {
            this.pl.utils.copyFile(file, file2);
        }
    }
    
    public void linkPS(String template) {
        File file = new File(new File(this.pl.getDataFolder() + File.separator + "templates" + File.separator + template) + File.separator + "plugins");
        if (!file.exists()) {
            file.mkdir();
        }
        Path path = new File(this.pl.getFile().getAbsolutePath()).toPath();
        this.pl.utils.debug("pluginFile = " + path.toString());
        Path path2 = new File(file + File.separator + "PlayerServers.jar").toPath();
        this.pl.utils.debug("pluginCopy = " + path2.toString());
        
        try {
            Files.createSymbolicLink(path2, path, (FileAttribute<?>[])new FileAttribute[0]);
        }
        catch (FileAlreadyExistsException ex2) {
        	try {
				Files.delete(path2.toAbsolutePath());
				linkPS(template);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            this.pl.utils.debug("Plugin file already existed when trying to create default.");
        }
        catch (FileSystemException | UnsupportedOperationException ex3) {
            this.pl.utils.log(Level.WARNING, "Failed to create symbolic link fo PlayerServers plugin .jar to template folder, creating a copy instead.");
            this.pl.utils.log(Level.WARNING, "Symbolic links may not be available on Windows, or certain file systems.");
            this.pl.utils.log(Level.WARNING, "Be sure to update ALL player server files when updating PlayerServers!");
            this.pl.utils.log(Level.WARNING, "You may want to create a script to update them all.");
            this.pl.utils.copyFile(path.toFile(), path2.toFile());
        }
        catch (IOException ex) {
            this.pl.utils.log(Level.SEVERE, "Failed to copy PlayerServers pl file to template. Please manually add it to the templates pl folder, and/or send this stack trace to the developer.");
            ex.printStackTrace();
        }
    }
}
