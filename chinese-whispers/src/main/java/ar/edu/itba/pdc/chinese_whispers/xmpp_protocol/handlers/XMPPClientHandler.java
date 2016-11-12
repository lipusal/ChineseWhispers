package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation.XMPPClientNegotiator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * This class makes the proxy act like an XMPP client, connecting to, and negotiating with, the origin server.
 * This handler assumes that it will have a {@link SelectionKey} set when a TCP connection is requested to
 * the connection layer. When that key is selected ad connectable, this class handleConnect method
 * will try to finish the connection. Upon success, it will start the negotiation process with the origin server.
 * When that process finishes, it will create a new {@link XMPPReadWriteHandler},
 * and notify the {@link XMPPServerHandler} that created this handler that the negotiation process has finished,
 * and that the new handler that is handling the connection with the origin server is the recently created one.
 *
 * <p>
 * Created by jbellini on 28/10/16.
 */
public class XMPPClientHandler extends NegotiatorHandler implements TCPClientHandler {


    private static final String PARTIAL_INITIAL_MESSAGE = "<stream:stream " +
            "xmlns:stream=\'http://etherx.jabber.org/streams\' " +
            "xmlns=\'jabber:client\' " +
            "xmlns:xml=\'http://www.w3.org/XML/1998/namespace\'";


    /**
     * A string builder to create strings efficiently
     */
    private static final StringBuilder stringBuilder = new StringBuilder();


    /**
     * Says if this handler has established a TCP connection with the origin server
     */
    private boolean connected;


    /**
     * Constructor.
     * It MUST only be called by {@link XMPPServerHandler}, when client-proxy negotiation ended.
     *
     * @param applicationProcessor        An object that can process XMPP messages bodies.
     * @param configurationsConsumer      An object that can be queried about which server each user must connect to.
     * @param metricsProvider             An object that manages the system metrics.
     * @param xmppServerHandler           The {@link XMPPServerHandler} that is creating this
     *                                    new {@link XMPPClientHandler}
     * @param clientJid                   The User's JID (The user that connected to the proxy).
     * @param negotiatorInitialParameters The negotiation initial parameters that were read by the
     *                                    {@link XMPPServerHandler} that is creating this new {@link XMPPClientHandler}
     * @param authorization               The authorization. // TODO: specify a bit more
     */
    /* package */ XMPPClientHandler(ApplicationProcessor applicationProcessor,
                                    MetricsProvider metricsProvider,
                                    ConfigurationsConsumer configurationsConsumer,
                                    XMPPServerHandler xmppServerHandler,
                                    String clientJid,
                                    Map<String, String> negotiatorInitialParameters,
                                    String authorization) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        if (xmppServerHandler == null) {
            throw new IllegalArgumentException();
        }
        this.peerHandler = xmppServerHandler;
        this.clientJid = clientJid;
        connected = false;
        xmppNegotiator = new XMPPClientNegotiator(this, authorization, negotiatorInitialParameters);
    }

    /**
     * Says if this handler has established a TCP connection.
     *
     * @return {@code true} if a TCP connection with the origin server was established, or {@code false} otherwise.
     */
    /* package */ boolean isConnected() {
        return connected;
    }


    /**
     * Starts the XMPP negotiation process by creating the first negotiation message to be sent.
     */
    // TODO: shouldn't this belong on the client negotiator?
    private void startXMPPNegotiation() {
        // TODO: check that this does not have problems with char encoding
        stringBuilder.setLength(0); // Clears the string builder
        stringBuilder.append(PARTIAL_INITIAL_MESSAGE);

        // Adds to the initial message all parameters sent by the client connected to the proxy
        for (String attributeKey : xmppNegotiator.getInitialParameters().keySet()) {
            stringBuilder.append(" ")
                    .append(attributeKey)
                    .append("=\'")
                    .append(xmppNegotiator.getInitialParameters().get(attributeKey))
                    .append("\'");
        }

        stringBuilder.append(">\n");
        System.out.println("Proxy to Server:" + stringBuilder); // TODO: log this.
        writeMessage(stringBuilder.toString().getBytes());
    }


    /**
     * Finishes the XMPP negotiation process.
     * When executing the method, first a new {@link XMPPReadWriteHandler} is created.
     * Then the {@link XMPPServerHandler} is request to start proxying,
     * by calling the {@link XMPPServerHandler#startProxying(XMPPReadWriteHandler)} method.
     * After that, the new {@link XMPPReadWriteHandler} is attached to this handler's key.
     * Finally, the key is marked as readable (in case it was marked as non-readable before)
     */
    @Override
    protected void finishXMPPNegotiation() {
        XMPPReadWriteHandler xmppReadWriteHandler = new XMPPReadWriteHandler(applicationProcessor, metricsProvider,
                configurationsConsumer, clientJid, this.key);
        ((XMPPServerHandler) peerHandler).startProxying(xmppReadWriteHandler);
        this.key.attach(xmppReadWriteHandler);
        this.key.interestOps(peerHandler.key.interestOps() | SelectionKey.OP_READ); // TODO: check if it's better to check the peer handler's write buffer before.
    }


    @Override
    public void handleConnect(SelectionKey key) {
        if (key != this.key) {
            throw new IllegalArgumentException();
        }
        SocketChannel channel = (SocketChannel) key.channel();
        this.connected = channel.isConnected();
        if (!this.connected) {
            try {
                if (channel.isConnectionPending()) {
                    this.connected = channel.finishConnect();
                } else {
                    InetSocketAddress remote = (InetSocketAddress) channel.getRemoteAddress();
                    if (remote == null) {
                        throw new IllegalStateException("Remote address wasn't specified."); // TODO: check this
                    }
                    this.connected = channel.connect(remote);

                }
            } catch (IOException e) {
                System.out.println("Connection refused");
                ((XMPPServerHandler) peerHandler).connectClientHandler(); // Ask peer handler to retry connection
            }
        }
        if (this.connected) {
            System.out.println("Connect established! Now listening messages"); // TODO: log this?
            this.key.interestOps(key.interestOps() | SelectionKey.OP_READ); // Makes the key readable TODO: check this!!
            startXMPPNegotiation();
        } else {
            System.out.println("Connection failed! Not Connected!"); // TODO: log this?
        }


        // TODO: Add this key when connected into some set in some future class to have tracking of connections
    }

    @Override
    void beforeClose() {
        ((XMPPServerHandler)peerHandler).handleFailure();
    }


    @Override
    public boolean handleError(SelectionKey key) {
        return false;
    }
}
