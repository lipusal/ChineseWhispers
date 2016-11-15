package ar.edu.itba.pdc.chinese_whispers.administration_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.AuthenticationProvider;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPAcceptorHandler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class AdminAcceptorHandler implements TCPAcceptorHandler {


    /**
     * {@link MetricsProvider} to be passed to the new created {@link AdminServerHandler}.
     */
    private final MetricsProvider metricsProvider;

    /**
     * {@link ConfigurationsConsumer} to be passed to the new created {@link AdminServerHandler}
     */
    private final ConfigurationsConsumer configurationsConsumer;

    /**
     * {@link AuthenticationProvider} to be passed to the new created {@link AdminServerHandler}
     */
    private final AuthenticationProvider authenticationProvider;


    public AdminAcceptorHandler(MetricsProvider metricsProvider,
                                ConfigurationsConsumer configurationsConsumer,
                                AuthenticationProvider authenticationProvider) {
        this.metricsProvider = metricsProvider;
        this.configurationsConsumer = configurationsConsumer;
        this.authenticationProvider = authenticationProvider;
    }


    @Override
    public boolean handleError(SelectionKey key) {
        // TODO:  Close all connections.
        return true;
    }

    @Override
    public boolean handleClose(SelectionKey key) {
        // TODO: Close all connections.
        return true;
    }

    @Override
    public SelectionKey handleAccept(SelectionKey key) {
        try {
            SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
            channel.configureBlocking(false);
            AdminServerHandler handler = new AdminServerHandler(metricsProvider,
                    configurationsConsumer, authenticationProvider);
            // The handler assigned to accepted sockets won't accept new connections, it will read and write
            // (it's writable upon creation because it might be created with data in its write messages queue)
            SelectionKey newKey = channel.register(key.selector(),
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE, handler);
            return newKey;
        } catch (IOException ignored) {
        }
        return null;
    }
}
