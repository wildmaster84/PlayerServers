package net.cakemine.playerservers.bungee.sync;

import net.md_5.bungee.api.plugin.*;
import net.cakemine.playerservers.bungee.*;
import net.md_5.bungee.api.event.*;
import java.util.logging.*;
import java.util.zip.GZIPInputStream;
import java.util.*;
import java.io.*;
import net.md_5.bungee.api.connection.*;
import net.md_5.bungee.event.*;

public class PluginListener implements Listener
{
    PlayerServers pl;
    
    public PluginListener(PlayerServers pl) {
        this.pl = pl;
    }
    
    private String[] getArgsArray(String s, String s2, String s3, String s4) {
        String[] array;
        if (s2.length() > 0 && s3.length() > 0 && s4.length() > 0) {
            array = new String[] { s, s2, s3, s4 };
        }
        else if (s2.length() > 0 && s3.length() > 0) {
            array = new String[] { s, s2, s3 };
        }
        else if (s2.length() > 0 && s3.length() < 1) {
            array = new String[] { s, s2 };
        }
        else {
            array = new String[] { s };
        }
        this.pl.utils.debug("getArgsArray returning: " + Arrays.toString(array));
        return array;
    }
    
    @EventHandler
    public void onPluginMessage(PluginMessageEvent pluginMessageEvent) {
        if (pluginMessageEvent.getTag().equals("playerservers:core")) {
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(pluginMessageEvent.getData())));
            try {
                String utf = dataInputStream.readUTF();
                this.pl.utils.debug("Received " + utf + " plugin message");
                String s = utf;
                switch (s) {
                    case "sendHelpMessage": {
                        this.pl.utils.helpMessage(this.pl.utils.getSender(dataInputStream.readUTF()), dataInputStream.readUTF(), dataInputStream.readUTF());
                        break;
                    }
                    case "confirmSync": {
                        String utf2 = dataInputStream.readUTF();
                        String utf3 = dataInputStream.readUTF();
                        this.pl.utils.debug("confirmSync values: " + utf2 + " " + utf3);
                        Server currentServer = this.pl.utils.getCurrentServer(utf2);
                        if (currentServer == null) {
                            this.pl.utils.debug("Sync with server of " + utf2 + " failed! Server was null.");
                            break;
                        }
                        if (utf3.equalsIgnoreCase("confirm")) {
                            this.pl.utils.debug("Sync with server " + currentServer.getInfo().getName() + " successful!");
                            this.pl.sender.syncedServers.add(currentServer);
                            break;
                        }
                        this.pl.utils.debug("server: " + currentServer);
                        this.pl.utils.debug("serverinfo: " + currentServer.getInfo());
                        this.pl.utils.debug("servername: " + currentServer.getInfo().getName());
                        this.pl.utils.log(Level.WARNING, "Sync with server " + currentServer.getInfo().getName() + " failed! Retrying.");
                        this.pl.sender.reSync(currentServer);
                        break;
                    }
                    case "reSync": {
                        String utf4 = dataInputStream.readUTF();
                        if (utf4 == null || utf4.isEmpty() || utf4.equalsIgnoreCase("") || utf4.equalsIgnoreCase("null")) {
                            this.pl.utils.log(Level.WARNING, "Invalid senderID on reSync: (" + String.valueOf(utf4) + ")");
                            break;
                        }
                        this.pl.sender.reSync(this.pl.utils.getCurrentServer(utf4));
                        break;
                    }
                    case "version": {
                        String utf5 = dataInputStream.readUTF();
                        String utf6 = dataInputStream.readUTF();
                        Server currentServer2 = this.pl.utils.getCurrentServer(utf5);
                        if (utf6.equalsIgnoreCase("mismatch")) {
                            this.pl.utils.log(Level.WARNING, "&b&l===============================================");
                            this.pl.utils.log(Level.WARNING, " &bVersion mismatch on server: " + currentServer2.getInfo().getName());
                            this.pl.utils.log(Level.WARNING, "&b&l===============================================");
                            break;
                        }
                        break;
                    }
                    case "APIcall": {
                        String utf7 = dataInputStream.readUTF();
                        String utf8 = dataInputStream.readUTF();
                        String s2 = utf7;
                        switch (s2) {
                            case "deleteServer": {
                                PlayerServers.getApi().deleteServer(UUID.fromString(utf8));
                                break;
                            }
                            case "createServer": {
                                PlayerServers.getApi().createServer(UUID.fromString(utf8), dataInputStream.readUTF());
                                break;
                            }
                            case "removeBungeeServer": {
                                PlayerServers.getApi().removeBungeeServer(utf8);
                                break;
                            }
                            case "addBungeeServer": {
                                PlayerServers.getApi().addBungeeServer(utf8, dataInputStream.readUTF(), (int)Integer.valueOf(dataInputStream.readUTF()), dataInputStream.readUTF(), Integer.valueOf(dataInputStream.readUTF()));
                                break;
                            }
                            case "stopAllServers": {
                                PlayerServers.getApi().stopAllServers();
                                break;
                            }
                            case "stopServerPlayer": {
                                PlayerServers.getApi().stopServerPlayer(utf8);
                                break;
                            }
                            case "stopServerUUID": {
                                PlayerServers.getApi().stopServerUUID(UUID.fromString(utf8));
                                break;
                            }
                            case "stopServerName": {
                                PlayerServers.getApi().stopServerName(utf8);
                                break;
                            }
                            case "startServerPlayer": {
                                PlayerServers.getApi().startServerPlayer(utf8);
                                break;
                            }
                            case "startServerUUID": {
                                PlayerServers.getApi().startServerUUID(UUID.fromString(utf8));
                                break;
                            }
                            case "startServerName": {
                                PlayerServers.getApi().startServerName(utf8);
                                break;
                            }
                        }
                        break;
                    }
                    case "permissionCheck": {
                        String utf9 = dataInputStream.readUTF();
                        String utf10 = dataInputStream.readUTF();
                        HashMap<String, String> hashMap = new HashMap<String, String>();
                        String[] split = utf10.split("%%%");
                        for (int length = split.length, i = 0; i < length; ++i) {
                            String[] split2 = split[i].split(":");
                            if (split2[1].equalsIgnoreCase("true")) {
                                hashMap.put(split2[0], split2[1]);
                            }
                        }
                        if (utf9 == null || utf9.isEmpty() || utf9.equalsIgnoreCase("") || utf9.equalsIgnoreCase("null")) {
                            this.pl.utils.debug("Invalid senderID on permissionCheck: (" + String.valueOf(utf9) + ")");
                            break;
                        }
                        if ((hashMap.size() > 0 && !this.pl.permMap.containsKey(utf9)) || (this.pl.permMap.containsKey(utf9) && !this.pl.permMap.get(utf9).equals(hashMap))) {
                            this.pl.permMap.put(utf9, hashMap);
                            this.pl.permMapChanged = true;
                        }
                        this.pl.utils.debug("permMap size = " + this.pl.permMap.size());
                        break;
                    }
                    case "helperAnnounce": {
                        ProxiedPlayer player = this.pl.proxy.getPlayer(UUID.fromString(dataInputStream.readUTF()));
                        if (player == null) {
                            this.pl.utils.log(Level.WARNING, "Player was null on helperAnnounce!");
                            break;
                        }
                        this.pl.usingHelper.put(player, player.getServer());
                        break;
                    }
                    case "APIaddTime": {
                        this.pl.expiryTracker.addTime(dataInputStream.readUTF(), Integer.valueOf(dataInputStream.readUTF()), dataInputStream.readUTF());
                        break;
                    }
                    case "APIremoveTime": {
                        this.pl.expiryTracker.removeTime(dataInputStream.readUTF(), Integer.valueOf(dataInputStream.readUTF()), dataInputStream.readUTF());
                        break;
                    }
                    case "psaCreate": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("create", dataInputStream.readUTF(), dataInputStream.readUTF(), ""));
                        break;
                    }
                    case "psaStart": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("start", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psaStop": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("stop", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psaStopAll": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "stopall" });
                        break;
                    }
                    case "psaJoin": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("join", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psaAddTime": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("addtime", dataInputStream.readUTF(), dataInputStream.readUTF(), dataInputStream.readUTF()));
                        break;
                    }
                    case "psaRemoveTime": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("removetime", dataInputStream.readUTF(), dataInputStream.readUTF(), dataInputStream.readUTF()));
                        break;
                    }
                    case "psaCheckTime": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("checktime", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psaMaxMem": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("maxmem", dataInputStream.readUTF(), dataInputStream.readUTF(), ""));
                        break;
                    }
                    case "psaStartMem": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("startmem", dataInputStream.readUTF(), dataInputStream.readUTF(), ""));
                        break;
                    }
                    case "psaTemplates": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "templates" });
                        break;
                    }
                    case "psaReload": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "reload" });
                        break;
                    }
                    case "psaSlots": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("slots", dataInputStream.readUTF(), dataInputStream.readUTF(), ""));
                        break;
                    }
                    case "psaDelete": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("delete", dataInputStream.readUTF(), dataInputStream.readUTF(), ""));
                        break;
                    }
                    case "psaMotd": {
                        this.pl.playerServerAdmin.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("motd", dataInputStream.readUTF(), dataInputStream.readUTF(), ""));
                        break;
                    }
                    case "psJoin": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("join", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psLeave": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "leave" });
                        break;
                    }
                    case "psCreate": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("create", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psWorlds": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "worlds" });
                        break;
                    }
                    case "psDelete": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "delete", dataInputStream.readUTF() });
                        break;
                    }
                    case "psHome": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "home" });
                        break;
                    }
                    case "psStop": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "stop" });
                        break;
                    }
                    case "psChecktime": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[] { "checktime" });
                        break;
                    }
                    case "psMotd": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("motd", dataInputStream.readUTF(), "", ""));
                        break;
                    }
                    case "psGUI": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), new String[0]);
                        break;
                    }
                    case "psShareTime": {
                        this.pl.playerServer.execute(this.pl.utils.getSender(dataInputStream.readUTF()), this.getArgsArray("sharetime", dataInputStream.readUTF(), dataInputStream.readUTF(), dataInputStream.readUTF()));
                        break;
                    }
                    default: {
                        String utf11 = dataInputStream.readUTF();
                        this.pl.utils.debug("plugin message sender uuid: " + utf11);
                        this.pl.sender.sendStructuredMessage(utf, utf11);
                        break;
                    }
                }
                dataInputStream.close();
            }
            catch (NullPointerException ex) {
                ex.printStackTrace();
            }
            catch (IOException ex2) {
                ex2.printStackTrace();
            }
        }
    }
    
}