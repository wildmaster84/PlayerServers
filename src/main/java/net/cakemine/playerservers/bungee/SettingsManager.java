package net.cakemine.playerservers.bungee;

import java.util.logging.*;
import java.util.*;
import net.cakemine.playerservers.bungee.events.*;
import net.md_5.bungee.api.plugin.*;
import java.io.*;

public class SettingsManager
{
    PlayerServers pl;
    
    public SettingsManager(PlayerServers pl) {
        this.pl = pl;
    }
    
    public boolean propExists(String s) {
        return new File(this.pl.serversFolder + File.separator + s + File.separator + "server.properties").exists();
    }
    
    public String getSetting(String s, String s2) {
        InputStream inputStream = null;
        Properties properties;
        try {
            File file = new File(this.pl.serversFolder + File.separator + s + File.separator + "server.properties");
            if (!file.exists()) {
                this.pl.utils.log(Level.WARNING, "Tried to get \"" + s2 + "\" setting from \"" + this.pl.serversFolder + File.separator + s + File.separator + "server.properties\" but it doesn't exist! Server files deleted?");
                return null;
            }
            inputStream = new FileInputStream(file);
            properties = new Properties();
            properties.load(inputStream);
        }
        catch (IOException ex) {
            this.pl.utils.log(Level.WARNING, "Tried to get \"" + s2 + "\" setting from \"" + this.pl.serversFolder + File.separator + s + File.separator + "server.properties\" but it doesn't exist or isn't readable! Server files deleted?");
            ex.printStackTrace();
            return null;
        }
        finally {
            if (inputStream != null) {
                try {
                	inputStream.close();
                }
                catch (IOException ex2) {
                    ex2.printStackTrace();
                }
            }
        }
        return properties.getProperty(s2);
    }
    
    public void changeSetting(String s, String s2, String s3) {
        ChangePropertiesEvent changePropertiesEvent = new ChangePropertiesEvent(this.pl, s, s2, s3);
        this.pl.proxy.getPluginManager().callEvent((Event)changePropertiesEvent);
        if (!changePropertiesEvent.isCancelled()) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = new FileInputStream(this.pl.serversFolder + File.separator + s + File.separator + "server.properties");
                Properties properties = new Properties();
                properties.load(inputStream);
                outputStream = new FileOutputStream(this.pl.serversFolder + File.separator + s + File.separator + "server.properties");
                properties.setProperty(s2, s3);
                properties.store(outputStream, null);
            }
            catch (IOException ex) {
                this.pl.utils.log(Level.WARNING, "Tried to modify \"" + s2 + "\" in \"" + this.pl.serversFolder + File.separator + s + File.separator + "server.properties\" but file doesn't exist or isn't writeable!  Server files deleted?");
                ex.printStackTrace();
                if (inputStream != null) {
                    try {
                        ((FileInputStream)inputStream).close();
                    }
                    catch (IOException ex2) {
                        ex2.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        ((FileOutputStream)outputStream).close();
                    }
                    catch (IOException ex3) {
                        ex3.printStackTrace();
                    }
                }
            }
            finally {
                if (inputStream != null) {
                    try {
                        ((FileInputStream)inputStream).close();
                    }
                    catch (IOException ex4) {
                        ex4.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        ((FileOutputStream)outputStream).close();
                    }
                    catch (IOException ex5) {
                        ex5.printStackTrace();
                    }
                }
            }
        }
    }
}
