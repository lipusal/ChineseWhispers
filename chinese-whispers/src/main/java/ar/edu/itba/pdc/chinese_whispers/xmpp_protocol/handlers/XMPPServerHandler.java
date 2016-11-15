package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;


import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NewConnectionsConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ServerNegotiationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;

import java.nio.channels.SelectionKey;
import java.util.Base64;

/**
 * This class makes the proxy act like an XMPP server, negotiating with the connected client.
 * The process of negotiation is done by a {@link ServerNegotiationProcessor}. When this process finishes,
 * a connection with the origin server is tried. Upon success, an {@link XMPPClientHandler} is created,
 * in order to connect to the server, and negotiate with it.
 * Once the negotiation with the origin server ended, control is given to a new {@link XMPPReadWriteHandler},
 * which will be in charge of reading and writing to the connected client.
 * <p>
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerHandler extends XMPPNegotiatorHandler {

    /**
     * Says how many times the peer connection can be tried.
     */
    private static int MAX_PEER_CONNECTIONS_TRIES = 3;


    /**
     * The new connections consumer that will be notified when new connections arrive.
     */
    private final NewConnectionsConsumer newConnectionsConsumer;

    /**
     * Holds how many peer connection tries have been done.
     */
    private int peerConnectionTries;

    /**
     * A string builder to create strings efficiently
     */
    private final StringBuilder stringBuilder;


//    private final ServerNegotiationProcessor serverNegotiationProcessor;


    /**
     * Constructor.
     * It MUST only be called by {@link XMPPAcceptorHandler}, when a client-proxy TCP connection has been established.
     *
     * @param applicationProcessor   An object that can process XMPP messages bodies.
     * @param newConnectionsConsumer An object that can track new TCP connections.
     * @param configurationsConsumer An object that can be queried about which server each user must connect to.
     * @param metricsProvider        An object that manages the system metrics.
     * @param key                    The {@link SelectionKey} that corresponds to this handler.
     */
    /* package */ XMPPServerHandler(ApplicationProcessor applicationProcessor,
                                    NewConnectionsConsumer newConnectionsConsumer,
                                    ConfigurationsConsumer configurationsConsumer,
                                    MetricsProvider metricsProvider,
                                    SelectionKey key) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        setNegotiationProcessor(new ServerNegotiationProcessor(this));
        this.newConnectionsConsumer = newConnectionsConsumer;
        this.peerConnectionTries = 0;
        this.key = key;
        this.stringBuilder = new StringBuilder();
    }

    @Override
    /* package */ void setKey(SelectionKey key) {
        throw new UnsupportedOperationException("Key can't be changed to an XMPPServerHandler");
    }




    @Override
    protected void beforeRead() {
        // TODO: check own output buffer
        inputBuffer.clear();
    }

    @Override
    protected void afterWrite() {
        // Nothing to be done...
    }

    /**
     * Makes this handler's key attach a new {@link XMPPReadWriteHandler}, and start operating as a proxy.
     * <p>
     * Note: This method MUST only be called once proxy-server negotiation ended.
     */
    /* package */ void startProxying(XMPPReadWriteHandler newPeerHandler) {
        if (newPeerHandler == null) {
            throw new IllegalArgumentException();
        }
        if (this.peerHandler == null || !((XMPPClientHandler) this.peerHandler).isConnected()) {
            throw new IllegalStateException();
        }

        // Create a read-write handler that will receive and send messages to the client connected to the proxy.
        XMPPReadWriteHandler xmppReadWriteHandler = new XMPPReadWriteHandler(applicationProcessor, metricsProvider,
                configurationsConsumer, clientJid, this.key, newPeerHandler);
        newPeerHandler.setPeerHandler(xmppReadWriteHandler);
        this.key.attach(xmppReadWriteHandler);

        String response = "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>";
        xmppReadWriteHandler.writeMessage(response.getBytes());
        xmppReadWriteHandler.enableReading();
    }



    @Override
    protected void handleResponse(ParserResponse parserResponse) {
        super.handleResponse(parserResponse);
        switch (parserResponse) { //TODO porqe solo estos 3?
            case HOST_UNKNOWN:
                notifyStreamError(XMPPErrors.HOST_UNKNOWN);
                break;
            case INVALID_AUTH_MECHANISM:
                notifyStreamError(XMPPErrors.INVALID_AUTH_MECHANISM);
                break;
            case MALFORMED_REQUEST:
                notifyStreamError(XMPPErrors.MALFORMED_REQUEST);
                break;
        }
    }

    /**
     * Finishes the XMPP negotiation process.
     * When executing this method, first parameters are checked in order to make sure that needed params are contained.
     * After that, an {@link XMPPClientHandler}, is created.
     * Finally, the {@link XMPPServerHandler#connectClientHandler()} method is called,
     * requesting a new connection with the origin server.
     * <p>
     * Note: This method must be called only once.
     */
    @Override
    protected void finishXMPPNegotiation() {
        // Stop reading till negotiation ends on the other side (i.e. handler connecting to origin server)
        disableReading();

        String[] authParameters;
        try {
            authParameters = new String(Base64.getDecoder().decode(getNegotiationProcessor().getAuthentication()))
                    .split("\0");
        } catch (IllegalArgumentException e) {
            // Shouldn't reach here, but in case...
            authParameters = null;
        }

        if (authParameters == null) {
            notifyStreamError(XMPPErrors.MALFORMED_REQUEST);
            return;
        }

        String userName;
        switch (authParameters.length) {
            case 2:
                // [username, password]
                userName = authParameters[0];
                break;
            case 3:
                // ["\0", username, password]
                userName = authParameters[1];
                break;
            default:
                // Shouldn't reach here, but in case...
                notifyStreamError(XMPPErrors.MALFORMED_REQUEST);
                return;
        }

        // Create a client handler to connect to origin server
        clientJid = userName + "@" + getNegotiationProcessor().getInitialParameters().get("to");
        this.peerHandler = new XMPPClientHandler(applicationProcessor, metricsProvider, configurationsConsumer, this,
                clientJid, getNegotiationProcessor().getInitialParameters(),
                getNegotiationProcessor().getAuthentication());


        // This will create a new key with the client handler attached to it.
        // It won't connect immediately, but mark the key as connectable and return.
        // The selector from the connection layer will then select the key to finish the connection process.
        connectClientHandler();
    }


    /**
     * Method to be executed to tunnel the client being connected to this server handler into the origin server.
     */
    /* package */ void connectClientHandler() {
        if (this.peerHandler == null || this.peerHandler.getClass() != XMPPClientHandler.class
                || this.clientJid == null) {
            throw new IllegalStateException();
        }

        // TODO: add some timestamp...

        // TODO: Should we avoid trying to connect again for some seconds? We can mark the clientHandler with a timestamp and retry after some time has passed.
        // TODO: We can save the key in a Map and update those timestamps before the select.
        if (peerConnectionTries >= MAX_PEER_CONNECTIONS_TRIES) {
            notifyStreamError(XMPPErrors.CONNECTION_REFUSED);
            return;
        }
        logger.trace("Trying to connect to origin server...");      //TODO which server?
        SelectionKey peerKey = TCPSelector.getInstance().
                addClientSocketChannel(configurationsConsumer.getServer(clientJid),
                        configurationsConsumer.getServerPort(clientJid),
                        (XMPPClientHandler) this.peerHandler);
        if (peerKey == null) {
            // Start of connection failed ...
            handleError(key); // Our own key
            return;
        }
        peerHandler.setKey(peerKey);
        peerConnectionTries++;
    }


    @Override
    public void handleTimeout(SelectionKey key) {
        if (peerHandler == null) {
          // Timeout when performing xmpp negotiation with the xmpp client
            notifyStreamError(XMPPErrors.CONNECTION_TIMEOUT);
            return;
        }
        // If peer handler is not null, then the timeout even was triggered when negotiating with the xmpp server

        // Will send a connection refused error to the XMPP client to which this handler is connected.
        notifyStreamError(XMPPErrors.CONNECTION_REFUSED);
        // The peer handler is an XMPPClientHandler connected to an XMPP server.
        // Will send a timeout error to the XMPP server to which the peer handler is connected.
        peerHandler.notifyStreamError(XMPPErrors.CONNECTION_TIMEOUT);
    }

    @Override
    public boolean handleError(SelectionKey key) {
        // TODO: what should we do in case of an error?
        return true;
    }
}