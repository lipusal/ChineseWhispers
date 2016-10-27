package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

/**
 * Created by jbellini on 27/10/16.
 */
public class TCPServerSelector extends TCPSelector {


    public TCPServerSelector(TCPServerHandler handler) throws IOException {
        super(handler);
    }


    public boolean addServerChannel(int port) {
        ServerSocketChannel channel;
        try {
            channel = ServerSocketChannel.open();
            channel.socket().bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    protected TCPServerHandler getTCPHandler() {
        return (TCPServerHandler) super.getTCPHandler();
    }


    @Override
    protected void TCPSelectorSpecificOperation(SelectionKey key) {
        if (key.isAcceptable()) {
            getTCPHandler().handleAccept(key);
        }
    }
}
