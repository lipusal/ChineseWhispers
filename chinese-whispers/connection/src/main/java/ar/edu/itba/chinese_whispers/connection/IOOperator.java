package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * Created by jbellini on 26/10/16.
 * <p>
 * This class handles IO Operations using round robin on selected SelectionKeys
 */
/* package */ class IOOperator {


    /**
     * The byte buffers attached to new registered keys size.
     */
    private static final int BUFFER_SIZE = 256;


    /**
     * Object to be informed when new connections were accepted.
     */
    private final NewConnectionsConsumer newConnectionsConsumer;
    /**
     * Object to be informed when new messages were received.
     */
    private final NewMessagesConsumer newMessagesConsumer;


    /**
     * The selector that multiplexes I/O.
     */
    private final Selector selector;


    /**
     * Stores accepted connections keys that haven't been notified to the newConnectionsConsumer.
     */
    private final Set<SelectionKey> acceptedConnections;
    /**
     * Stores read messages in the SelectionKey's channel.
     */
    private final Map<SelectionKey, Deque<byte[]>> readMessagesQueues;
    /**
     * Stores messages to be written in the SelectionKey's channel.
     */
    private final Map<SelectionKey, Deque<byte[]>> writeMessagesQueues;


    public IOOperator(NewConnectionsConsumer newConnectionsConsumer, NewMessagesConsumer newMessagesConsumer)
            throws IOException {
        this.newConnectionsConsumer = newConnectionsConsumer;
        this.newMessagesConsumer = newMessagesConsumer;
        this.selector = Selector.open();
        this.acceptedConnections = new HashSet<>();
        this.readMessagesQueues = new HashMap<>();
        this.writeMessagesQueues = new HashMap<>();
    }


    /**
     * Registers a new channel in the selector without any attachment.
     * Note: Typically, this method should be used to register new bound server socket channels.
     *
     * @param channel     The channel to be registered.
     * @param interestOps The interest set of operations.
     * @return The key representing the registered channel.
     * @throws IOException If I/O error occurs.
     */
    public SelectionKey registerChannel(SelectableChannel channel, int interestOps) throws IOException {
        return registerChannel(channel, interestOps, null);
    }

    /**
     * Registers a new channel in the selector, attaching the given {@code attachment} in the resulting Selection Key.
     * Note: Typically, this method should be used to register accepted server socket channels.
     *
     * @param channel     The channel to be registered.
     * @param interestOps The interest set of operations.
     * @param attachment  The attachment to be attached in the resulting Selection Key.
     * @return The key representing the registered channel.
     * @throws IOException if I/O error occurs.
     */
    public SelectionKey registerChannel(SelectableChannel channel, int interestOps, Object attachment)
            throws IOException {
        channel.configureBlocking(false); // We only accept non-blocking channels
        return channel.register(selector, interestOps, attachment);
    }


    /**
     * Gets the next message in the read messages queue for the given {@code key}.
     * Note: This method synchronizes the readMessageQueues.
     *
     * @param key The key whose next read messages must be retrieved.
     * @return The read message, or {@code null} if no message was read.
     */
    /* package */ byte[] pollReadMessage(SelectionKey key) {
        synchronized (readMessagesQueues) {
            return pollMessage(readMessagesQueues, key);
        }
    }

    /**
     * Adds the given {@code message} to be written in the given {@code key}'s channel.
     * Note: This method synchronizes the writeMessageQueues.
     *
     * @param key     The key whose channel will be used to write the given message.
     * @param message The message to be written.
     */
    /* package */ void offerWriteMessage(SelectionKey key, byte[] message) {
        synchronized (writeMessagesQueues) {
            offerMessage(writeMessagesQueues, key, message);
        }
    }

    /**
     * Makes all IO Operations, checking for unwritten messages before performing IO operations,
     * and notifying new connections and new messages afterwards.
     *
     * @return {@code true} if IO operations were performed, or {@code false} otherwise.
     */
    public boolean makeIOOperations() throws IOException {
        checkWrites(); // Mark as writable those keys with unsent messages
        boolean operationsPerformed = doIOOperations();
        if (operationsPerformed) {
            notifyNewConnections();
            notifyNewMessages();
        }
        return operationsPerformed;
    }


    /**
     * Adds the given {@code message} into the corresponding queue for the given {@code key}.
     * Note: This method synchronizes the readMessageQueues.
     *
     * @param key     The key whose channel received the message.
     * @param message The message received.
     */
    private void offerReadMessage(SelectionKey key, byte[] message) {
        synchronized (readMessagesQueues) {
            offerMessage(readMessagesQueues, key, message);
        }
    }

    /**
     * Gets the next message in the write messages queue for the given {@code key}.
     * Note: This method synchronizes the writeMessageQueues.
     *
     * @param key The key whose next write message must be retrieved.
     * @return The write message, or {@code null} if no message was left to write.
     */
    private byte[] pollWriteMessage(SelectionKey key) {
        synchronized (writeMessagesQueues) {
            return pollMessage(writeMessagesQueues, key);
        }
    }

    /**
     * Adds the given {@code message} into the corresponding queue for the given {@code key},
     * at the beginning of the queue.
     * Note: This method synchronizes the writeMessageQueue
     *
     * @param key     The key whose channel will be used to write the given message.
     * @param message The message to be written.
     */
    private void pushWriteMessage(SelectionKey key, byte[] message) {
        synchronized (writeMessagesQueues) {
            pushMessage(writeMessagesQueues, key, message);
        }
    }

    /**
     * Retrieves the next message in the corresponding queue for the given {@code key}, in the specified
     * {@code map} of queues.
     * Note: This method does not synchronize any object.
     *
     * @param deques The map containing the queues.
     * @param key    The key whose queue must be retrived.
     * @return The next message in the queue, or {@code null} if there are no messages to poll.
     */
    private byte[] pollMessage(Map<SelectionKey, Deque<byte[]>> deques, SelectionKey key) {
        Deque<byte[]> deque = deques.get(key);
        return deque == null ? null : deque.poll();
    }

    /**
     * Adds the specified {@code message} in the corresponding queue for the given {@code key},
     * in the specified {@code map} of queues.
     * Note: This method does not synchronize any object.
     *
     * @param deques  The map containing the queues.
     * @param key     The key whose queue must be retrieved.
     * @param message The message to be offered.
     */
    private void offerMessage(Map<SelectionKey, Deque<byte[]>> deques, SelectionKey key, byte[] message) {
        Deque<byte[]> deque = deques.get(key);
        if (deque == null) {
            deque = new LinkedList<>();
            deques.put(key, deque);
        }
        deque.offer(message);
    }

    /**
     * Adds the specified {@code message} in the corresponding queue for the fiven {@code key},
     * in the specified {@code map} of queues, pushing it at the beginning of the queue.
     * Note: This method does not synchronize any object.
     *
     * @param deques  The map containing the queues.
     * @param key     The key whose queue must be retrieved.
     * @param message The message to be pushed.
     */
    private void pushMessage(Map<SelectionKey, Deque<byte[]>> deques, SelectionKey key, byte[] message) {
        Deque<byte[]> deque = deques.get(key);
        if (deque == null) {
            deque = new LinkedList<>();
            deques.put(key, deque);
        }
        deque.push(message);
    }

    /**
     * Returns {@code true} if the corresponding readMessagesQueue for the given {@code key} has messages.
     *
     * @param key The key whose readMessagesQueue must be queried for unread messages.
     * @return {@code true} if there is at least one message that was not read, or {@code false} otherwise.
     */
    private boolean hasUnreadMessages(SelectionKey key) {
        synchronized (readMessagesQueues) {
            return hasMessages(readMessagesQueues, key);
        }
    }

    /**
     * Returns {@code true} if the corresponding writeMessageQueue for the given {@code key} has messages.
     *
     * @param key The key whose writeMessageQueue must be queried for unwritten messages.
     * @return {@code true} if there is at least one message that was not written, or {@code false} otherwise.
     */
    private boolean hasUnwrittenMessages(SelectionKey key) {
        synchronized (writeMessagesQueues) {
            return hasMessages(writeMessagesQueues, key);
        }
    }


    /**
     * Returns {@code true} if there is a message in the corresponding queue for the given {@code key},
     * in the given map of {@code queues}.
     *
     * @param deques The map containing the queues.
     * @param key    The key whose queue must be queried.
     * @return {@code true} if there is at least one message in the queue, or {@code false} otherwise.
     */
    private boolean hasMessages(Map<SelectionKey, Deque<byte[]>> deques, SelectionKey key) {
        Deque<byte[]> deque = deques.get(key);
        return deque != null && deque.peek() != null;
    }


    /**
     * Tells the newConnectionsConsumer which keys represent new connections.
     * Note: This method synchronizes the accepted connections set and the new connections consumer, in that order.
     */
    private void notifyNewConnections() {
        synchronized (acceptedConnections) {
            synchronized (newConnectionsConsumer) {
                acceptedConnections.forEach(newConnectionsConsumer::tellNewConnection);
            }
            acceptedConnections.clear();
        }
    }

    /**
     * Tells the newMessagesConsumer which keys has new messages.
     * Note: This method synchronizes the read message queues and the new messages consumer, in that order.
     */
    private void notifyNewMessages() {
        synchronized (readMessagesQueues) {
            synchronized (newMessagesConsumer) {
                readMessagesQueues.keySet().stream().filter(this::hasUnreadMessages)
                        .forEach(newMessagesConsumer::tellNewMessage);
            }
        }
    }


    /**
     * Marks keys with unsent write messages as writable (i.e. setting their interestOps bit mask accordingly)
     */
    private void checkWrites() {
        synchronized (writeMessagesQueues) {
            writeMessagesQueues.keySet().stream().filter(this::hasUnwrittenMessages)
                    .forEach(key -> key.interestOps(key.interestOps() | SelectionKey.OP_WRITE));
        }
    }


    /**
     * Handles accept operation.
     * Note: This method synchronizes the accepted connections set.
     *
     * @param key The key to be attended.
     */
    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        SelectionKey newKey = registerChannel(channel, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
        synchronized (acceptedConnections) {
            acceptedConnections.add(newKey);
        }
    }

    /**
     * Handles read operation.
     * Note: This method synchronizes on the read message queues
     * as it calls {@link IOOperator#offerReadMessage(SelectionKey, byte[])}
     *
     * @param key The key to be attended.
     */
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buf = (ByteBuffer) key.attachment();

        buf.clear(); // Sets the buffer position to zero in order to fill it completely
        int readBytes = channel.read(buf);
        if (readBytes > 0) {
            // If a message was actually read ...
            byte[] message = new byte[readBytes];
            System.arraycopy(buf.array(), 0, message, 0, readBytes);
            offerReadMessage(key, message); // Offers the new message to the corresponding queue

            // TODO: if readBytes is smaller than BUFFER_SIZE, should we retry reading?
            // TODO: we can read again to see if a new message arrived till total amount of read bytes == BUFFER_SIZE

            // TODO: should we notify the newMessagesConsumer now or after the select loop?
        } else if (readBytes == -1) {
            channel.close(); // Channel reached end of stream
        }
    }


    /**
     * Handles write operation.
     * Note: This method synchronizes on the write message queues
     * as it call {@link IOOperator#pollWriteMessage(SelectionKey)}
     *
     * @param key The key to be attended.
     */
    private void handleWrite(SelectionKey key) throws IOException {
        ByteBuffer buf = (ByteBuffer) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        byte[] message = pollWriteMessage(key); // TODO: should we create a bigger message of BUFFER_SIZE length joining smaller messages?


        // Shouldn't happen as this handler is called only if the key has messages to be written
        if (message != null) {
            // If the message is bigger than the buffer size, we create a new message with the writable part,
            // and return the rest of the message to beginning of the queue.
            if (message.length > BUFFER_SIZE) {
                byte[] actualMessage = new byte[BUFFER_SIZE];
                System.arraycopy(message, 0, actualMessage, 0, actualMessage.length);
                byte[] restOfMessage = new byte[message.length - actualMessage.length];
                System.arraycopy(message, actualMessage.length, restOfMessage, 0, restOfMessage.length);
                pushWriteMessage(key, restOfMessage);
                message = actualMessage;
            }
            buf.clear();
            buf.put(message);
            buf.flip();
            // Message must be written totally as buffer may be overwritten in next select iteration.
            // TODO: Should we attach an object containing two buffers (one for input and the other for output) instead of one single buffer?
            do {
                channel.write(buf);
            } while (buf.hasRemaining()); // Continue writing if message wasn't totally written
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }


    /**
     * Performs all ready IO Operations
     *
     * @return {@code true} if IO operations were performed, or {@code false} otherwise.
     */
    private boolean doIOOperations() throws IOException {
        // No ready operations
        if (selector.selectNow() == 0) {
            return false;
        }
        for (SelectionKey key : selector.selectedKeys()) {

            if (key.isAcceptable()) {
                handleAccept(key);
            }
            if (key.isReadable()) {
                handleRead(key);
            }
            if (key.isValid() && key.isWritable()) {
                handleWrite(key);
            }

            // TODO: Check connection reset by peer...
        }
        selector.selectedKeys().clear(); // Removes all selected keys
        return true;
    }


}
