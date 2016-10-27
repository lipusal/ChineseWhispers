package ar.edu.itba.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPServerHandler extends TCPHandler {

    void handleAccept(SelectionKey key);
}
