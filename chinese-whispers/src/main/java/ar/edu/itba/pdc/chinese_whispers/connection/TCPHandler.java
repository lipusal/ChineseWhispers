package ar.edu.itba.pdc.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPHandler {


    /**
     * Handles error situations.
     *
     * @param key The {@link SelectionKey} that fell into error.
     * @return {@code true} if the key must be closed, or {@code false} otherwise.
     */
    boolean handleError(SelectionKey key);

    /**
     * Handles close event.
     *
     * @param key The {@link SelectionKey} whose channel will be closed.
     * @return {@code true} if the connection was successfully closed, or {@code false} otherwise.
     */
    boolean handleClose(SelectionKey key);

}