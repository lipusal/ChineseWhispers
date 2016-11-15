package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.application.LogHelper;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPReadWriteHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Base XMPP handler that defines methods for sending and writing messages.
 * Each instance of this class will hold a {@link SelectionKey}, which in turn is holding (or will hold soon)
 * the instance.
 * <p>
 * Created by jbellini on 28/10/16.
 */
/* package */ abstract class XMPPHandler extends BaseHandler implements TCPReadWriteHandler, OutputConsumer {

    // Constants
    /**
     * The buffers size.
     */
    protected static final int BUFFER_SIZE = 8 * 1024; // We use an 8 KiB buffer


    // Communication stuff
    /**
     * Buffer to fill with read data.
     */
    protected final ByteBuffer inputBuffer;
//    /**
//     * Buffer to fill with data to be written.
//     */
//    protected ByteBuffer outputBuffer;
    /**
     * Says if it is the first message being sent.
     * It is used to know, in case of error, if the "stream" tag must be sent or not.
     */
    private boolean firstMessage;
    /**
     * The handler of the other end of the connection.
     */
    protected XMPPHandler peerHandler;
    /**
     * Selection Key that attaches this handler.
     */
    protected SelectionKey key;
    /**
     * Tells if this handler must be closed.
     * If true, it will be closed on the next writing operation.
     */
    private boolean mustClose;
    /**
     * Tells if the notify close operation was performed while on {@link HandlerState#ERROR} state.
     */
    private boolean closeRequestedWhileInErrorState;
    /**
     * A Deque which holds messages to be sent in the future.
     */
    private final Deque<ByteBuffer> buffers;

    // XMPP Stuff
    /**
     * Client JID
     */
    protected String clientJid; // Will be initialized when XMPP client sends "Auth" tag.


    // Other stuff
    /**
     * Tells which is this handler's state.
     */
    protected HandlerState handlerState;
    /**
     * Logger
     */
    protected final Logger logger;


    /**
     * Constructor.
     *
     * @param applicationProcessor   An object that can process XMPP messages bodies.
     * @param configurationsConsumer An object that can be queried about which server each user must connect to.
     * @param metricsProvider        An object that manages the system metrics.
     */
    protected XMPPHandler(ApplicationProcessor applicationProcessor, MetricsProvider metricsProvider,
                          ConfigurationsConsumer configurationsConsumer) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
//        this.outputBuffer = ByteBuffer.allocate(4 * ((BUFFER_SIZE > XMLInterpreter.MAX_AMOUNT_OF_BYTES) ?
//                BUFFER_SIZE : XMLInterpreter.MAX_AMOUNT_OF_BYTES));
        this.buffers = new LinkedList<>();
        this.mustClose = false;
        firstMessage = true;
        this.handlerState = HandlerState.NORMAL;
        logger = LogHelper.getLogger(getClass());
//        buffers.push(ByteBuffersManager.getByteBuffer()); // Saves one initial buffer
    }


    /**
     * Sets the {@link SelectionKey} for this handler.
     * <p>
     * Note: The given key must not have attached anything but this handler
     * (i.e. it's attachment must be null or this handler)
     *
     * @param key The new key for this handler.
     */
    /* package */ void setKey(SelectionKey key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        if (key.attachment() != null && key.attachment() != this) {
            // A handler must not hold a key that has attached another thing than itself
            throw new IllegalStateException();
        }
        this.key = key;
        // TODO: check if writing must be enabled for this key (as messages could have arrived to this handler)
    }

    /**
     * Says if the handler has already been told to send the first message.
     *
     * @return {@code true} if this handler's output buffer is empty, or {@code false} otherwise.
     */
    public boolean firstMessage() {
        return firstMessage;
    }

    /**
     * Makes this handler be able to read from its key's channel.
     *
     * @return This handler if reading could be enabled, or null otherwise.
     */
    /* package */ XMPPHandler enableReading() {
        if (this.key != null && this.key.isValid()) {
            return setKeyInterestOps(this.key.interestOps() | SelectionKey.OP_READ);
        }
        return null;
    }

    /**
     * Makes this handler be able to write to its key's channel.
     *
     * @return This handler if writing could be enabled, or null otherwise.
     */
    /* package */ XMPPHandler enableWriting() {
        if (this.key != null && this.key.isValid()) {
            return setKeyInterestOps(this.key.interestOps() | SelectionKey.OP_WRITE);
        }
        return null;
    }

    /**
     * Makes this handler not be able to read from its key's channel.
     *
     * @return This handler.
     */
    /* package */ XMPPHandler disableReading() {
        if (this.key != null && this.key.isValid()) {
            return setKeyInterestOps(this.key.interestOps() & ~SelectionKey.OP_READ);
        }
        return this;
    }

    /**
     * Makes this handler not be able to write to its key's channel.
     *
     * @return This handler.
     */
    /* package */ XMPPHandler disableWriting() {
        if (this.key != null && this.key.isValid()) {
            return setKeyInterestOps(this.key.interestOps() & ~SelectionKey.OP_WRITE);
        }
        return this;
    }


    /**
     * Wrapper method to set this handler's key interest ops.
     *
     * @param interestOps The new interest ops.
     * @return This handler.
     */
    protected XMPPHandler setKeyInterestOps(int interestOps) {
        this.key.interestOps(interestOps);
        return this;
    }

    /**
     * Actions performed after notifying this handler has reached an error situation.
     */
    protected abstract void afterNotifyingError();

    /**
     * Actions performed after notifying this handler's close
     */
    protected abstract void afterNotifyingClose();


    /**
     * Base method for notifying errors.
     * Sets this handler state in error state.
     * This method can only be called if the handler's state is {@link HandlerState#NORMAL}.
     *
     * @param error The error to be notified.
     */
    private void notifyError(XMPPErrors error) {
        if (error == null) {
            throw new IllegalArgumentException();
        }
        if (handlerState != HandlerState.NORMAL) {
            return;
        }
        handlerState = HandlerState.ERROR;
        disableReading(); // Can't read anymore
        afterNotifyingError();
    }

    /**
     * Sets this handler state in error state.
     * This method can only be called if the handler's state is {@link HandlerState#NORMAL}.
     * Note: After sending a stream error, the handler will continue with the close operation.
     *
     * @param error The error this handler has experienced.
     */
    public void notifyStreamError(XMPPErrors error) {
        notifyError(error);
        StreamErrorsManager.getInstance().notifyError(this, error); // Notify the corresponding errors manager.
    }

    /**
     * Sets this handler state in error state.
     * This method can only be called if the handler's state is {@link HandlerState#NORMAL}.
     * Note: After sending a stanza error, if closing wasn't requested, the handler state will be normal again.
     * Otherwise, it will continue with the closing operation.
     *
     * @param error The error this handler has experienced.
     */
    public void notifyStanzaError(XMPPErrors error) {
        notifyError(error);
        StreamErrorsManager.getInstance().notifyError(this, error); // Notify the corresponding errors manager.
    }

    /**
     * Sets this handler state in close state.
     * In case this handlers state is not {@link HandlerState#NORMAL}, it will not do anything.
     */
    public void notifyClose() {
        if (handlerState != HandlerState.NORMAL) {
            if (handlerState == HandlerState.ERROR) {
                closeRequestedWhileInErrorState = true;
            }
            return;
        }
        if (this.key == null) {
            return; // Nothing can be done if there is no key in this handler.
        }
        handlerState = HandlerState.CLOSE;
        disableReading(); // Stop reading, since this handler must be closed.
        ClosingManager.getInstance().notifyClose(this);
        afterNotifyingClose();
    }

    /**
     * Wrapper method to notify closing after sending an stream error.
     */
    public void notifyStreamErrorWasSent() {
        if (this.handlerState != HandlerState.ERROR) {
            return;
        }
        this.handlerState = HandlerState.NORMAL;
        notifyClose();
    }

    /**
     * Wrapper method to notify a stanza error was sent, allowing to take certain decisions
     * based on this handler's state
     */
    public void notifyStanzaErrorWasSent() {
        handlerState = HandlerState.NORMAL;
        if (closeRequestedWhileInErrorState) {
            notifyClose();
            return;
        }
        enableReading();

    }

    /**
     * Request this handler to be closed when possible.
     * If it's not in a close state, it will notify the closing.
     */
    /* package*/ void requestClose() {
        if (this.key == null) {
            return; // Can't close a handler if it does not contain a Selection Key
        }
        if (!this.key.isValid()) {
            handleClose(this.key); // As the key is not valid, just close the connection
        }
        notifyClose(); // In case it wasn't notified
        mustClose = true;
    }



    /**
     * Method to be executed once a message is received (i.e. when a message is read).
     *
     * @param message The recently read message.
     */
    abstract protected void processReadMessage(byte[] message, int length);


    /**
     * Saves the given {@code message} in this handler to be sent when possible.
     *
     * @param message The message to be sent.
     */
    /* package */ void postMessage(byte[] message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (message.length == 0 || this.key == null || !this.key.isValid()) {
            // Do nothing...
            return;
        }
        if (firstMessage) {
            firstMessage = false;
        }
        int count = 0;
        ByteBuffer actualBuffer = buffers.pollLast();
        if (actualBuffer != null) {
            count += storeInByteBuffer(actualBuffer, message, count);
        }
        while (count < message.length) {
            count += storeInByteBuffer(ByteBuffersManager.getByteBuffer(), message, count);
        }
        enableWriting();
    }

    /**
     * Stores bytes in the given {@link ByteBuffer} till the buffer is full or no more bytes must be stored.
     *
     * @param actualBuffer The buffer where data will be stored.
     * @param message      The byte array containing the data.
     * @param offset       Offset for the given message.
     * @return
     */
    private int storeInByteBuffer(ByteBuffer actualBuffer, byte[] message, int offset) {
        int stored = actualBuffer.remaining();
        if (stored + offset > message.length) {
            stored = message.length - offset;
        }
        actualBuffer.put(message, offset, stored);
        buffers.offerLast(actualBuffer);
        return stored;
    }


    @Override
    public void consumeMessage(byte[] message) {
        if (handlerState == HandlerState.NORMAL) {
            postMessage(message);
        }
    }


    /**
     * Performs actions based on the given {@link ParserResponse}
     *
     * @param parserResponse The response through which a decision must be taken.
     */
    protected void handleResponse(ParserResponse parserResponse) { //TODO mandarme ERROR al que lo envio, no al que recibe.

        switch (parserResponse) {
            case XML_ERROR:
                notifyStreamError(XMPPErrors.BAD_FORMAT);
                break;
            case POLICY_VIOLATION:
                notifyStreamError(XMPPErrors.POLICY_VIOLATION);
                break;
            case STREAM_CLOSED:
                notifyClose();
                break;
            default:
                // Any other case, do nothing...
        }
    }

    /**
     * Actions to be done before reading from this handler's key channel.
     * This method must prepare the buffer in order to read.
     */
    protected abstract void beforeRead();

    /**
     * Actions to be done after writing into this handler's key channel.
     */
    protected abstract void afterWrite();


    @Override
    public void handleRead(SelectionKey key) {
        // Before trying to read, a key must be set to this handler.
        if (this.key == null) {
            throw new IllegalStateException();
        }
        // The given key mustn't be null, and must be the same as the set key.
        if (key == null || key != this.key) {
            throw new IllegalArgumentException();
        }
        if (this.handlerState != HandlerState.NORMAL) {
            // It can happen that the state was changed after being selected but before reading
            // because the peer handler notified a close or an error situation
            // In those cases, nothing more is read.
            disableReading(); // In case reading wasn't disabled
            return; // If this handler is not in normal state, it must not read anymore
        }

        beforeRead(); // Will state how many bytes can be read

        // TODO: make position be zero and limit be capacity when its empty

        SocketChannel channel = (SocketChannel) this.key.channel();
        int readBytes = 0;
        try {
            readBytes = channel.read(inputBuffer);
        } catch (IOException e) {
            // I/O error (for example, connection reset by peer)
            handleClose(this.key); // TODO: close peer also
        }
        if (readBytes > 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("<-- {}", new String(inputBuffer.array(), 0, readBytes));
            }
            processReadMessage(inputBuffer.array(), inputBuffer.position());
            metricsProvider.addReadBytes(readBytes);
        } else if (readBytes == -1) {
            notifyClose();
        }
        // TODO: should we clear the buffer? note that this method already sets the position and limit
    }


    @Override
    public void handleWrite(SelectionKey key) {
        // Before trying to write, a key must be set to this handler.
        if (this.key == null) {
            throw new IllegalStateException();
        }
        // The given key mustn't be null, and must be the same as the set key.
        if (key == null || key != this.key) {
            throw new IllegalArgumentException();
        }

        ByteBuffer outputBuffer = buffers.pollFirst();
        if (outputBuffer == null) {
            disableWriting(); // No data to be sent, so handler must disable its writing key.
            if (mustClose) {
                // If reached this point, no data must be sent, but still the key is being selected as writable
                // That means that the handler must be closed.
                handleClose(this.key);
            }
            return; // No message to be sent
        }

        outputBuffer.flip(); // Makes the buffer's limit be set to its position, and it position, to 0
        int writtenBytes = 0;
        SocketChannel channel = (SocketChannel) this.key.channel();
        try {
            writtenBytes = channel.write(outputBuffer);
        } catch (IOException e) {
            handleClose(this.key);
        }
        if (outputBuffer.hasRemaining()) {
            outputBuffer.compact(); // Moves position to limit - position and limit to the capacity
            buffers.offerFirst(outputBuffer); // Returns the buffer to de deque
        } else {
            ByteBuffersManager.returnByteBuffer(outputBuffer); // Buffer has been completely used.
            if (buffers.isEmpty()) {
                // No more data to be written
                disableWriting();
                if (mustClose) {
                    // If this handler mustClose field is true, it means that it has been requested to close
                    // Up to this point, all stored data was already sent, so it's ready to be closed.
                    handleClose(this.key);
                }
            }
        }
        if (writtenBytes > 0 && logger.isTraceEnabled()) {
            logger.trace("--> {}", new String(outputBuffer.array(), 0, writtenBytes));
        }

        metricsProvider.addSentBytes(writtenBytes);

        afterWrite();
    }


    @Override
    public boolean handleError(SelectionKey key) {


        return false; // TODO: change as specified in javadoc
    }

    @Override
    public boolean handleClose(SelectionKey key) {
        if (key != this.key) {
            throw new IllegalArgumentException();
        }
        try {
            this.key.channel().close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    /**
     * This class is in charge of retrieving and storing instances of {@link ByteBuffer}.
     */
    private static class ByteBuffersManager {
        /**
         * A stack that contains non-used {@link ByteBuffer}s. It allows to re-use buffers.
         */
        private static final Stack<ByteBuffer> buffersStack = new Stack<>();


        /**
         * Gets a {@link ByteBuffer} from the buffers stack.
         *
         * @return An unused buffer.
         */
        private static ByteBuffer getByteBuffer() {

            if (buffersStack.isEmpty()) {
                return ByteBuffer.allocate(BUFFER_SIZE);
            }
            ByteBuffer buffer = buffersStack.pop();
            buffer.clear();
            return buffer;
        }

        /**
         * Stores the given {@link ByteBuffer} in the buffers stack.
         * It will clear the buffer, so it will lose all its information.
         *
         * @param buffer The buffer to be stored.
         */
        private static void returnByteBuffer(ByteBuffer buffer) {
            if (buffer == null) {
                throw new IllegalArgumentException();
            }
            buffersStack.push(buffer);
        }

        /**
         * Returns the capacity of the buffers given by this manager.
         *
         * @return The capacity of the buffers given by this manager.
         */
        private static int buffersSize() {
            return BUFFER_SIZE;
        }
    }


    protected enum HandlerState {
        NORMAL,
        ERROR,
        CLOSE
    }

}
