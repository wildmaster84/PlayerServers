package net.cakemine.playerservers.bukkit;

import java.io.*;
import org.bukkit.*;
import java.util.*;

public class SettingsManager
{
    PlayerServers pl;
    
    public SettingsManager(PlayerServers pl) {
        this.pl = pl;
    }
    
    public String getSetting(String s) {
        File file = new File("");
        InputStream inputStream = null;
        Properties properties;
        try {
            inputStream = new FileInputStream(file.getAbsolutePath() + File.separator + "server.properties");
            properties = new Properties();
            properties.load(inputStream);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        finally {
            if (inputStream != null) {
                try {
                    ((FileInputStream)inputStream).close();
                }
                catch (IOException ex2) {
                    ex2.printStackTrace();
                }
            }
        }
        return properties.getProperty(s);
    }
    
    public void changeSetting(String s, String s2) {
        File file = new File("");
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(file.getAbsolutePath() + File.separator + "server.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            outputStream = new FileOutputStream(file.getAbsolutePath() + File.separator + "server.properties");
            properties.setProperty(s, s2);
            properties.store(outputStream, null);
        }
        catch (IOException ex) {
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
    
    public void setGamemode(int n) {
        this.pl.settingsManager.changeSetting("gamemode", String.valueOf(n));
        GameMode defaultGameMode = null;
        switch (n) {
            case 0: {
                defaultGameMode = GameMode.SURVIVAL;
                break;
            }
            case 1: {
                defaultGameMode = GameMode.CREATIVE;
                break;
            }
            case 2: {
                defaultGameMode = GameMode.ADVENTURE;
                break;
            }
            case 3: {
                defaultGameMode = GameMode.SPECTATOR;
                break;
            }
            default: {
                defaultGameMode = GameMode.SURVIVAL;
                n = 0;
                break;
            }
        }
        Bukkit.setDefaultGameMode(defaultGameMode);
    }
    
    public void setDifficulty(int n) {
        Difficulty difficulty = null;
        switch (n) {
            case 0: {
                difficulty = Difficulty.PEACEFUL;
                break;
            }
            case 1: {
                difficulty = Difficulty.EASY;
                break;
            }
            case 2: {
                difficulty = Difficulty.NORMAL;
                break;
            }
            case 3: {
                difficulty = Difficulty.HARD;
                break;
            }
            default: {
                difficulty = Difficulty.EASY;
                n = 1;
                break;
            }
        }
        Iterator<World> iterator = (Iterator<World>)Bukkit.getWorlds().iterator();
        while (iterator.hasNext()) {
            iterator.next().setDifficulty(difficulty);
        }
        this.pl.settingsManager.changeSetting("difficulty", String.valueOf(n));
    }
    
    public void setPvP(boolean pvp) {
        this.pl.settingsManager.changeSetting("pvp", String.valueOf(pvp));
        Iterator<World> iterator = Bukkit.getWorlds().iterator();
        while (iterator.hasNext()) {
            iterator.next().setPVP(pvp);
        }
    }
}
