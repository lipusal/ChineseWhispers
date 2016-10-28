package ar.edu.itba.chinese_whispers.xmpp;

import ar.edu.itba.chinese_whispers.connection.BasicIOOperations;
import ar.edu.itba.chinese_whispers.connection.TCPServerHandler;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.util.*;

/**
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerHandler implements TCPServerHandler {


    private int connectionIds = 0;

    private final Map<Integer, SelectionKey> connections;
    private final Map<SelectionKey, Integer> reverseConnections;

    private final Map<Integer, Deque<byte[]>> readMessages;

    private final Map<Integer, Deque<byte[]>> writeMessages;

    public XMPPServerHandler() {
        connections = new HashMap<>();
        reverseConnections = new HashMap<>();
        readMessages = new HashMap<>();
        writeMessages = new HashMap<>();
    }


    public void printConnections() {
        for (Integer each : connections.keySet()) {
            SelectionKey key = connections.get(each);
            if (key.isValid()) {
                InetAddress addr = ((SocketChannel) key.channel()).socket().getInetAddress();
                System.out.println("Connection with " + addr.getHostName() + " (" + addr.getHostAddress() + ")"
                        + " has id: " + each);

            }
        }
    }


    /**
     * Returns the next message in the read message queue for the given {@code connectionIds}.
     *
     * @param connectionIds The connection id.
     * @return The next read message for the given {@code connectionId}, or {@code null} if there is no message.
     */
    public byte[] getReadMessage(int connectionIds) {

        Deque<byte[]> deque = readMessages.get(connectionIds);
        if (deque == null) {
            deque = new LinkedList<>();
            readMessages.put(connectionIds, deque);
        }
        return deque.poll();
    }

    /**
     * Adds a message to be written in the channel corresponding to the given {@code connectionIds}.
     *
     * @param connectionIds The connection id.
     * @param message       The message to be written.
     */
    public void addWriteMessage(int connectionIds, byte[] message) {
        Deque<byte[]> deque = writeMessages.get(connectionIds);
        if (deque == null) {
            deque = new LinkedList<>();
            writeMessages.put(connectionIds, deque);
        }
        SelectionKey key = connections.get(connectionIds);
        // Just in case...
        if (key != null) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            deque.offer(message);
        }
    }

    public Set<Integer> getConnections() {
        return connections.keySet();
    }


//    public void performOperations() {
//        for (Integer each : readMessages.keySet()) {
//            Deque<byte[]> deque = readMessages.get(each);
//            if (deque != null) {
//                byte[] message = deque.poll();
//                if (message != null) {
//                    for (int i = 0; i < message.length; i++) {
//                        if (message[i] == 'A') {
//                            message[i] = '4';
//                        } else if (message[i] == 'E') {
//                            message[i] = '3';
//                        } else if (message[i] == 'I') {
//                            message[i] = '1';
//                        } else if (message[i] == '0') {
//                            message[i] = '0';
//                        } else if (message[i] == 'C') {
//                            message[i] = '<';
//                        }
//                    }
//                    deque = writeMessages.get(each);
//                    if (deque == null) {
//                        deque = new LinkedList<>();
//                        writeMessages.put(each, deque);
//                    }
//                    deque.offer(message);
//                    SelectionKey key = connections.get(each);
//                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
//                }
//            }
//        }
//    }


    @Override
    public void handleRead(SelectionKey key) {

        BasicIOOperations.beforeRead(key);
        byte[] message = BasicIOOperations.doRead(key);
        BasicIOOperations.afterRead(key);
        Integer connectionId = reverseConnections.get(key);
        if (connectionId == null) {
            throw new IllegalStateException();
        }
        Deque<byte[]> readDeque = readMessages.get(connectionId);
        if (readDeque == null) {
            readDeque = new LinkedList<>();
            readMessages.put(connectionId, readDeque);
        }
        readDeque.offer(message);
    }

    @Override
    public void handleWrite(SelectionKey key) {
        Integer connectionId = reverseConnections.get(key);
        if (connectionId == null) {
            throw new IllegalStateException();
        }
        Deque<byte[]> deque = writeMessages.get(connectionId);
        // Shouldn't happen this ...
        if (deque == null) {
            deque = new LinkedList<>();
            writeMessages.put(connectionId, deque);
        }
        byte[] message = deque.poll();
        if (message != null) {
            int wontBeWrittenBytes = BasicIOOperations.beforeWrite(key, message);

            // Message is too long to send it in one iteration ...
            if (wontBeWrittenBytes > 0) {
                byte[] actualMessage = new byte[message.length - wontBeWrittenBytes];
                System.arraycopy(message, 0, actualMessage, 0, actualMessage.length);
                byte[] wontBeSentMessage = new byte[wontBeWrittenBytes];
                System.arraycopy(message, actualMessage.length, wontBeSentMessage, 0, wontBeSentMessage.length);
                deque.push(wontBeSentMessage);
                message = actualMessage;
            }
            int writtenBytes = BasicIOOperations.doWrite(key, message);

            // Something happened so the message couldn't be send totally ...
            if (writtenBytes < message.length) {
                byte[] restOfMessage = new byte[message.length - writtenBytes];
                System.arraycopy(message, writtenBytes, restOfMessage, 0, restOfMessage.length);
                deque.push(restOfMessage);
            }
            BasicIOOperations.afterWrite(key);
            if (writeMessages.isEmpty()) {
                // Turns off the write bit if there are no more messages to write
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }

    }

    @Override
    public SelectionKey handleAccept(SelectionKey key) {

        BasicIOOperations.beforeAccept(key);
        SelectionKey newKey = BasicIOOperations.doAccept(key);
        BasicIOOperations.afterAccept(key);
        connections.put(connectionIds, newKey);
        reverseConnections.put(newKey, connectionIds);
        connectionIds++;
        printConnections();
        return newKey;
    }
}
