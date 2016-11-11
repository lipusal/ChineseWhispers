package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: make it extend from another manager to share code with ClosingManager!!!!

/**
 * This class is in charge of sending error messages according to the error situation each
 * stored {@link XMPPHandler} reached.
 * In order to perform its task, this class stores a {@link Runnable} - in the {@link TCPSelector} - that
 * is in charge of writing to the stored handlers the corresponding message.
 * This task can be executed whenever, but it's not thread safe.
 * Note that once the message was completely written into a given handler, that handler will be notified to close.
 * (i.e. the method {@link XMPPHandler#notifyClose()} will be called).
 * <p>
 * This class implements the singleton pattern.
 * <p>
 * Created by jbellini on 11/11/16.
 */
public class ErrorManager {

    /**
     * String to be sent when detecting error.
     * It does not contain </stream:stream> because close_message is always sent when closing.
     */
    private final static String BAD_FORMAT = "<stream:error>\n" +
            "<bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n" +
            "</stream:error>\n";

    private final static String POLICY_VIOLATION = "<stream:error>\n" +
            "<policy-violation xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n" +
            "</stream:error>\n";

    private final static String SYSTEM_SHUTDOWN = "<stream:error>\n" +
            "<system-shutdown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n" +
            "</stream:error>\n";

    private final static String INTERNAL_SERVER_ERROR = "<stream:error>\n" +
            "<internal-server-error xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n" +
            "</stream:error>\n";

    private final Map<XMPPHandler, byte[]> errorHandlers;

    private final HashMap<XMPPErrors, String> errorMessages;


    /**
     * Holds the singleton.
     */
    private static ErrorManager singleton;


    /**
     * Private constructor (only called by the {@link ErrorManager#getInstance()} method.
     */
    private ErrorManager() {
        this.errorHandlers = new HashMap<>();
        this.errorMessages = new HashMap<>();
        this.errorMessages.put(XMPPErrors.BAD_FORMAT, BAD_FORMAT);
        this.errorMessages.put(XMPPErrors.POLICY_VIOLATION, POLICY_VIOLATION);
        this.errorMessages.put(XMPPErrors.SYSTEM_SHUTDOWN, SYSTEM_SHUTDOWN);
        this.errorMessages.put(XMPPErrors.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);
        // Stores in the TCP Selector "always run" set a task that is in charge of sending error messages.
        TCPSelector.getInstance().addAlwaysRunTask(() -> {

            // TODO: before sending error, all remaining data must be sent

            // Stores those messages that must be removed from the map
            // (i.e. those whose message has been completely written).
            Set<XMPPHandler> toRemoveHandlers = new HashSet<>();
            for (XMPPHandler each : errorHandlers.keySet()) {
                byte[] message = errorHandlers.get(each);
                int writtenData = each.writeMessage(errorHandlers.get(each));
                // If no bytes could be stored, finish this iteration step
                if (writtenData == 0) {
                    continue;
                }
                // If message wasn't completely stored, save what couldn't be stored to write it afterwards.
                if (writtenData < message.length) {
                    int nonWrittenBytes = message.length - writtenData;
                    byte[] restOfMessage = new byte[nonWrittenBytes];
                    System.arraycopy(message, writtenData, restOfMessage, 0, nonWrittenBytes);
                    errorHandlers.put(each, restOfMessage);
                } else {
                    toRemoveHandlers.add(each); // Message was completely written
                    // Once the error message is completely written, the corresponding handler must be closed.
                    each.notifyClose();
                }
            }
            // Remove all handlers whose message has been completely written
            toRemoveHandlers.forEach(errorHandlers::remove);
        });
    }


    /**
     * Gets the singleton instance.
     *
     * @return The only instance in all the system.
     */
    public static ErrorManager getInstance() {
        if (singleton == null) {
            singleton = new ErrorManager();
        }
        return singleton;
    }

    /**
     * Stores the given {@link XMPPHandler} in this manager, saving it with the corresponding error message
     * (according to the given {@link XMPPErrors} value).
     *
     * @param handler The handler that reached an error situation.
     * @param error   The error it reached.
     */
    public void notifyError(XMPPHandler handler, XMPPErrors error) {
        errorHandlers.put(handler, errorMessages.get(error).getBytes());
    }


    /**
     * Returns whether there is unsent data in this manager for the given {@link XMPPHandler}
     *
     * @param handler The handler that must be checked if any message is still waiting for writing
     * @return {@code true} if there is unsent data, or {@code false} otherwise.
     */
    public boolean stillNotCompletelySent(XMPPHandler handler) {
        // If the map contains the handler as a key, it has unsent data.
        // Otherwise, it would have been removed from the map.
        return errorHandlers.containsKey(handler);
    }

}
