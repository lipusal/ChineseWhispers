package ar.edu.itba.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPClientHandler extends TCPHandler {

    /**
     * Handles the connect operation.
     *
     * @param key The {@link SelectionKey} that contains the socket channel to be connected.
     * @return {@code true} if the connection was stablished, or {@code false} otherwise.
     */
    boolean handleConnect(SelectionKey key);
}
