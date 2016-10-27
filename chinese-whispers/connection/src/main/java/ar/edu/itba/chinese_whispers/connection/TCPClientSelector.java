package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by jbellini on 27/10/16.
 */
public class TCPClientSelector extends TCPSelector{



    public TCPClientSelector(TCPClientHandler handler) throws IOException {
        super(handler);
    }


    public boolean addClientChannel(String host, int port) {
        SocketChannel channel;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(host, port));
            channel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    protected TCPClientHandler getTCPHandler() {
        return (TCPClientHandler) super.getTCPHandler();
    }


    @Override
    protected void TCPSelectorSpecificOperation(SelectionKey key) {
        if (key.isConnectable()) {
            getTCPHandler().handleConnect(key);
        }
    }
}
