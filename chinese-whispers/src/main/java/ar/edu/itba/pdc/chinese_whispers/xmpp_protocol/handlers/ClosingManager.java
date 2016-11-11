package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;

import java.util.*;

/**
 * This class is in charge of closing nicely those {@link XMPPHandler} that have been stored.
 * Closing nicely means that those handlers will send the XMPP clossing message (i.e. the </stream> tag).
 * In order to perform its task, this class stores a {@link Runnable} - in the {@link TCPSelector} - that
 * is in charge of writing to the stored handlers the corresponding message.
 * This task can be executed whenever, but it's not thread safe.
 * Note that once the message was completely written into a given handler, it will be closed
 * (i.e. the corresponding channel will be closed).
 * <p>
 * This class implements the singleton pattern.
 * <p>
 * Created by jbellini on 11/11/16.
 */
public class ClosingManager {

    /**
     * String to be sent to finish communication
     */
    private final static String CLOSE_MESSAGE = "</stream:stream>\n";


    /**
     * Stores the closable handlers, and the message to be sent.
     */
    private final Map<XMPPHandler, byte[]> closableHandlers;

    /**
     * Holds the singleton.
     */
    private static ClosingManager singleton;


    /**
     * Private constructor (only called by the {@link ClosingManager#getInstance()} method.
     */
    private ClosingManager() {
        this.closableHandlers = new HashMap<>();
        // Stores in the TCP Selector "always run" set a task that is in charge of sending closing messages.
        TCPSelector.getInstance().addAlwaysRunTask(() -> {


            // TODO: before closing, all remaining data must be sent

            // Stores those messages that must be removed from the map
            // (i.e. those whose message has been completely written).
            Set<XMPPHandler> toRemoveHandlers = new HashSet<>();
            for (XMPPHandler each : closableHandlers.keySet()) {
                byte[] message = closableHandlers.get(each);
                int writtenData = each.writeMessage(closableHandlers.get(each));
                // If no bytes could be stored, finish this iteration step
                if (writtenData == 0) {
                    continue;
                }
                // If message wasn't completely stored, save what couldn't be stored to write it afterwards.
                if (writtenData < message.length) {
                    int nonWrittenBytes = message.length - writtenData;
                    byte[] restOfMessage = new byte[nonWrittenBytes];
                    System.arraycopy(message, writtenData, restOfMessage, 0, nonWrittenBytes);
                    closableHandlers.put(each, restOfMessage);
                } else {
                    toRemoveHandlers.add(each); // Message was completely written
                    each.requestClose(); // Now it is ready for being closed.
                    // TODO: make handler close
                }
            }
            // Remove all handlers whose message has been completely written
            toRemoveHandlers.forEach(closableHandlers::remove);
        });
    }

    /**
     * Gets the singleton instance.
     *
     * @return The only instance in all the system.
     */
    public static ClosingManager getInstance() {
        if (singleton == null) {
            singleton = new ClosingManager();
        }
        return singleton;
    }


    /**
     * Stores an {@link XMPPHandler} in this manager, in order to write into it the closing message.
     *
     * @param handler The handler that want to be closed.
     */
    public void notifyClose(XMPPHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException();
        }
        if (closableHandlers.containsKey(handler)) {
            return;
        }
        closableHandlers.put(handler, CLOSE_MESSAGE.getBytes());
    }

}
