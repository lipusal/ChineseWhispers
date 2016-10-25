package ar.edu.itba.chinese_whispers.connection;

import com.sun.corba.se.spi.activation.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jbellini on 25/10/16.
 */
public class TCPConnectionsHandler {

    private final SelectorThread selectorThread;

    private final Map<Integer, SelectionKey> acceptedConnections;

    private final Map<SelectionKey, Integer> reverseAcceptedConnections;

    private final Thread controlThread;


    private static int serverSocketIdentifier = 0;

    private final Map<Integer, ServerSocketChannel> clientSockets;

    private static int clientSocketIdentifier = 0;





    private final Map<Integer, SelectionKey> serverSockets;




    public TCPConnectionsHandler() throws IOException {
        this.selectorThread = new SelectorThread();
        this.acceptedConnections = new HashMap<>();
        this.reverseAcceptedConnections = new HashMap<>();
        this.controlThread = new Thread(new ControlThread());
        this.serverSockets = new HashMap<>();
        this.clientSockets = new HashMap<>();
    }


    public int addServerSocket(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        int result;
        synchronized (this) {
            result = serverSocketIdentifier++;
            // Will block until the selectorThread registers the new channel
            serverSockets.put(result, selectorThread.registerChannel(channel, SelectionKey.OP_ACCEPT));
        }
        return result;
    }

    public void addClientSocket(InetSocketAddress server, int port) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    /**
     * This class represents a thread that will be in charge of ...
     */
    private class ControlThread implements Runnable {

        @Override
        public void run() {

        }
    }

}
