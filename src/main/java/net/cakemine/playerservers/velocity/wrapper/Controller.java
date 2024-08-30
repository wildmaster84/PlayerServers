package net.cakemine.playerservers.velocity.wrapper;

import net.cakemine.playerservers.velocity.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.logging.*;
import java.io.*;

public class Controller implements Runnable {
    private PlayerServers pl;
    private SocketChannel connection;
    private ExecutorService writeExecutor;
    private ScheduledExecutorService retryExecutor;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private int retryTime;
    private int retries;
    private int maxRetries;
    private boolean firstStart;
    private boolean shutdown;
    private String ip;
    private int port;

    public Controller(PlayerServers pl) {
        this.pl = pl;
        this.retryTime = 10;
        this.retries = 0;
        this.maxRetries = 10;
        this.firstStart = true;
        this.shutdown = false;
        this.ip = pl.wrapperAddress;
        this.port = pl.wrapperPort;
        this.writeExecutor = Executors.newSingleThreadExecutor();
        this.retryExecutor = Executors.newScheduledThreadPool(1);
        this.readBuffer = ByteBuffer.allocate(1024);
        this.writeBuffer = ByteBuffer.allocate(1024);
    }

    @Override
    public void run() {
        connect();
    }

    public SocketChannel getSocket() {
        return this.connection;
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
        } else {
            array = new String[] { "java", "-jar", "-Djava.util.logging.SimpleFormatter.format=%1$tH:%1$tM:%1$tS %4$s:%5$s%n", "PSWrapper.jar", String.valueOf(this.port) };
        }
        this.pl.proxy.getScheduler().buildTask(this.pl, () -> {
        	ProcessBuilder directory = new ProcessBuilder(array).directory(new File(Controller.this.pl.getDataFolder(), "scripts"));
            try {
                directory.start();
                Controller.this.pl.utils.log("PSWrapper started up.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void connect() {
        try {
            tryReconnect();
            
            while (!this.shutdown) {
            	if (this.connection != null) {
            		int bytesRead = this.connection.read(readBuffer);
                    if (bytesRead > 0) {
                        readBuffer.flip();
                        String line = StandardCharsets.UTF_8.decode(readBuffer).toString();
                        this.pl.utils.log("[PSWrapper] " + line);
                        readBuffer.clear();
                    } else if (bytesRead == -1) {
                        this.pl.utils.log(Level.WARNING, "[PSWrapper] Lost connection, reconnecting...");
                        tryReconnect();
                        retryExecutor.schedule(this::connect, 15, TimeUnit.SECONDS);
                    }
            	}
                
            }
        } catch (IOException ex) {}
    }

    public void reconnect() {
        this.pl.utils.log(Level.WARNING, "[PSWrapper] Lost connection, reconnecting...");
        this.shutdown = true;
        try {
            this.connection.close();
            connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (this.connection.isConnected()) {
                this.shutdown = true;
                this.pl.utils.log("Closing connection to PSWrapper.");
                this.connection.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void tryReconnect() {
    	if (this.connection != null) {
        	if (this.connection.isOpen()) {
        		try {
					this.connection.close();
				} catch (IOException e) {}
        	}
        }

        try {
            this.connection = SocketChannel.open(new InetSocketAddress(this.ip, this.port));
            this.connection.configureBlocking(false);
            this.pl.utils.log("Connected to PSWrapper at " + this.ip + ":" + this.port);
            this.retries = 0;
        } catch (IOException ex) {
            handleReconnectFailure(ex);
        }
    }

    private void handleReconnectFailure(IOException ex) {
        try {
            if (this.firstStart) {
                this.pl.utils.log("Starting up PSWrapper and retrying connection...");
                this.firstStart = false;
                if (!(ex instanceof BindException)) {
                	this.startWrapper();
                } else {
                	this.pl.utils.log("Reconnecting to wrapper");
                }
                retryExecutor.schedule(this::tryReconnect, this.retryTime / 2, TimeUnit.SECONDS);
                return;
            }
            this.pl.utils.log(Level.WARNING, "Failed to connect to " + this.ip + ":" + this.port);
            if (this.retries < this.maxRetries) {
                this.pl.utils.log(this.maxRetries - this.retries + " tries left. Retrying in " + this.retryTime + " seconds...");
                retryExecutor.schedule(this::tryReconnect, this.retryTime, TimeUnit.SECONDS);
                ++this.retries;
            } else {
                handleMaxRetriesReached();
            }
        } catch (InterruptedException ex2) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleMaxRetriesReached() throws InterruptedException {
        this.pl.utils.log("Max retries (" + this.maxRetries + ") reached!");
        if (this.pl.wrapper.equalsIgnoreCase("default")) {
            this.pl.utils.log("Relaunching PSWrapper.");
            this.retries = 0;
            this.startWrapper();
            Thread.sleep(10000L);
        } else {
            this.pl.utils.log("Retrying in 1 minute.");
            retryExecutor.schedule(this::tryReconnect, 1, TimeUnit.MINUTES);
            this.retries = 0;
        }
    }

    public void send(String s) {
        if (!s.startsWith("+heartbeat")) {
            this.pl.utils.log("Sending command: " + s);
        }
        String query = s + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(query.getBytes(StandardCharsets.UTF_8));

        try {
			this.connection.write(buffer);
		} catch (SocketException e) {
            handleSocketException(e);
        } catch (IOException e) {
           
        }
    }

    private void handleSocketException(SocketException e) {
        if (e.getMessage().equalsIgnoreCase("Connection reset by peer")) {
            reconnect();
        } else {
            this.pl.utils.log(Level.WARNING, "Failed to connect to " + this.ip + ":" + this.port);
            this.pl.utils.log(Level.WARNING, "Please ensure the wrapper is running and the connection details are correct!");
        }
    }
}
