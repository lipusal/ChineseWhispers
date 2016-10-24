package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;

/**
 * Created by jbellini on 24/10/16.
 * <p>
 * This class handles ...
 */
public class Connector {


    private static final int TIMEOUT = 1000;


    private static int nextIdentifier = 0;

    /**
     * Contains opened connection.
     * The {@code Integer} represents an identifier, and the {@code SelectionKey} the key representing the connection.
     */
    private final Map<Integer, SelectionKey> openedConnections;
    /**
     * Inverse mapping of opened connections
     */
    private final Map<SelectionKey, Integer> reverseOpenedConnections;
    /**
     * Stores read messages.
     */
    private final Map<Integer, List<byte[]>> readMessageQueues;
    /**
     * Stores messages to be written.
     */
    private final Map<Integer, List<byte[]>> writeMessageQueues;
    /**
     * The selector that will listen events.
     */
    private final Selector selector;
    /**
     * A set of selector threads that will perform I/O operations.
     */
    private final Set<SelectorThread> selectorThreads;
    /**
     * The max. amount of selector threads.
     */
    private final int maxThreads;

    /**
     * Thread that controls creation and destruction of Selector Threads
     */
    private final Thread controllingThread;

    /**
     * Object to be notified of new connections
     */
    private final NewConnectionsNotifiable notifiable;

    /**
     * Constructor.
     *
     * @param maxThreads The max. amount of threads.
     * @throws IOException If an I/O error occurs when opening the selector.
     */
    public Connector(NewConnectionsNotifiable notifiable, int maxThreads) throws IOException {
        // This code is executed by this connector's creator (externally)
        openedConnections = new HashMap<>();
        reverseOpenedConnections = new HashMap<>();
        readMessageQueues = new HashMap<>();
        writeMessageQueues = new HashMap<>();
        selector = Selector.open();
        selectorThreads = new HashSet<>();
        this.maxThreads = maxThreads;
        this.notifiable = notifiable;
        this.controllingThread = new Thread(new ControllingThread(2000));
    }


    /**
     * Creates, binds and registers a server socket which listens in the given {@code port}.
     *
     * @param port The port in which the socket will listen.
     */
    public void addSocket(int port) {
        // This code is executed by this connector's creator (externally)
        try {
            ServerSocketChannel listeningChannel = ServerSocketChannel.open();
            listeningChannel.socket().bind(new InetSocketAddress(port));
            listeningChannel.configureBlocking(false); // Non-blocking socket
            synchronized (selector) {
                listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: What should we do here?
        }
    }


    /**
     * Starts listening in the added ports.
     */
    public void startListening() {
        // This code is executed by this connector's creator (externally)
        controllingThread.start();
    }


    public byte[] pollReadMessage(int identifier) {
        // This code is executed externally
        return pollMessage(readMessageQueues, identifier);
    }

    public void offerWriteMessage(int identifier, byte[] message) {
        // This code is executed externally
        offerMessage(writeMessageQueues, identifier, message);
        SelectionKey key;
        // Synchronizes the opened connections map to get the corresponding selection key.
        synchronized (openedConnections) {
            key = openedConnections.get(identifier);
        }
        synchronized (selector) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE); // TODO: Should be done in SelectorThread
        }
//        // Synchronizes the key to change it's interest operations.
//        synchronized (key) {
//            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//        }

    }





    /*
     * Methods to be used by Selector threads.
     */

    /**
     * Gets the selector.
     *
     * @return The selector.
     */
    /* package */  Selector getSelector() {
        // No need of synchronization
        return selector;
    }

    /**
     * Adds a connection to the identifier - SelectionKey mapping.
     *
     * @param key The key that represents the new connection.
     */
    /* package */ void registerConnection(SelectionKey key) {
        int identifier;
        synchronized (this) {
            identifier = nextIdentifier++;
        }
        synchronized (openedConnections) {
            openedConnections.put(identifier, key);
        }
        synchronized (reverseOpenedConnections) {
            reverseOpenedConnections.put(key, identifier);
        }
    }

    /**
     * Removes a connection from the identifier - SelectionKey mapping
     *
     * @param key The key that represents the connection
     * @return {@code true} if the key was removed, or {@code false} otherwise.
     */
    /* package */ boolean unregisterConnection(SelectionKey key) {
        int identifier = getIdentifier(key);
        if (identifier == -1) {
            return false;
        }
        synchronized (openedConnections) {
            openedConnections.remove(key);
        }
        synchronized (reverseOpenedConnections) {
            reverseOpenedConnections.remove(identifier);
        }
        return true;
    }

    /**
     * Adds the given read {@code message} into the read messages map.
     * The given {@code key} is mapped to the corresponding identifier.
     *
     * @param key     The {@link SelectionKey} whose channel received the message.
     * @param message The received message.
     * @return {@code true} if the message was added, or {@©code false} otherwise.
     */
    /* package */ boolean offerReadMessage(SelectionKey key, byte[] message) {
        int identifier = getIdentifier(key);
        if (identifier < 0) {
            // TODO: should we throw an exception?
            return false;
        }
        offerMessage(readMessageQueues, identifier, message);
        return true;
    }

    /**
     * Gets the next message to write to the given key's channel.
     * The given {@code key} is mapped to the corresponding identifier.
     *
     * @param key The {@link SelectionKey} whose channel must be written.
     * @return The next message to be written.
     */
    /* package */ byte[] pollWriteMessage(SelectionKey key) {
        byte[] result = null;
        int identifier = getIdentifier(key);
        if (identifier < 0) {
            // TODO: should we throw an exception?
            return null;
        }
        return pollMessage(writeMessageQueues, identifier);
    }





    /*
     * Helper methods
     */

    /**
     * Gets the identifier for the given key.
     *
     * @param key The key's whose identifier must be retrieved.
     * @return The key's identifier.
     */
    private int getIdentifier(SelectionKey key) {
        Integer identifier;
        synchronized (reverseOpenedConnections) {
            identifier = reverseOpenedConnections.get(key);
        }
        return (identifier == null) ? -1 : identifier;
    }

    /**
     * Gets the next message for the given identifier in the given map.
     *
     * @param map        The map where new messages will be checked.
     * @param identifier The connection identifier.
     * @return The next messages in the given map for the given identifier.
     */
    private byte[] pollMessage(Map<Integer, List<byte[]>> map, int identifier) {
        byte[] result = null;
        synchronized (map) {
            List<byte[]> messages = map.get(identifier);
            if (messages != null) {
                Iterator<byte[]> it = messages.iterator();
                if (it.hasNext()) {
                    result = it.next();
                    it.remove();
                }
            }
        }
        return result;
    }

    /**
     * Adds the given message to the given map, using the given identifier as the key.
     *
     * @param map        The map were the message must be added.
     * @param identifier The connection identifier.
     * @param message    The message to be added.
     */
    private void offerMessage(Map<Integer, List<byte[]>> map, int identifier, byte[] message) {
        synchronized (map) {
            List<byte[]> messages = map.get(identifier);
            if (messages == null) {
                messages = new LinkedList<>();
                map.put(identifier, messages);
            }
            messages.add(message);
        }
    }



    /*
     * Controlling thread
     */


    private class ControllingThread implements Runnable {

        private final int sleepTime;


        private ControllingThread(int sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            while (true) {

                // TODO: Check how to destroy threads?

                // No synchronization as this is the only thread that accesses this set.
                int actualThreads = selectorThreads.size();

                int actualKeys;
                // Synchronizes the connector's selector's keys set.
                synchronized (selector.keys()) {
                    actualKeys = selector.keys().size();
                }

                if (actualKeys > actualThreads * SelectorThread.MAX_KEYS_PER_THREAD
                        && actualThreads < maxThreads) {
                    SelectorThread thread = new SelectorThread(Connector.this, TIMEOUT);
                    selectorThreads.add(thread);
                    new Thread(thread).start();
                }

                // TODO: notify the notifiable object
                synchronized (notifiable) {
                    notifiable.notifyNewConnections(openedConnections.keySet()); // TODO: mark those already notified
                }
                try {
                    Thread.sleep(sleepTime); // Sleeps for the specified sleeping time, then it starts again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
