package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;

import java.util.*;

/**
 * This class is in charge of closing nicely those {@link XMPPHandler} that have been stored.
 * Closing nicely means that those handlers will send the XMPP clossing message (i.e. the </stream> tag).
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
//        closableHandlers.put(handler, CLOSE_MESSAGE.getBytes());
        handler.postMessage(CLOSE_MESSAGE.getBytes());
        handler.requestClose();
    }

}
