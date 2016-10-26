package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;

/**
 * Created by jbellini on 26/10/16.
 */
public class TCPHandler implements NewConnectionsConsumer, NewMessagesConsumer {




    private final IOOperator operator;




    public TCPHandler() throws IOException {
        this.operator = new IOOperator(this, this);
    }


    public void addServerSocket(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();   // Creates the new server socket
        channel.socket().bind(new InetSocketAddress(port));         // Binds the new server socket
        operator.registerChannel(channel, SelectionKey.OP_ACCEPT);
    }


    public void startListening() throws IOException {
        while (true) {
            operator.makeIOOperations();
        }
    }


    @Override
    public void tellNewMessage(SelectionKey key) {

        // This is for testing just for now ...
        String message = new String(operator.pollReadMessage(key), Charset.forName("UTF-8"));
        System.out.println(message);
        System.out.println("El largo del mensaje es: " + message.length());
        operator.offerWriteMessage(key, message.trim().concat(" --> Tiene un largo de " + message.length() + "\n")
                .getBytes());
        // What it must really do is to store the mapped connectionId corresponding to the key as a connection with unread messages

    }

    @Override
    public void tellNewConnection(SelectionKey key) {
        // This is for testing just for now ...
        System.out.println("A new connection has been established...");
        // What it must really do is to store the key and make a mapping with a connectionId
    }
}
