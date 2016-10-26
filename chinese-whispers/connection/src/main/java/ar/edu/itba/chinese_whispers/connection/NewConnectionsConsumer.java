package ar.edu.itba.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 26/10/16.
 */
/* package */ interface NewConnectionsConsumer {

    /**
     * Tells the implementors that a new connection was accepted, and that it was registered with the given {@code key}.
     *
     * @param key The key that corresponds to the new connection.
     */
    void tellNewConnection(SelectionKey key);

    // TODO: define method that receives a set of SelectionKeys?
}
