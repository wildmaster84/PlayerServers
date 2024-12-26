package net.cakemine.playerservers.velocity;

import java.util.logging.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import java.nio.file.attribute.*;
import java.nio.file.*;

public class TemplateManager
{
    PlayerServers pl;
    public HashMap<File, Map<String, Object>> templates;
    
    public TemplateManager(PlayerServers pl) {
        this.templates = new HashMap<File, Map<String, Object>>();
        this.pl = pl;
    }
    
    public void loadTemplates() {
        File file = new File(this.pl.configManager.getDataFolder() + File.separator + "templates");
        if (!file.exists()) {
            this.defaultTemplate();
        }
        if (file.isDirectory()) {
            for (File file3 : file.listFiles()) {
                this.templates.put(file3, this.loadTemplateConfig(file3));
                this.updateConfig(file3);
                this.linkPS(file3.getName());
                this.pl.utils.log("Loaded template: " + this.pl.templateManager.getTemplateSetting(file3, "template-name"));
            }
        }
    }
    
    public void updateConfig(File file) {
        if (this.templates.get(file).get("default-expiry-time") == null) {
        	this.templates.get(file).put("default-expiry-time", "1 day");
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + "config with default-expiry-time option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("shutdown-on-expire") == null) {
        	this.templates.get(file).put("shutdown-on-expire", false);
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + " config with shutdown-on-expire option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("owner-join-message") == null) {
        	this.templates.get(file).put("owner-join-message", "&eWelcome the owner of the server!||&6&o%player%&e has joined!");
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + " config with owner-join-message option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("owner-join-commands") == null) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("#enter custom commands to run on join here.");
            list.add("#commands starting with # won't be ran.");
            list.add("#placeholders: %owner-name%, %owner-uuid%, %template-name%, %server-name%, %server-port%");
            this.templates.get(file).put("owner-join-commands", (Object)list);
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + " config with owner-join-commands option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("default-Xmx") == null) {
        	this.templates.get(file).put("default-Xmx", (Object)this.pl.defaultMem.get("xmx"));
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + "config with default-Xmx option.");
            this.saveTemplateConfig(file);
        }
        if (this.templates.get(file).get("default-Xms") == null) {
        	this.templates.get(file).put("default-Xms", (Object)this.pl.defaultMem.get("xms"));
            this.pl.utils.debug("Updated template " + this.getTemplateSetting(file, "template-name") + "config with default-Xms option.");
            this.saveTemplateConfig(file);
        }
    }
    
    public Map<String, Object> loadTemplateConfig(File file) {
        if (file.isDirectory()) {
            File file2 = new File(file, "PlayerServers.yml");
            if (!file2.exists()) {
                this.pl.configManager.copyResource(file2);
            }
            return this.pl.configManager.yaml.load(this.getClass().getClassLoader().getResourceAsStream("PlayerServers.yml"));
        }
        return null;
    }
    
    public void saveTemplateConfig(File file) {
        if (file.isDirectory()) {
            this.pl.configManager.saveConfig(this.templates.get(file), "PlayerServers.yml");
            this.loadTemplateConfig(file);
        }
    }
    
    public File getTemplateFile(String templateName) {
        this.pl.utils.debug("getTemplateFile input: " + templateName);
        String stripColor = this.pl.utils.stripColor(this.pl.utils.color(templateName)).content();
        this.pl.utils.debug("stripped color: " + stripColor);
        for (Entry<File, Map<String, Object>> entry : this.templates.entrySet()) {
            File file = entry.getKey();
            String stripColor2 = this.pl.utils.stripColor(this.pl.utils.color(entry.getValue().get("template-name").toString())).content();
            this.pl.utils.debug("tempFile: " + file + " | tempName: " + stripColor2);
            if (stripColor2.equalsIgnoreCase(stripColor) || file.getName().equalsIgnoreCase(templateName) || file.getName().equalsIgnoreCase(stripColor)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public String getTemplateSetting(File file, String s) {
        if (this.templates.containsKey(file)) {
            return this.templates.get(file).get(s).toString();
        }
        return null;
    }
    
    public List<String> getTemplateSettingList(File file, String s) {
        if (this.templates.containsKey(file)) {
            return (List)this.templates.get(file).get(s);
        }
        return null;
    }
    
    public int expireTime(File file) {
        if (this.templates.get(file).get("default-expiry-time") == null || this.templates.get(file).get("default-expiry-time").toString().isEmpty()) {
            return 1;
        }
        return Integer.valueOf(this.templates.get(file).get("default-expiry-time").toString().replaceAll("(\\s|month|mnth|mnt|mth|mo|week|wk|wek|w|day|dy|d|hour|hr|h|minute|min|mi|m)(s)?", ""));
    }
    
    public String expireUnit(File file) {
        if (this.templates.get(file).get("default-expiry-time") == null || this.templates.get(file).get("default-expiry-time").toString().isEmpty()) {
            return "day";
        }
        return this.templates.get(file).get("default-expiry-time").toString().replaceAll("[0-9]|\\s", "");
    }
    
    public void defaultTemplate() {
        File file = new File(this.pl.configManager.getDataFolder() + File.separator + "templates");
        File file2 = new File(this.pl.configManager.getDataFolder() + File.separator + "templates" + File.separator + "default");
        File file3 = new File(file2 + File.separator + "plugins");
        File file4 = new File(this.pl.configManager.getDataFolder() + File.separator + "template");
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
            this.pl.configManager.copyResource(file5);
        }
        File file6 = new File(file2 + File.separator + "spigot.yml");
        this.pl.utils.debug("spigotYml = " + file6.toString());
        if (!file6.exists()) {
            this.pl.configManager.copyResource(file6);
        }
        this.templateDone();
    }
    
    public boolean templateDone() {
        File file = new File(this.pl.configManager.getDataFolder(), "templates" + File.separator + "default" + File.separator + "add-spigot-jar-here.txt");
        File file2 = new File(this.pl.configManager.getDataFolder(), "templates" + File.separator + "default");
        if (file2.isDirectory()) {
            String[] list = file2.list();
            Pattern compile = Pattern.compile("(?i)(spigot|folia|server|paper|paperspigot|craftbukkit|cauldron|kcauldron|minecraft-server|minecraft_server|forge)(.+)?(\\.jar)");
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
        File file = new File(this.pl.configManager.getDataFolder() + File.separator + "templates" + File.separator + template + File.separator + "plugins");
        if (!file.exists()) {
            file.mkdir();
        }
        Pattern pattern = Pattern.compile("PlayerServers-[a-zA-Z0-9.-]+.jar");
        File[] pluginFiles = this.pl.configManager.getDataFolder().getParentFile().listFiles((dir, name) -> pattern.matcher(name).matches());
        if (pluginFiles != null && pluginFiles.length > 0) {
            for (File plugin : pluginFiles) {
            	this.pl.utils.debug("pluginFile = " + plugin.getPath());
            	File path2 = new File(file.getPath() + File.separator + "PlayerServers.jar");

            	try {
            		Files.createSymbolicLink(path2.toPath(), plugin.toPath().toAbsolutePath(), (FileAttribute<?>[])new FileAttribute[0]);
                }
                catch (FileAlreadyExistsException ex2) {
                	try {
                		Files.delete(path2.toPath().toAbsolutePath());
                		linkPS(template);
                	} catch (IOException e) {
                		e.printStackTrace();
                	}
                	this.pl.utils.debug("Plugin file already existed when trying to create default.");
                }
            	catch (FileSystemException | UnsupportedOperationException ex3) {
            		this.pl.utils.log(Level.WARNING, "Failed to create symbolic link fo PlayerServers plugin .jar to template folder, creating a copy instead.");
            		this.pl.utils.log(Level.WARNING, "Symbolic links may not be available on Windows, or certain file systems.");
            		this.pl.utils.log(Level.WARNING, "Be sure to update ALL player server files when updating PlayerServers!");
            		this.pl.utils.log(Level.WARNING, "You may want to create a script to update them all.");
            		this.pl.utils.copyFile(plugin, path2);
            	}
            	catch (IOException ex) {
            		this.pl.utils.log(Level.SEVERE, "Failed to copy PlayerServers pl file to template. Please manually add it to the templates pl folder, and/or send this stack trace to the developer.");
            		ex.printStackTrace();
                }
            }
        }
    }
}
