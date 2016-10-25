package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 * Created by jbellini on 25/10/16.
 */
public class SelectorThread implements Runnable {

    /**
     * The selector that will listen I/O events.
     */
    private final Selector selector;

    /**
     * Stores read messages in the SelectionKey's channel
     */
    private final Map<SelectionKey, Queue<byte[]>> readMessagesQueues;
    /**
     * Stores messages to be written in the SelectionKey's channel
     */
    private final Map<SelectionKey, Queue<byte[]>> writeMessagesQueues;


    public SelectorThread() throws IOException {
        this.selector = Selector.open();
        this.readMessagesQueues = new HashMap<>();
        this.writeMessagesQueues = new HashMap<>();
    }


    /**
     * Registers a new channel in the selector without any attachment.
     * Typically, this method should be used to register new bound server socket channels.
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
     *
     * @param channel     The channel to be registered.
     * @param interestOps The interest set of operations.
     * @param attachment  The attachment to be attached in the resulting Selection Key.
     * @return The key representing the registered channel.
     * @throws IOException if I/O error occurs.
     */
    public SelectionKey registerChannel(SelectableChannel channel, int interestOps, Object attachment)
            throws IOException {
        channel.configureBlocking(false); // Non-blocking channels
        return channel.register(selector, interestOps, attachment); // Synchronizes on the selector's key sey.
    }

    /**
     * Gets the next message in the read messages queue for the given {@code key}.
     * Note: This method synchronizes the readMessagesQueues map.
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
     * Note: This method synchronizes the writeMessagesQueues map.
     *
     * @param key     The key whose channel will be used to write the given message.
     * @param message The message to be written.
     */
    /* package */ void offerWriteMessage(SelectionKey key, byte[] message) {
        synchronized (writeMessagesQueues) {
            offerMessage(readMessagesQueues, key, message);
        }
    }


    /**
     * Adds the given {@code message} into the corresponding queue for the given {@code key}.
     * Note: This method synchronizes the readMessagesQueues map.
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
     * Note: This method synchronizes the writeMessagesQueues map.
     *
     * @param key The key whose next write message must be retrieved.
     * @return The write message, or {@code null} if no message was read.
     */
    private byte[] pollWriteMessage(SelectionKey key) {
        synchronized (writeMessagesQueues) {
            return pollMessage(writeMessagesQueues, key);
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
            Queue<byte[]> queue = writeMessagesQueues.get(key);
            return queue != null && queue.peek() != null;
        }
    }


    /**
     * Retrieves the next message in the corresponding queue for the given {@code key}, in the specified
     * {@code map} of queues.
     * Note: This method does not synchronize any object.
     *
     * @param map The map containing the queues.
     * @param key The key whose queue must be retrived.
     * @return The next message in the queue, or {@code null} if there are no messages to poll.
     */
    private byte[] pollMessage(Map<SelectionKey, Queue<byte[]>> map, SelectionKey key) {
        Queue<byte[]> queue = map.get(key);
        return queue == null ? null : queue.poll();
    }

    /**
     * Adds the specified {@code message} in the corresponding queue for the given {@code key},
     * in the specified {@code map} of queues.
     * Note: This method does not synchronize any object.
     *
     * @param map     The map containing the queues.
     * @param key     The key whose queue must be retrieved.
     * @param message The message to be offered.
     */
    private void offerMessage(Map<SelectionKey, Queue<byte[]>> map, SelectionKey key, byte[] message) {
        Queue<byte[]> queue = map.get(key);
        if (queue == null) {
            queue = new LinkedList<>();
            map.put(key, queue);
        }
        queue.offer(message);
    }


    @Override
    public void run() {

        while (true) {

            try {
                if (selector.selectNow() == 0) {
                    // Do something else...
                    checkWrites(); // check if there are new messages to be written.
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue; // Try again...
            }

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (key.isAcceptable()) {
                    // Handle accept...
                }

                if (key.isReadable()) {
                    // Handle read...
                }

                if (key.isValid() && key.isWritable()) {
                    // Handle write...
                }


            }

        }
    }


    /**
     * Checks if there are new messages to be written,
     * changing the corresponding keys' interestOps to be selected for write
     */
    private void checkWrites() {
        synchronized (writeMessagesQueues) {
            Set<SelectionKey> toBeRemovedKeys = new HashSet<>();
            for (SelectionKey key : writeMessagesQueues.keySet()) {
                if (hasUnwrittenMessages(key)) {
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                } else {
                    toBeRemovedKeys.add(key);
                }
            }
            // Removes keys that didn't have any message but had a queue.
            toBeRemovedKeys.forEach(writeMessagesQueues::remove); // TODO: check if necessary.
        }
    }
}