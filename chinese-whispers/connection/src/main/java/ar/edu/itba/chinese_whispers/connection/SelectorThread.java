package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * Created by jbellini on 24/10/16.
 */
public class SelectorThread implements Runnable {


    /* package */ final static int MAX_KEYS_PER_THREAD = 100;

    private final static int BUFFER_SIZE = 2048;


    /**
     * A set containing accepted keys.
     */
    private final Set<SelectionKey> acceptedKeys;
    /**
     * The connector that created this thread.
     */
    private final Connector connector;
    /**
     * The timeout of the select operation.
     */
    private final long timeout;


    /* package */ SelectorThread(Connector connector, long timeout) {
        this.acceptedKeys = new HashSet<>();
        this.connector = connector;
        this.timeout = timeout;
    }


    private void notifyConnectorAboutNewKeys() {
        Iterator<SelectionKey> it = acceptedKeys.iterator();
        if (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();
            synchronized (connector) {
                connector.registerConnection(key);
            }
        }
    }


    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        SelectionKey newKey;
        synchronized (key.selector()) {
            newKey = channel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
        }
        acceptedKeys.add(newKey);
    }

    private void handleRead(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buf = (ByteBuffer) key.attachment();

        long readBytes;
        byte[] message = new byte[0];
        do {
            int bufferInitialPosition = buf.position();
            readBytes = channel.read(buf);

            // if no bytes were read, do nothing
            if (readBytes > 0) {

                // Todo: check max array size
                if (readBytes + message.length > Integer.MAX_VALUE - 1) {
                    throw new RuntimeException(); // TODO: What do we do if maximum array size is exceeded?
                }

                // TODO: check out of memory exception
                byte[] aux = new byte[message.length + (int) readBytes];
                System.arraycopy(message, 0, aux, 0, message.length);
                System.arraycopy(buf.array(), bufferInitialPosition, aux, message.length, (int) readBytes);
                message = aux;
            }
        } while (readBytes > 0);

        if (readBytes == -1) {
            channel.close(); // Channel reached end of stream

        }

        // Channel actually received a message
        if (message.length > 0) {
            boolean added;
            do {
                // May return false if another thread handled the accept operation but it didn't notify connector yet.
                added = connector.offerReadMessage(key, message);
            }
            while (!added);
        }

    }


    private void handleWrite(SelectionKey key) throws IOException {


        ByteBuffer buf = (ByteBuffer) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        byte[] message = null;
        do {
            synchronized (connector) {
                message = connector.pollWriteMessage(key);
            }
            if (message != null) {

                // TODO: check what happens if data is bigger than buffer size

                buf.clear();
                buf.put(message);
                buf.flip();
                channel.write(buf);
                buf.compact();
            }
        }
        while (message != null);
    }


    @Override
    public void run() {

        Set<SelectionKey> selectedKeys = new HashSet<>(); // Created here to avoid allocating a HashSet each loop
//        Map<SelectionKey, Integer> interestOps = new HashMap<>();
        while (true) {

            // Perform other tasks
            notifyConnectorAboutNewKeys();


            try {
                // TODO: check synchronization
                if (connector.getSelector().select(timeout) == 0) {
                    continue;
                }


                synchronized (connector.getSelector()) {
                    // Clone collection in order to be able to continue adding keys in other thread
                    selectedKeys.addAll(connector.getSelector().selectedKeys());
                    connector.getSelector().selectedKeys().clear();

                    // Avoid the keys from being selected by other thread before being processed by this thread.
                    // This is done by setting the key's interest operations mask to 0,
                    // and setting it back after processing the key.
                    // Additionally, a key mask might be set to SelectionKey.OP_WRITE if a message must be sent to
//                    for (SelectionKey key : selectedKeys) {
//                        interestOps.put(key, key.interestOps());
//                        key.interestOps(0);
//                    }
                }

                Iterator<SelectionKey> it = selectedKeys.iterator();
//                DummyClass dummy = new DummyClass();
                while (it.hasNext()) {
                    // TODO: check synchronization of keys
                    SelectionKey key = it.next();
                    it.remove();
//                    dummy.setKey(key);


//                    synchronized (dummy.getKey()) {
                    synchronized (key) {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        }

                        if (key.isReadable()) {
                            // Perform reading...
                            handleRead(key);
                        }

                        if (key.isValid() && key.isWritable()) {
                            // Perform writing...
                            handleWrite(key);
                        }
                    }
//                    key.interestOps(interestOps.remove(key) | key.interestOps());


                }


            } catch (IOException e) {
                // TODO: what do we do here?
            }


        }
    }

    private static class DummyClass {

        private SelectionKey key;


        public SelectionKey getKey() {
            return key;
        }

        public void setKey(SelectionKey key) {
            this.key = key;
        }
    }
}
