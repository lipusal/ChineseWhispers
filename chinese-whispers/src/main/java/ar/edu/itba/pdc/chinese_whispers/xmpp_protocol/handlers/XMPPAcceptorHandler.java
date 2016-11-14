package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NewConnectionsConsumer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * This class makes the proxy accept new connections from XMPP clients.
 * When a new connection arrives, it will create a new {@link XMPPServerHandler}, and attach it to the
 * {@link SelectionKey} created when registering the accepted {@link SocketChannel}.
 * <p>
 * Created by jbellini on 29/10/16.
 */
public class XMPPAcceptorHandler extends BaseHandler implements TCPAcceptorHandler {


    /**
     * The new connections consumer to pass it to each new {@link XMPPServerHandler}
     */
    private final NewConnectionsConsumer newConnectionsConsumer;


    /**
     * Constructor.
     * <p/>
     * Note: All objects passed in this constructor will be used when creating new handlers.
     *
     * @param applicationProcessor   An object that can process XMPP messages bodies.
     * @param newConnectionsConsumer An object that can track new TCP connections.
     * @param configurationsConsumer An object that can be queried about which server each user must connect to.
     * @param metricsProvider        An object that manages the system metrics.
     */
    public XMPPAcceptorHandler(ApplicationProcessor applicationProcessor,
                               NewConnectionsConsumer newConnectionsConsumer,
                               ConfigurationsConsumer configurationsConsumer,
                               MetricsProvider metricsProvider) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        this.newConnectionsConsumer = newConnectionsConsumer;
    }


    @Override
    public boolean handleError(SelectionKey key) {
        // TODO: what should we do in case of an error?
        return true;
    }

    @Override
    public boolean handleClose(SelectionKey key) {
        // TODO: What should we do when closing the acceptor? Close all connection? Let them finish when they want?
        return true;
    }


    @Override
    public SelectionKey handleAccept(SelectionKey key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        try {
            SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
            if (channel == null) {
                return null;
            }
            channel.configureBlocking(false);

            // The net key will be listening till the client connected to its channel sends a message
            SelectionKey newKey = channel.register(key.selector(), SelectionKey.OP_READ);
            // The new handler will act as an XMPP server till negotiation with client finishes.
            XMPPServerHandler handler = new XMPPServerHandler(applicationProcessor,
                    newConnectionsConsumer,
                    configurationsConsumer,
                    metricsProvider,
                    newKey);
            newKey.attach(handler);

            return newKey;
            // TODO: Add this new key into some set in some future class to have tracking of connections
        } catch (IOException ignored) {
            // TODO: what should we do here? XMPP connection wasn't established yet, so it's not necessary to close nicely the connection?
        }
        return null;
    }
}
