package net.cakemine.playerservers.bungee.wrapper;

import net.cakemine.playerservers.bungee.*;
import java.net.*;
import java.util.logging.*;
import java.io.*;

public class Controller implements Runnable {
    PlayerServers pl;
    Socket connection;
    BufferedReader read;
    BufferedWriter write;
    private int retryTime;
    private int retries;
    private int maxRetries;
    private boolean firstStart;
    private boolean shutdown;
    private String ip;
    private int port;
    
    public Controller(PlayerServers pl) {
        this.retryTime = 10;
        this.retries = 0;
        this.maxRetries = 10;
        this.firstStart = true;
        this.shutdown = false;
        this.ip = pl.wrapperAddress;
        this.pl = pl;
        this.port = pl.wrapperPort;
    }
    
    @Override
    public void run() {
        this.connect();
    }
    
    public Socket getSocket() {
        return this.connection;
    }
    
    public OutputStream getOutputStream() {
        try {
            return this.connection.getOutputStream();
        }
        catch (IOException ex) {
            return null;
        }
    }
    
    public InputStream getInputStream() {
        try {
            return this.connection.getInputStream();
        }
        catch (IOException ex) {
            return null;
        }
    }
    
    public int getPort() {
        return this.port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getAddress() {
        return this.ip;
    }
    
    public void setAddress(String ip) {
        this.ip = ip;
    }
    
    public void startWrapper() {
        this.pl.utils.debug("PSWrapper starting up on port " + this.port + " from '" + this.pl.getDataFolder().getAbsolutePath() + File.separator + "scripts" + File.separator + "PSWrapper.jar'");
        this.pl.utils.debug("PSWrapper using servers in folder: " + this.pl.serversFolder);
        String[] array;
        if (this.pl.debug) {
            array = new String[] { "java", "-jar", "-Djava.util.logging.SimpleFormatter.format=%1$tH:%1$tM:%1$tS %4$s:%5$s%n", "PSWrapper.jar", String.valueOf(this.port), "debug" };
        }
        else {
            array = new String[] { "java", "-jar", "-Djava.util.logging.SimpleFormatter.format=%1$tH:%1$tM:%1$tS %4$s:%5$s%n", "PSWrapper.jar", String.valueOf(this.port) };
        }
        this.pl.proxy.getScheduler().runAsync(this.pl, new Runnable() {
            @Override
            public void run() {
                ProcessBuilder directory = new ProcessBuilder(array).directory(new File(Controller.this.pl.getDataFolder(), "scripts"));
                try {
                    directory.start();
                    Controller.this.pl.utils.log("PSWrapper started up.");
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    public void connect() {
        try {
            this.tryReconnect();
            Thread.sleep(500L);
            while (!this.shutdown) {
                String line = this.read.readLine();
                if (line != null) {
                    this.pl.utils.log("[PSWrapper] " + line);
                }
                else {
                    this.pl.utils.log(Level.WARNING, "[PSWrapper] Lost connection, reconnecting...");
                    this.tryReconnect();
                    Thread.sleep(15000L);
                }
            }
        }
        catch (IOException ex) {}
        catch (InterruptedException ex2) {}
    }
    
    public void reconnect() {
    	this.pl.utils.log(Level.WARNING, "[PSWrapper] Lost connection, reconnecting...");
		this.shutdown = true;
        try {
			this.connection.close();
		} catch (IOException e) {}
        connect();
    }
    
    public void disconnect() {
        try {
            if (this.connection.isConnected() || !this.connection.isClosed()) {
                this.shutdown = true;
                this.pl.utils.log("Closing connection to PSWrapper.");
                this.connection.close();
            }
        }
        catch (IOException ex) {}
    }
    
    public void tryReconnect() {
        try {
            if (this.connection != null) {
                this.connection.close();
            }
            if (this.read != null) {
                this.read.close();
            }
        }
        catch (IOException ex) {
        	System.out.println("Error: " + ex);
        }
        try {
            this.connection = new Socket(this.ip, this.port);
            this.pl.utils.log("Connected PSWrapper at " + this.ip + ":" + this.port);
            InputStreamReader inputStreamReader = new InputStreamReader(this.connection.getInputStream());
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.connection.getOutputStream());
            this.read = new BufferedReader(inputStreamReader);
            this.write = new BufferedWriter(outputStreamWriter);
            this.retries = 0;
        }
        catch (IOException ex2) {
            try {
            	if (this.firstStart) {
                    this.pl.utils.log("Starting up PSWrapper and retrying connection in " + this.retryTime / 2 + " seconds...");
                    this.firstStart = false;
                    this.startWrapper();
                    Thread.sleep(this.retryTime * 500);
                    this.tryReconnect();
                    return;
                }
                this.pl.utils.log(Level.WARNING, "Failed to connect to " + this.ip + ":" + this.port);
                if (this.retries < this.maxRetries) {
                    this.pl.utils.log(this.maxRetries - this.retries + " tries left. Retrying in " + this.retryTime + " seconds...");
                    Thread.sleep(this.retryTime * 1000);
                    ++this.retries;
                    this.tryReconnect();
                }
                else {
                    this.pl.utils.log("Max retries (" + this.maxRetries + ") reached!");
                    if (this.pl.wrapper.equalsIgnoreCase("default")) {
                        this.pl.utils.log("Relaunching PSWrapper.");
                        this.retries = 0;
                        this.startWrapper();
                        Thread.sleep(10000L);
                    }
                    else {
                        this.pl.utils.log("Retrying in 1 minute.");
                        Thread.sleep(60000L);
                        this.retries = 0;
                    }
                }
            }
            catch (InterruptedException ex3) {}
        }
    }
    
    public void send(String s) {
        if (!s.startsWith("+heartbeat")) {
            this.pl.utils.log("Sending command: " + s);
        }
        try {
            this.write.write(s + '\n');
            this.write.flush();
        }
        catch (SocketException e) {
        	if (e.getMessage().equalsIgnoreCase("Connection reset by peer")) {
        		reconnect();
        	} else {
        		this.pl.utils.log(Level.WARNING, "Failed to connect to " + this.ip + ":" + this.port);
        		this.pl.utils.log(Level.WARNING, "Please be sure the wrapper is running and " + this.ip + ":" + this.port + "is correct!");
        	}
        }
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
