package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ClientNegotiationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;

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
 * <p>
 * <p>
 * Created by jbellini on 28/10/16.
 */
public class XMPPClientHandler extends XMPPNegotiatorHandler implements TCPClientHandler {


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
     * @param authentication              The authentication string (i.e. text between the auth tags).
     */
    /* package */ XMPPClientHandler(ApplicationProcessor applicationProcessor,
                                    MetricsProvider metricsProvider,
                                    ConfigurationsConsumer configurationsConsumer,
                                    XMPPServerHandler xmppServerHandler,
                                    String clientJid,
                                    Map<String, String> negotiatorInitialParameters,
                                    String authentication) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        setNegotiationProcessor(new ClientNegotiationProcessor(this, authentication, negotiatorInitialParameters));
        if (xmppServerHandler == null) {
            throw new IllegalArgumentException();
        }
        this.peerHandler = xmppServerHandler;
        this.clientJid = clientJid;
        connected = false;
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
     * Sets the given interest ops to this handler's key, if it is connected.
     * If its not connected, nothing is done.
     *
     * @param interestOps The new interest ops.
     * @return This handler.
     */
    @Override
    protected XMPPHandler setKeyInterestOps(int interestOps) {
        if (!connected) {
            return this;
        }
        return super.setKeyInterestOps(interestOps);
    }


    @Override
    protected void beforeRead() {
        inputBuffer.clear(); // Clears the buffer in order to read at most its capacity.
    }

    @Override
    protected void afterWrite() {
        // Nothing to be done...
    }


    @Override
    protected void handleResponse(ParserResponse parserResponse) {
        super.handleResponse(parserResponse);
        switch (parserResponse) {
            case XML_ERROR:
                // super class method just calls the own notify error method (which does not notify the peer handler)
                peerHandler.notifyError(XMPPErrors.INTERNAL_SERVER_ERROR); // Not our proxy, but the origin server
                break;
            case HOST_UNKNOWN:
                peerHandler.notifyError(XMPPErrors.HOST_UNKNOWN_FROM_SERVER); // Make peer handler send error
                notifyClose(); // Close this handler
                break;
            case UNSUPPORTED_NEGOTIATION_MECHANISM:
                // Tells the client that connection couldn't be established
                peerHandler.notifyError(XMPPErrors.UNSUPPORTED_NEGOTIATION_MECHANISM_FOR_CLIENT);
                // Tells the server that the authentication process is aborted
                notifyError(XMPPErrors.UNSUPPORTED_NEGOTIATION_MECHANISM);
                break;
            case FAILED_NEGOTIATION:
                peerHandler.notifyError(XMPPErrors.FAILED_NEGOTIATION);
                notifyError(XMPPErrors.FAILED_NEGOTIATION_FOR_SERVER);
                break;
        }
    }

    /**
     * Starts the XMPP negotiation process by creating the first negotiation message to be sent.
     */
    private void startXMPPNegotiation() {
        ((ClientNegotiationProcessor) getNegotiationProcessor()).sendInitialMessage();
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
        enableReading();
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
                logger.info("Connection refused");  //TODO which connection?
                ((XMPPServerHandler) peerHandler).connectClientHandler(); // Ask peer handler to retry connection
            }
        }
        if (this.connected) {
            logger.info("Connection established! Now listening for messages");  //TODO between who and who?
            this.key.interestOps(0); // Turn off all keys
            enableReading();
            startXMPPNegotiation();
        } else {
            logger.warn("Connection failed! Not Connected!"); // TODO: Which connection?
        }


        // TODO: Add this key when connected into some set in some future class to have tracking of connections
    }


    @Override
    public void handleTimeout(SelectionKey key) {
        // An XMPPClientHandler always have a peer handler

        // The peer handler is an XMPPServerHandler waiting for this handler to connect to an XMPP server
        // In case this handler reached a timeout event, the XMPPServerHandler will send to the XMPP client
        // connected to it that the connection was refused.
        peerHandler.notifyError(XMPPErrors.CONNECTION_REFUSED);
        // Will send the timeout error to the server to which this XMPPClientHandler is connected to.
        notifyError(XMPPErrors.CONNECTION_TIMEOUT);
    }

    @Override
    public boolean handleError(SelectionKey key) {
        return false;
    }
}
