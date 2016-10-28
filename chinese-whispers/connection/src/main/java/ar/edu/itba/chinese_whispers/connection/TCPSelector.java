package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jbellini on 27/10/16.
 * <p>
 * This class handles all IO operations.
 * Implements the singleton pattern to have only one selector in all the project.
 */
public final class TCPSelector {

    /**
     * The byte buffer size.
     */
    /* package */ static final int BUFFER_SIZE = 512;
    /**
     * Timeout for the select operation.
     */
    private static final int TIMEOUT = 3000;
    /**
     * The selector to perform IO operations.
     */
    private final Selector selector;
    /**
     * Contains tasks to be run when channel selection times out without selecting anything.
     */
    private final Set<Runnable> tasks;
    /**
     * Maps a {@link SelectionKey} with its corresponding {@link TCPHandler}.
     */
    private final Map<SelectionKey, TCPHandler> handlers;
    /**
     * Contains keys that had errors when selecting.
     */
    private final Set<SelectionKey> errorKeys;


    /**
     * Contains the singleton
     */
    private static TCPSelector instance;


    private TCPSelector() throws IOException {
        this.selector = Selector.open();
        this.tasks = new HashSet<>();
        this.handlers = new HashMap<>();
        this.errorKeys = new HashSet<>();
    }


    public static TCPSelector getInstance() {
        if (instance == null) {
            try {
                instance = new TCPSelector();
            } catch (IOException ignored) {
            }
        }
        return instance;
    }


    public void addTask(Runnable task) {
        tasks.add(task);
    }

    public void removeTaks(Runnable task) {
        tasks.remove(task);
    }


    public boolean addServerSocketChannel(int port, TCPServerHandler handler) throws IOException {
        if (port < 0 || port > Short.MAX_VALUE || handler == null) {
            throw new IllegalArgumentException();
        }
        try {
            ServerSocketChannel channel;
            channel = ServerSocketChannel.open();
            channel.socket().bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
            handlers.put(key, handler);
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    public boolean addClientSocketChannel(String host, int port, TCPClientHandler handler) {
        if (host == null || host.equals("") || port < 0 || port > Short.MAX_VALUE || handler == null) {
            throw new IllegalArgumentException();
        }
        try {
            SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(host, port));
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
            handlers.put(key, handler);
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    public boolean doSelect() {
        try {
            if (selector.select(TIMEOUT) == 0) {
                // No IO operation ...
                tasks.forEach(Runnable::run);
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // If control reached here, there are IO Operations pending to be handled
        for (SelectionKey key : selector.selectedKeys()) {
            TCPHandler handler = handlers.get(key);

            // Shouldn't be null, but in case ...
            if (handler == null) {
                errorKeys.add(key);
                continue;
            }
            try {
                if (key.isAcceptable()) {
                    // Key only can be acceptable if it's channel is a server socket channel
                    SelectionKey newKey = ((TCPServerHandler) handler).handleAccept(key);
                    if (newKey != null) {
                        handlers.put(newKey, handler);
                    }
                }

                if (key.isConnectable()) {
                    // Key only can be connectable if it's channel is a client socket channel
                    Boolean connected = ((TCPClientHandler) handler).handleConnect(key);
                    if (connected) {
                        handlers.put(key, handler);
                    }
                }

                if (key.isReadable()) {
                    handler.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    handler.handleWrite(key);
                }
            } catch (Throwable ignored) {
                // If any error occurred, don't crash ...
                errorKeys.add(key);
            }
        }
        selector.selectedKeys().clear();
        // TODO: close error keys? Cancel them?
        errorKeys.clear(); // Clears the set in order to leave it empty for the next iteration.
        return true;
    }


}
