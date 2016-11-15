package ar.edu.itba.pdc.chinese_whispers.connection;

import ar.edu.itba.pdc.chinese_whispers.application.Configurations;
import ar.edu.itba.pdc.chinese_whispers.application.LogHelper;
import ar.edu.itba.pdc.chinese_whispers.application.MetricsManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

/**
 * Created by jbellini on 27/10/16.
 * <p>
 * This class handles all IO operations.
 * Implements the singleton pattern to have only one selector in all the project.
 * To register a channel, a {@link TCPHandler} must be registered with it
 * as the select operation uses it to perform operations.
 * Contains a set of nothingToDoTasks to be performed in case no IO events were triggered when selecting.
 */
public final class TCPSelector {


    /**
     * Timeout for the select operation.
     */
    private static final int SELECT_TIMEOUT = 3000;
    /**
     * Timeout till the connection is closed.
     */
    private static final int CONNECTION_TIMEOUT = 5 * 60000; // 5 minutes
    /**
     * Max amount of connection tries till key is cancelled.
     */
    private static final int MAX_CONNECTION_TRIES = 10;
    /**
     * Max amount of TCP connections allowed.
     */
    private static final int MAX_AMOUNT_OF_CONNECTIONS = 500;

    /**
     * The selector to perform IO operations.
     */
    private final Selector selector;
    /**
     * Contains all the accepted keys that are connected.
     */
    private final Set<SelectionKey> acceptedKeys;
    /**
     * Contains when the last activity took place.
     */
    private final Map<SelectionKey, Long> lastActivities;
    /**
     * Tasks that are performed always before the select operation.
     */
    private final Set<Runnable> alwaysRunTasks;
    /**
     * Contains nothingToDoTasks to be run when channel selection times out without selecting anything.
     */
    private final Set<Runnable> nothingToDoTasks;
    /**
     * Contains connectable keys that didn't connect yet, saving how many tries were done.
     */
    private final Map<SelectionKey, Integer> connectionTries;
    /**
     * Set used by the timeout task to remove keys with issues.
     */
    private final Set<SelectionKey> removableKeys;


    private final Logger logger;
    /**
     * Contains the singleton.
     */
    private static TCPSelector instance;


    /**
     * Private constructor (for singleton pattern).
     *
     * @throws IOException If the selector couldn't be opened.
     */
    private TCPSelector() throws IOException {
        this.selector = Selector.open();
        this.acceptedKeys = new HashSet<>();
        this.lastActivities = new HashMap<>();
        this.alwaysRunTasks = new HashSet<>();
        this.nothingToDoTasks = new HashSet<>();
        this.connectionTries = new HashMap<>();
        removableKeys = new HashSet<>();
        this.logger = LogHelper.getLogger(getClass());
        alwaysRunTasks.add(() -> {
            // Checks the timeout for each key in the lastActivities map's key set.
            removableKeys.clear();
            for (SelectionKey key : lastActivities.keySet()) {
                if (!key.isValid()) {
                    removableKeys.add(key);
                    continue;
                }
                if (key.attachment() instanceof TCPAcceptorHandler) {
                    removableKeys.add(key); // Don't check again acceptor handlers.
                    continue; // Do nothing with these handlers.
                }
                if (!(key.attachment() instanceof TCPTimeoutCancellableHandler)) {
                    // Shouldn't reach this point
                    // In case one attachment is not a TCPHandler, that key is cancelled
                    key.cancel();
                    removableKeys.add(key);
                    continue;
                }
                TCPTimeoutCancellableHandler handler = (TCPTimeoutCancellableHandler) key.attachment();
                Long lastActivity = lastActivities.get(key);
                long currentTime = System.currentTimeMillis();
                if (lastActivity == null) {
                    // The key's first activity was not registered...
                    registerTimeoutCancelableKey(key, currentTime);
                    continue;
                }
                if (currentTime - lastActivity >= CONNECTION_TIMEOUT) {

                    handler.handleTimeout(key);
                    // Updates the last activity timestamp
                    registerTimeoutCancelableKey(key, currentTime);
                }
            }
            removableKeys.forEach(lastActivities::remove); // Remove all those keys with problems
        });
        alwaysRunTasks.add(() -> {
            // This task updates the accepted key set,
            // Removing those that are not contained in the selector's keys set.
            Iterator<SelectionKey> it = acceptedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey each = it.next();
                // If the selector does not contains the key, it means that the connection was closed.
                if (!selector.keys().contains(each)) {
                    it.remove();
                }
            }
        });
    }


    /**
     * Gets the singleton instance.
     *
     * @return The only instance of this class.
     */
    public static TCPSelector getInstance() {
        if (instance == null) {
            try {
                instance = new TCPSelector();
            } catch (IOException ignored) {
                // Will return null if IOException is thrown
            }
        }
        return instance;
    }

    /**
     * Adds the key in the last activities map.
     * Note that the key attachment must not be null, and must implement {@link TCPTimeoutCancellableHandler} interface.
     *
     * @param key The key that must be check for timeout.
     */
    private void registerTimeoutCancelableKey(SelectionKey key) {
        registerTimeoutCancelableKey(key, System.currentTimeMillis());
    }

    /**
     * Adds the key in the last activities map.
     * Note that the key attachment must not be null, and must implement {@link TCPTimeoutCancellableHandler} interface.
     * If the key already had a registered timestamp, and the given timestamp is smaller that the registered one,
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param key       The key that must be check for timeout.
     * @param timestamp The new timestamp
     * @throws IllegalArgumentException if the the key already had a registered timestamp,
     *                                  and the given timestamp is smaller that the registered one
     */
    private void registerTimeoutCancelableKey(SelectionKey key, long timestamp) {
        if (key.attachment() == null || !(key.attachment() instanceof TCPTimeoutCancellableHandler)) {
            return; // Do nothing with keys that are not of our interest.
        }
        Long oldTimestamp = lastActivities.get(key);
        if (oldTimestamp != null && timestamp < oldTimestamp) {
            throw new IllegalArgumentException();
        }
        lastActivities.put(key, System.currentTimeMillis());
    }


    /**
     * Adds a task to be performed when no IO operations where selected.
     *
     * @param task The task to be performed.
     */
    public void addAlwaysRunTask(Runnable task) {
        alwaysRunTasks.add(task);
    }

    /**
     * Removes the given task from the set of nothingToDoTasks to be performed when no IO operations where selected.
     *
     * @param task The task to be removed.
     */
    public void removeAlwaysRunTask(Runnable task) {
        alwaysRunTasks.remove(task);
    }

    /**
     * Adds a task to be performed when no IO operations where selected.
     *
     * @param task The task to be performed.
     */
    public void addNothingToDoTask(Runnable task) {
        nothingToDoTasks.add(task);
    }

    /**
     * Removes the given task from the set of nothingToDoTasks to be performed when no IO operations where selected.
     *
     * @param task The task to be removed.
     */
    public void removeNothingToDoTask(Runnable task) {
        nothingToDoTasks.remove(task);
    }

    /**
     * Adds a server socket channel to this selector.
     *
     * @param port    The port in which the server socket channel will be bond and listen for incoming connections.
     * @param handler A {@link TCPAcceptorHandler} to handle the accept operation.
     * @return The {@link SelectionKey} representing the new connection if the socket channel was bound,
     * or {@code null} otherwise.
     */
    public SelectionKey addServerSocketChannel(int port, TCPAcceptorHandler handler) {
        if (port < 0 || port > 0xFFFF || handler == null) {
            throw new IllegalArgumentException();
        }
        try {
            // Will throw exception if the socket couldn't be opened.
            ServerSocketChannel channel = ServerSocketChannel.open();
            // Will throw exception if the socket couldn't bind, or if the socket is already bound.
            channel.socket().bind(new InetSocketAddress(port));
            // Will throw exception is the channel was closed (can't happen this)
            channel.configureBlocking(false);
            // Will throw exception if the channel was closed (can't happen this)
            return channel.register(selector, SelectionKey.OP_ACCEPT, handler);
        } catch (IOException e) {
            return null;
        }

    }


    /**
     * Adds a client socket channel to this selector, and starts connecting it (in non-blocking mode).
     *
     * @param host    The host to be connected with.
     * @param port    The port in which the host is listening.
     * @param handler A {@link TCPClientHandler} to handle selected IO operations.
     * @return The {@link SelectionKey} representing the new connection, if the socket could start connecting,
     * or {@code null} otherwise.
     */
    public SelectionKey addClientSocketChannel(String host, int port, TCPClientHandler handler) {
        if (host == null || host.isEmpty() || port < 0 || port > 0xFFFF || handler == null) {
            throw new IllegalArgumentException();
        }
        try {
            // Will throw exception if the socket couldn't be opened.
            SocketChannel channel = SocketChannel.open();
            // Will throw exception is the channel was closed (can't happen this)
            channel.configureBlocking(false);
            // Will throw exception if connection couldn't start, or name can't be resolved
            channel.connect(new InetSocketAddress(host, port));
            // Will throw exception if the channel was closed (can't happen this)
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT, handler);
            // Saves the first activity for the new key
            registerTimeoutCancelableKey(key);
            return key;
        } catch (IOException | UnresolvedAddressException e) {
            return null;
        }

    }

    /**
     * Logs the given {@link Throwable}.
     * That's the message (if any), and all the stacktrace.
     *
     * @param e
     */
    private void logException(Throwable e) {
        if (e.getMessage() == null) {
            logger.error("Exception when trying to perform an \"always-run\" task");
        } else {
            logger.error("Exception when trying to perform an \"always-run\" task. Message {}", e.getMessage());
        }
        logger.error("Stacktrace:");
        for (StackTraceElement each : e.getStackTrace()) {
            logger.error(each.toString());
        }
    }

    /**
     * Performs IO operations. If no IO event was triggered, the added nothingToDoTasks will be performed.
     *
     * @return {@code true} if IO events where triggered, or {@code false} otherwise.
     */
    public boolean doSelect() {
        try {
            alwaysRunTasks.forEach(Runnable::run); // Run all tasks that are required to run always
        } catch (Throwable e) {
            logException(e);
        }
        try {
            if (selector.select(SELECT_TIMEOUT) == 0) {
                // No IO operation ...
                try {
                    nothingToDoTasks.forEach(Runnable::run);
                } catch (Throwable e) {
                    logException(e);
                }
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // If control reached here, there are IO Operations pending to be handled
        for (SelectionKey key : selector.selectedKeys()) {
            try {
                TCPHandler handler = (TCPHandler) key.attachment();

                // Shouldn't be null, but in case ...
                if (handler == null) {
                    key.cancel();
                    continue;
                }
                registerTimeoutCancelableKey(key); // registers a new timestamp
                // Only valid keys with a TCPHandler as an attachment will reach this point...
                if (key.isAcceptable()) {
                    // Key can only be acceptable if it's channel is a server socket channel
                    SelectionKey newKey = ((TCPAcceptorHandler) handler).handleAccept(key);
                    if (newKey != null) {
                        // Saves the first activity for the new connection
                        registerTimeoutCancelableKey(newKey);
                        if (acceptedKeys.size() >= MAX_AMOUNT_OF_CONNECTIONS) {
                            newKey.cancel(); // No more connections allowed.
                            try {
                                newKey.channel().close();
                            } catch (Throwable e) {
                                newKey.cancel(); // Allows cancelling the new key
                            }
                            continue;
                        }
                        acceptedKeys.add(newKey); // Adds the new key in the accepted keys.
                        MetricsManager.getInstance().addAccesses(1);
                    }
                } else if (key.isConnectable()) {
                    // Key can only be connectable if it's channel is a client socket channel
                    ((TCPClientHandler) handler).handleConnect(key);
                    if (key.isValid()) {
                        // Key could have been invalidated if connection was refused
                        afterTryingConnection(key); // Check if connection was established
                    }
                } else {
                    // If key is acceptable or connectable, it mustn't reach this point...
                    // Keys up to this point are valid, as they were selected and no method has been called.
                    // Only ReadWriteHandlers's key will be readable or writable, or else the key will be cancelled
                    if (key.isReadable()) {
                        ((TCPReadWriteHandler) handler).handleRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        ((TCPReadWriteHandler) handler).handleWrite(key);
                    }
                }
            } catch (Throwable e) {
                try {
                    key.channel().close();
                } catch (Throwable anotherThrowable) {
                    key.cancel();
                }
                logException(e);
            }
        }
        selector.selectedKeys().clear();
        return true;
    }


    /**
     * Checks if the connection was established for the channel of the given {@link SelectionKey}.
     * If it wasn't the amount of tries are updated.
     * If that amount of tries is greater or equals to MAX_CONNECTION_TRIES, then the given {@code key} is cancelled.
     *
     * @param key The {@link SelectionKey} whose channel must be checked it it was connected.
     */
    private void afterTryingConnection(SelectionKey key) {
        if (((SocketChannel) key.channel()).isConnectionPending()) {
            Integer tries = connectionTries.get(key);
            if (tries == null) {
                tries = 0;
            }
            tries++;
            if (tries >= MAX_CONNECTION_TRIES) {
                key.cancel();
                connectionTries.remove(key);
            } else {
                connectionTries.put(key, tries);
            }
        }
    }

}