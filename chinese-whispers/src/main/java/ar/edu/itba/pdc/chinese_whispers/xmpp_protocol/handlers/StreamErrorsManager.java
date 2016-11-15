package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

/**
 * This class is responsible of sending stream errors messages.
 * This errors are unrecoverable, so once sent, the handler that had all its message sent will be closed.
 * <p>
 * This class implements the singleton pattern.
 * <p>
 * Created by jbellini on 11/11/16.
 */
public class StreamErrorsManager extends ErrorsManager {


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
     * Holds the singleton.
     */
    private final static StreamErrorsManager singleton = new StreamErrorsManager();

    /**
     * Private constructor (only called by the {@link StreamErrorsManager#getInstance()} method.
     */
    private StreamErrorsManager() {

        addErrorMessage(XMPPErrors.BAD_FORMAT, BAD_FORMAT);
        addErrorMessage(XMPPErrors.POLICY_VIOLATION, POLICY_VIOLATION);
        addErrorMessage(XMPPErrors.CONNECTION_REFUSED, CONNECTION_REFUSED);
        addErrorMessage(XMPPErrors.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        addErrorMessage(XMPPErrors.HOST_UNKNOWN, HOST_UNKNOWN);
        addErrorMessage(XMPPErrors.HOST_UNKNOWN_FROM_SERVER, HOST_UNKNOWN_FROM_SERVER);
        addErrorMessage(XMPPErrors.INVALID_AUTH_MECHANISM, INVALID_MECHANISM_FAILURE);
        addErrorMessage(XMPPErrors.MALFORMED_REQUEST, MALFORMED_REQUEST);
        addErrorMessage(XMPPErrors.UNSUPPORTED_NEGOTIATION_MECHANISM, UNSUPPORTED_NEGOTIATION_MECHANISM);
        addErrorMessage(XMPPErrors.UNSUPPORTED_NEGOTIATION_MECHANISM_FOR_CLIENT,
                UNSUPPORTED_NEGOTIATION_MECHANISM_FOR_CLIENT);
        addErrorMessage(XMPPErrors.FAILED_NEGOTIATION, FAILED_NEGOTIATION);
        addErrorMessage(XMPPErrors.FAILED_NEGOTIATION_FOR_SERVER, FAILED_NEGOTIATION_FOR_SERVER);
        addErrorMessage(XMPPErrors.SYSTEM_SHUTDOWN, SYSTEM_SHUTDOWN);
        addErrorMessage(XMPPErrors.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);
    }


    /**
     * Gets the singleton instance.
     *
     * @return The only instance in all the system.
     */
    public static StreamErrorsManager getInstance() {
        return singleton;
    }

    @Override
    protected void afterSendingError(XMPPHandler handler) {
        // Once the error message is completely written, the corresponding handler must be closed.
        handler.notifyStreamErrorWasSent();
    }
}
