package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.application.IdGenerator;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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


    private final static String INITIAL_TAG_UNCLOSED = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<stream:stream version='1.0' " +
            "xmlns:stream=\'http://etherx.jabber.org/streams\' " +
            "xmlns=\'jabber:client\' " +
            "xmlns:xml=\'http://www.w3.org/XML/1998/namespace\'";

    // XML errors
    private final static String BAD_FORMAT = "<stream:error>" +
            "<bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>";
    private final static String POLICY_VIOLATION = "<stream:error>" +
            "<policy-violation xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>";


    // TCP level errors
    private final static String CONNECTION_REFUSED = "<stream:error>" +
            "<remote-connection-failed xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>";
    private final static String CONNECTION_TIMEOUT = "<stream:error>" +
            "<connection-timeout xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>";

    // Negotiation errors
    private final static String HOST_UNKNOWN = "<stream:error>" +
            "<host-unknown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>";
    private final static String HOST_UNKNOWN_FROM_SERVER = CONNECTION_REFUSED; // Same XMPP response
    private final static String INVALID_MECHANISM_FAILURE = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
            "<invalid-mechanism/>" +
            "</failure>";
    private final static String MALFORMED_REQUEST = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
            "<malformed-request/>" +
            "</failure>";
    private final static String UNSUPPORTED_NEGOTIATION_MECHANISM = "<abort xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>";
    private final static String UNSUPPORTED_NEGOTIATION_MECHANISM_FOR_CLIENT = CONNECTION_REFUSED; // Same XMPP response
    private final static String FAILED_NEGOTIATION = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
            "<not-authorized/>" +
            "</failure>";
    private final static String FAILED_NEGOTIATION_FOR_SERVER = UNSUPPORTED_NEGOTIATION_MECHANISM; // Same XMPP response


    // System errors
    private final static String SYSTEM_SHUTDOWN = "<stream:error>" +
            "<system-shutdown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>\n";
    private final static String INTERNAL_SERVER_ERROR = "<stream:error>" +
            "<internal-server-error xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
            "</stream:error>\n";


    /**
     * Set containing the {@link ParserResponse} values that are errors.
     */
    private final Set<ParserResponse> parserResponseErrors;
    /**
     * Holds the message that mus be sent for each value of the {@link XMPPErrors} enum.
     */
    private final HashMap<XMPPErrors, String> errorMessages;
    /**
     * Holds those {@link XMPPHandler}s that reached an error situation.
     * The value is the message that must be sent.
     */
    private final Map<XMPPHandler, byte[]> errorHandlers;


    /**
     * Holds the singleton.
     */
    private static ErrorManager singleton;

    /**
     * Private constructor (only called by the {@link ErrorManager#getInstance()} method.
     */
    private ErrorManager() {
        this.parserResponseErrors = new HashSet<>();
        parserResponseErrors.add(ParserResponse.XML_ERROR);
        parserResponseErrors.add(ParserResponse.POLICY_VIOLATION);
        parserResponseErrors.add(ParserResponse.HOST_UNKNOWN);
        parserResponseErrors.add(ParserResponse.INVALID_AUTH_MECHANISM);
        parserResponseErrors.add(ParserResponse.MALFORMED_REQUEST);
        parserResponseErrors.add(ParserResponse.UNSUPPORTED_NEGOTIATION_MECHANISM);
        parserResponseErrors.add(ParserResponse.FAILED_NEGOTIATION);

        this.errorMessages = new HashMap<>();
        this.errorMessages.put(XMPPErrors.BAD_FORMAT, BAD_FORMAT);
        this.errorMessages.put(XMPPErrors.POLICY_VIOLATION, POLICY_VIOLATION);
        this.errorMessages.put(XMPPErrors.CONNECTION_REFUSED, CONNECTION_REFUSED);
        this.errorMessages.put(XMPPErrors.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        this.errorMessages.put(XMPPErrors.HOST_UNKNOWN, HOST_UNKNOWN);
        this.errorMessages.put(XMPPErrors.HOST_UNKNOWN_FROM_SERVER, HOST_UNKNOWN_FROM_SERVER);
        this.errorMessages.put(XMPPErrors.INVALID_AUTH_MECHANISM, INVALID_MECHANISM_FAILURE);
        this.errorMessages.put(XMPPErrors.MALFORMED_REQUEST, MALFORMED_REQUEST);
        this.errorMessages.put(XMPPErrors.UNSUPPORTED_NEGOTIATION_MECHANISM, UNSUPPORTED_NEGOTIATION_MECHANISM);
        this.errorMessages.put(XMPPErrors.UNSUPPORTED_NEGOTIATION_MECHANISM_FOR_CLIENT,
                UNSUPPORTED_NEGOTIATION_MECHANISM_FOR_CLIENT);
        this.errorMessages.put(XMPPErrors.FAILED_NEGOTIATION, FAILED_NEGOTIATION);
        this.errorMessages.put(XMPPErrors.FAILED_NEGOTIATION_FOR_SERVER, FAILED_NEGOTIATION_FOR_SERVER);
        this.errorMessages.put(XMPPErrors.SYSTEM_SHUTDOWN, SYSTEM_SHUTDOWN);
        this.errorMessages.put(XMPPErrors.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);

        this.errorHandlers = new HashMap<>();
        // Stores in the TCP Selector "always run" set a task that is in charge of sending error messages.
        TCPSelector.getInstance().addAlwaysRunTask(() -> {

            // Stores those messages that must be removed from the map
            // (i.e. those whose message has been completely written).
            Set<XMPPHandler> toRemoveHandlers = new HashSet<>(); //TODO se hace SIEMPRE. optimizar.
            for (XMPPHandler each : errorHandlers.keySet()) {
                byte[] message = errorHandlers.get(each);
                // If no message was sent, the <stream:stream> tag is prepended
                if (each.firstMessage()) { //agregar al closing?
                    byte[] initialTag = (INITIAL_TAG_UNCLOSED+" id='"+IdGenerator.generateId() + "'>").getBytes();
                    byte[] aux = new byte[initialTag.length + message.length];
                    System.arraycopy(initialTag, 0, aux, 0, initialTag.length);
                    System.arraycopy(message, 0, aux, initialTag.length, message.length);
                    message = aux;
                }
                int writtenData = each.writeMessage(message);
                // If no bytes could be stored, finish this iteration step
                if (writtenData == 0) {
                    continue;
                }
                // If message wasn't completely stored, save what couldn't be stored to write it afterwards.
                //TODO check if -1 here can happend and if needed
                if (writtenData!=-1 && writtenData < message.length) {
                    int nonWrittenBytes = message.length - writtenData;
                    byte[] restOfMessage = new byte[nonWrittenBytes]; //TODO porque no byte buffer?
                    System.arraycopy(message, writtenData, restOfMessage, 0, nonWrittenBytes);
                    errorHandlers.put(each, restOfMessage);
                } else {
                    toRemoveHandlers.add(each); // Message was completely written
                    // Once the error message is completely written, the corresponding handler must be closed.
                    each.notifyErrorWasSent();
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

    public Set<ParserResponse> parserResponseErrors() {
        return parserResponseErrors;
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
