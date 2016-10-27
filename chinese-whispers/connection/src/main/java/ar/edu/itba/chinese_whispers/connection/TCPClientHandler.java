package ar.edu.itba.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPClientHandler extends TCPHandler {

    void handleConnect(SelectionKey key);
}
