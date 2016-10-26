package ar.edu.itba.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 26/10/16.
 */
/* package */ interface NewMessagesConsumer {

    /**
     * Tells the implementors that the given key received a new message.
     *
     * @param key The key that corresponds to the new connection.
     */
    void tellNewMessage(SelectionKey key);

    // TODO: define method that receives a set of SelectionKeys?
}
