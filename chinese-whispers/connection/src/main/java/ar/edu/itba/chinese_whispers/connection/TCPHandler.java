package ar.edu.itba.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPHandler {


    void handleRead(SelectionKey key);

    void handleWrite(SelectionKey key);
}
