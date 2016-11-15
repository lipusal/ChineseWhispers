package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.application.IdGenerator;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Base errors manager. It is in charge of sending errors messages according to the error situation each
 * stored {@link XMPPHandler} reached.
 * In order to perform its task, this class defines a {@link Runnable}, and stores it in the {@link TCPSelector}
 * "always-run" set.
 * <p>
 * Created by jbellini on 14/11/16.
 */
/* package */ abstract class ErrorsManager {


    private final static String INITIAL_TAG_UNCLOSED = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<stream:stream version='1.0' " +
            "xmlns:stream=\'http://etherx.jabber.org/streams\' " +
            "xmlns=\'jabber:client\' " +
            "xmlns:xml=\'http://www.w3.org/XML/1998/namespace\'";


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


    protected ErrorsManager() {
        this.parserResponseErrors = new HashSet<>();
        parserResponseErrors.add(ParserResponse.XML_ERROR);
        parserResponseErrors.add(ParserResponse.POLICY_VIOLATION);
        parserResponseErrors.add(ParserResponse.HOST_UNKNOWN);
        parserResponseErrors.add(ParserResponse.INVALID_AUTH_MECHANISM);
        parserResponseErrors.add(ParserResponse.MALFORMED_REQUEST);
        parserResponseErrors.add(ParserResponse.UNSUPPORTED_NEGOTIATION_MECHANISM);
        parserResponseErrors.add(ParserResponse.FAILED_NEGOTIATION);

        this.errorMessages = new HashMap<>();
        this.errorHandlers = new HashMap<>();

        TCPSelector.getInstance().addAlwaysRunTask(() -> {

            // Stores those messages that must be removed from the map
            // (i.e. those whose message has been completely written).
            Set<XMPPHandler> toRemoveHandlers = new HashSet<>(); //TODO se hace SIEMPRE. optimizar.
            for (XMPPHandler each : errorHandlers.keySet()) {
                byte[] message = errorHandlers.get(each);
                // If no message was sent, the <stream:stream> tag is prepended
                if (each.firstMessage()) { //agregar al closing?
                    byte[] initialTag = (INITIAL_TAG_UNCLOSED + " id='" + IdGenerator.generateId() + "'>").getBytes();
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
                if (writtenData != -1 && writtenData < message.length) {
                    int nonWrittenBytes = message.length - writtenData;
                    byte[] restOfMessage = new byte[nonWrittenBytes]; //TODO porque no byte buffer?
                    System.arraycopy(message, writtenData, restOfMessage, 0, nonWrittenBytes);
                    errorHandlers.put(each, restOfMessage);
                } else {
                    toRemoveHandlers.add(each); // Message was completely written
                    afterSendingError(each);
                }
            }
            // Remove all handlers whose message has been completely written
            toRemoveHandlers.forEach(errorHandlers::remove);
        });
    }


    /**
     * Actions performed after sending errors completely.
     *
     * @param handler The handler that received an error message completely.
     */
    protected abstract void afterSendingError(XMPPHandler handler);

    /**
     * Allows subclasses add more parser responses to the set.
     *
     * @param response The {@link ParserResponse} that will be added to the set.
     */
    protected void addParserResponseErrors(ParserResponse response) {
        this.parserResponseErrors.add(response);
    }


    protected void addErrorMessage(XMPPErrors errors, String message) {
        if (errors == null || message == null) {
            return;
        }
        errorMessages.put(errors, message);
    }


    /**
     * Returns a set containing those {@link ParserResponse} values that are considered as errors.
     *
     * @return The set with {@link ParserResponse} that are errors.
     */
    public Set<ParserResponse> parserResponseErrors() {
        return new HashSet<>(parserResponseErrors); // Another set in case it's modified by caller.
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
