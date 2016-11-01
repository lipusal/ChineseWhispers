package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;


import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NewConnectionsConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ProxyConfigurationProvider;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation.XMPPServerNegotiator;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.XMLInterpreter;

import java.nio.channels.SelectionKey;
import java.util.Base64;
import java.util.Map;

/**
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerHandler extends XMPPHandler implements TCPHandler {

    /**
     * Says how many times the peer connection can be tried.
     */
    private static int MAX_PEER_CONNECTIONS_TRIES = 3; // TODO: define number ASAP


    /**
     * A proxy connection configurator to get server and port to which a user should establish a connection.
     */
    private final ProxyConfigurationProvider proxyConfigurationProvider;
    /**
     * The new connections consumer that will be notified when new connections arrive.
     */
    private final NewConnectionsConsumer newConnectionsConsumer;

    /**
     * Holds how many peer connection tries have been done.
     */
    private int peerConnectionTries;


    /**
     * Constructor.
     * This constructor will create it's {@link XMPPClientHandler} peer.
     *
     * @param applicationProcessor       The application processor.
     * @param newConnectionsConsumer     The object to be notified when new XMPP connections are established.
     * @param proxyConfigurationProvider The object to be queried for proxy configurations.
     */
    /* package */ XMPPServerHandler(ApplicationProcessor applicationProcessor,
                                    NewConnectionsConsumer newConnectionsConsumer,
                                    ProxyConfigurationProvider proxyConfigurationProvider) {
        super(applicationProcessor);
        this.newConnectionsConsumer = newConnectionsConsumer;
        this.peerHandler = new XMPPClientHandler(applicationProcessor, this);
        this.xmlInterpreter = new XMLInterpreter(applicationProcessor, peerHandler);
        this.proxyConfigurationProvider = proxyConfigurationProvider;
        this.peerConnectionTries = 0;
        xmppNegotiator = new XMPPServerNegotiator(this);
    }


    @Override
    protected void sendProcessedStanza(byte[] message) {
        xmlInterpreter.setSilenced(proxyConfigurationProvider.isUserSilenced(clientJid));
        super.sendProcessedStanza(message);
    }

    @Override
    public void handleWrite(SelectionKey key) {
        System.out.println("ServerHandler(proxy-client)");
        super.handleWrite(key);
    }


    @Override
    protected void processReadMessage(byte[] message) {
        if (message != null && message.length > 0) {

            if (connectionState == ConnectionState.XMPP_STANZA_STREAM) {
                sendProcessedStanza(message);
            } else if (connectionState == ConnectionState.XMPP_NEGOTIATION) {

                ParserResponse parserResponse = xmppNegotiator.feed(message);
                handleResponse(parserResponse);
                if (parserResponse == ParserResponse.NEGOTIATION_END) {

                    //Stop reading until negotiation finish on other sides.
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);

                    //Initialize obtained data
                    connectionState = ConnectionState.XMPP_STANZA_STREAM;
                    Map<String, String> initialNegotiationParameters = xmppNegotiator.getInitialParameters();

                    // TODO: These are accessing peer handler's private fields...

                    peerHandler.xmppNegotiator.setAuthorization(xmppNegotiator.getAuthorization());
                    peerHandler.xmppNegotiator.setInitialParameters(initialNegotiationParameters);

                    if (!initialNegotiationParameters.containsKey("to")) {
                        throw new IllegalStateException();
                        //TODO send error? Send error before this?
                    } else {
                        String authDecoded = new String(Base64.getDecoder().decode(xmppNegotiator.getAuthorization()));
                        String[] authParameters = authDecoded.split("\0");
                        if (authParameters.length != 3) { //Nothing, user, pass
                            throw new IllegalStateException();
                            //TODO send error? Send error before this?
                        }
                        clientJid = authParameters[1] + "@" + initialNegotiationParameters.get("to");
                        //Uncomment this to silence arriving msg. What should be done of clientJID of server?
                        //otherEndHandler.clientJID=clientJID;
                    }

                    //Connect Peer
                    connectPeer(); // TODO: Check that only once code reaches here...

                    //Generates first message
                    StringBuilder startStream = new StringBuilder();
                    startStream.append("<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" ")
                            .append("xmlns=\"jabber:client\" ")
                            .append("xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" ");
                    for (String attributeKey : xmppNegotiator.getInitialParameters().keySet()) {
                        startStream.append(attributeKey)
                                .append("=\"")
                                .append(xmppNegotiator.getInitialParameters().get(attributeKey))
                                .append("\" ");
                    }
                    startStream.append("> ");
                    System.out.println("Proxy to Server:" + startStream);


                    // TODO: Check this! the peer's key setting was commented.
                    // Sends first message to otherEndHandler for him to send. Note that this sets peer's key.
                    peerHandler.consumeNegotiationMessage(startStream.toString().getBytes());

                }
            }

        }
    }


    /**
     * Method to be executed to tunnel the client being connected to this server handler into the origin server.
     */
    /* package */ void connectPeer() {
        if (clientJid == null) {
            throw new IllegalStateException();
        }
        // TODO: Should we avoid trying to connect again for some seconds? We can mark the clientHandler with a timestamp and retry after some time has passed.
        // TODO: We can save the key in a Map and update those timestamps before the select.
        if (peerConnectionTries >= MAX_PEER_CONNECTIONS_TRIES) {
            // TODO: Close connection?
            closeHandler();
            return;
        }
        System.out.print("Trying to connect to origin server...");
        SelectionKey peerKey = TCPSelector.getInstance().
                addClientSocketChannel(proxyConfigurationProvider.getServer(clientJid),
                        proxyConfigurationProvider.getServerPort(clientJid),
                        (XMPPClientHandler) peerHandler);
        if (peerKey == null) {
            // Connection failed ...
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