package ar.edu.itba.pdc.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 14/11/16.
 */
public interface TCPReadWriteHandler extends TCPTimeoutCancellableHandler {


    /**
     * Handles the read operation.
     *
     * @param key The {@link SelectionKey} that contains the channel to be read.
     */
    void handleRead(SelectionKey key);

    /**
     * Handles the write operation.
     *
     * @param key The {@link SelectionKey} that contains the channel to be written.
     */
    void handleWrite(SelectionKey key);


}
