package ar.edu.itba.pdc.chinese_whispers.administration_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by jbellini on 28/10/16.
 */
public class AdministrationHandler implements TCPHandler {

    // Constants
    /**
     * The buffers size.
     */
    private static final int BUFFER_SIZE = 1024;

    // Communication stuff
    /**
     * Contains parcial messages read
     */
    private final Deque<Byte> messageRead;
    /**
     * Contains messges to be written
     */
    protected final Deque<Byte> writeMessages;
    /**
     * Buffer from to fill when reading
     */
    private final ByteBuffer inputBuffer;
    /**
     * Buffer to fill when writing
     */
    private final ByteBuffer outputBuffer;
    /**
     * boolean to tell if the handler is already closing
     */
    private boolean isClosing;


    public AdministrationHandler() {
        messageRead = new ArrayDeque<>();
        writeMessages = new ArrayDeque<>();
        inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    }



    /**
     * Makes this {@link AdministrationHandler} to be closable (i.e. stop receiving messages, send all unsent messages,
     * send close message, and close the corresponding key's channel).
     * Note: Once this method is executed, there is no chance to go back.
     */
    /* package */ void closeHandler(SelectionKey key) {
        if (isClosing) return;
        System.out.println("Close AdministrationHandler");
        isClosing = true;
        // TODO: What happens if handler contains half a message?
        if (key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ); // Invalidates reading
        } else {
            handleClose(key); // If key is not valid, proceed to close the handler without writing anything
        }
    }


    @Override
    public void handleRead(SelectionKey key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        SocketChannel channel = (SocketChannel) key.channel();
        inputBuffer.clear();
        try {
            int readBytes = channel.read(inputBuffer);
            System.out.println("ReadBytes= " + readBytes);
            if (readBytes >= 0) {
                for(byte b: inputBuffer.array()){
                    if(b=='\0'){
                        process(messageRead);
                    }else{
                        messageRead.offer(b);
                    }
                }
            } else if (readBytes == -1) {
                handleClose(key);
            }
        } catch (IOException ignored) {
            // I/O error (for example, connection reset by peer)
        }
        if(!writeMessages.isEmpty()) key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    private void process(Deque<Byte> messageRead) {
//TODO handle messages right
        //TODO process message
        while(!messageRead.isEmpty()){
            writeMessages.offer(messageRead.poll());
        }
    }

    @Override
    public void handleWrite(SelectionKey key) {// TODO: check how we turn on and off

        int byteWritten = 0;
        if (!writeMessages.isEmpty()) {
            byte[] message;
            if (writeMessages.size() > BUFFER_SIZE) {
                message = new byte[BUFFER_SIZE];
            } else {
                message = new byte[writeMessages.size()];
            }
            for (int i = 0; i < message.length; i++) {
                message[i] = writeMessages.poll();
            }
            if (message.length > 0) {
                SocketChannel channel = (SocketChannel) key.channel();
                outputBuffer.clear();
                outputBuffer.put(message);
                outputBuffer.flip();
                try {
                    do {
                        byteWritten+=channel.write(outputBuffer);
                    }
                    // TODO check if this is not blocking. In case it's blocking, we can return those bytes to the queue with a push operation (it's a deque)
                    while (outputBuffer.hasRemaining()); // Continue writing if message wasn't totally written
                } catch (IOException e) {
                    int bytesSent = outputBuffer.limit() - outputBuffer.position();
                    byte[] restOfMessage = new byte[message.length - bytesSent];
                    System.arraycopy(message, bytesSent, restOfMessage, 0, restOfMessage.length);
                }
            }
        }

        if (writeMessages.isEmpty()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            if (isClosing) {
                handleClose(key);
            }
        }

        System.out.println("Bytes written by administrator: "+ byteWritten);
    }


    @Override
    public boolean handleError(SelectionKey key) {
        return false; // TODO: change as specified in javadoc
    }

    @Override
    public boolean handleClose(SelectionKey key) {
        try {
            key.channel().close();
            // TODO: send some message before? Note: if yes, we can't close the peer's key now.
        } catch (IOException e) {

        }
        return false; // TODO: change as specified in javadoc
    }
}
