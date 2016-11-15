package ar.edu.itba.pdc.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 14/11/16.
 */
public interface TCPTimeoutCancellableHandler extends TCPHandler {


    /**
     * Handles the timeout event.
     *
     * @param key The {@link SelectionKey} that represents the connection to be
     */
    void handleTimeout(SelectionKey key);

}
