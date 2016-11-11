package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;


import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NewConnectionsConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation.XMPPServerNegotiator;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;

import java.nio.channels.SelectionKey;
import java.util.Base64;
import java.util.Map;

/**
 * This class makes the proxy act like an XMPP server, negotiating with the connected client.
 * The process of negotiation is done by an {@link XMPPServerNegotiator}. When this process finishes,
 * a connection with the origin server is tried. Upon success, an {@link XMPPClientHandler} is created,
 * in order to connect to the server, and negotiate with it.
 * Once the negotiation with the origin server ended, control is given to a new {@link XMPPReadWriteHandler},
 * which will be in charge of reading and writing to the connected client.
 * <p>
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerHandler extends NegotiatorHandler implements TCPHandler {

    /**
     * Says how many times the peer connection can be tried.
     */
    private static int MAX_PEER_CONNECTIONS_TRIES = 3; // TODO: define number ASAP


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
        this.newConnectionsConsumer = newConnectionsConsumer;
        this.peerConnectionTries = 0;
        this.key = key;
        xmppNegotiator = new XMPPServerNegotiator(this);
        this.stringBuilder = new StringBuilder();
    }

    @Override
    /* package */ void setKey(SelectionKey key) {
        throw new UnsupportedOperationException("Key can't be changed to an XMPPServerHandler");
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

        // TODO: check what handlers need to operate correctly


        // Create a read-write handler that will receive and send messages to the client connected to the proxy.
        XMPPReadWriteHandler xmppReadWriteHandler = new XMPPReadWriteHandler(applicationProcessor, metricsProvider,
                configurationsConsumer, clientJid, this.key, newPeerHandler);
        newPeerHandler.setPeerHandler(xmppReadWriteHandler);
        this.key.attach(xmppReadWriteHandler);

        String response = "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>\n"; //TODO retry?
        System.out.println(response); //TODO delete souts
        xmppReadWriteHandler.consumeMessage(response.getBytes());
        this.key.interestOps(this.key.interestOps() | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
        this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_READ);

        // Check params
        if (!xmppNegotiator.getInitialParameters().containsKey("to")) { //TODO send error
            closeHandler();
            return;
        }

        String authDecoded; // TODO: beware of char encoding
        try {
            // The authorization content might be invalid (i.e. not be a valid base64 scheme)
            authDecoded = new String(Base64.getDecoder().decode(xmppNegotiator.getAuthorization()));
        } catch (IllegalArgumentException e) {
            closeHandler(); //TODO send error
            return;
        }
        String[] authParameters = authDecoded.split("\0");
        String userName;
        if (authParameters.length == 3) {   //Nothing, user, pass
            userName=authParameters[1];
        }else if(authParameters.length == 2){
            userName=authParameters[0];
        }else{
            closeHandler(); //TODO send error
            return;
        }


        // Create a client handler to connect to origin server
        clientJid = userName + "@" + xmppNegotiator.getInitialParameters().get("to"); // TODO: check if readWrite handler needs it.
        this.peerHandler = new XMPPClientHandler(applicationProcessor, metricsProvider, configurationsConsumer, this,
                clientJid, xmppNegotiator.getInitialParameters(), xmppNegotiator.getAuthorization());


        // TODO: check what handler need to operate correctly

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
            // TODO: Close connection?
            closeHandler();
            return;
        }
        System.out.print("Trying to connect to origin server...");
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
    public boolean handleError(SelectionKey key) {
        // TODO: what should we do in case of an error?
        return true;
    }
}