package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.application.IdGenerator;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Base errors manager. It is in charge of sending errors messages according to the error situation each
 * stored {@link XMPPHandler} reached.
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
     * Posts the corresponding error message (according to the given {@link XMPPErrors} value),
     * to the given {@link XMPPHandler}.
     *
     * @param handler The handler that reached an error situation.
     * @param error   The error it reached.
     */
    public void notifyError(XMPPHandler handler, XMPPErrors error) {
//        if (handler.firstMessage()) {
//            handler.postMessage((INITIAL_TAG_UNCLOSED + " id='" + IdGenerator.generateId() + "'>").getBytes());
//        }
//        handler.postMessage(errorMessages.get(error).getBytes());
//        afterSendingError(handler);
        doNotify(handler, errorMessages.get(error).getBytes());
    }

    /**
     * Performs the error posting action.
     *
     * @param handler The handler that reached an error situation.
     * @param message The mesage to be posted to the given handler.
     */
    protected void doNotify(XMPPHandler handler, byte[] message) {
        if (handler.firstMessage()) {
            handler.postMessage((INITIAL_TAG_UNCLOSED + " id='" + IdGenerator.generateId() + "'>").getBytes());
        }
        handler.postMessage(message);
        afterSendingError(handler);
    }

}
