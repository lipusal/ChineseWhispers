package ar.edu.itba.pdc.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPAcceptorHandler extends TCPHandler {

    /**
     * Handles the accept operation.
     *
     * @param key The {@link SelectionKey} that contains the socket channel that has a pending accept.
     * @return The {@link SelectionKey} registering the accepted connection, or {@code null} if errors occurred.
     */
    SelectionKey handleAccept(SelectionKey key);
}