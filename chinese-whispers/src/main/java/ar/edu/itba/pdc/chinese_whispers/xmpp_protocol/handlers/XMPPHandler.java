package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPReadWriteHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.XMLInterpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

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
    /**
     * Buffer to fill with data to be written.
     */
    protected final ByteBuffer outputBuffer;
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


    // XMPP Stuff
    /**
     * XML Parser
     */
    protected XMLInterpreter xmlInterpreter; // Should be initialized by subclass.
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
        this.outputBuffer = ByteBuffer.allocate((BUFFER_SIZE * 4 > XMLInterpreter.MAX_AMOUNT_OF_BYTES) ?
                BUFFER_SIZE * 4 : XMLInterpreter.MAX_AMOUNT_OF_BYTES);
        this.mustClose = false;
        firstMessage = true;
        this.handlerState = HandlerState.NORMAL;
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
     * Sets this handler state in error state.
     * This method can only be called if the handler's state is {@link HandlerState#NORMAL}.
     *
     * @param error The error this handler has experienced.
     */
    public void notifyError(XMPPErrors error) {
        if (error == null) {
            throw new IllegalArgumentException();
        }
        if (handlerState != HandlerState.NORMAL) {
            return;
        }
        handlerState = HandlerState.ERROR;
        disableReading(); // Can't read anymore
        ErrorManager.getInstance().notifyError(this, error); // Notify the errors manager that an error occurred.
        afterNotifyingError();
    }

    /**
     * Sets this handler state in close state.
     * In case this handlers state is not {@link HandlerState#NORMAL}, it will not do anything.
     */
    public void notifyClose() {
        if (handlerState != HandlerState.NORMAL) {
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
     * Wrapper method to notify closing after sending an error.
     */
    public void notifyErrorWasSent() {
        if (this.handlerState != HandlerState.ERROR) {
            throw new IllegalStateException();
        }
        this.handlerState = HandlerState.NORMAL;
        notifyClose();
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
     * Returns how many bytes are allowed to be written into this handler.
     *
     * @return The amount of bytes allowed to be written into this handler.
     */
    @Override
    public int remainingSpace() {
        // After each write, the buffer's limit is set to its capacity, and its position to the next free element.
        return outputBuffer.limit() - outputBuffer.position();
    }


    /**
     * Method to be executed once a message is received (i.e. when a message is read).
     *
     * @param message The recently read message.
     */
    abstract protected void processReadMessage(byte[] message, int length);


    /**
     * Saves the given {@code message} in this handler to be sent when possible.
     * If there is no space for the all the message to be sent, it is stored only
     * the amount of space that is allowed in.
     * <p>
     * Note: In case there this handler has its {@link SelectionKey} invalidated, it will close the connection.
     *
     * @param message The message to be sent.
     * @return How many bytes could be saved in this handler, or -1 if the handler had an invalidated key.
     */
    /* package */ int writeMessage(byte[] message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (message.length == 0) {
            return 0; // Do nothing if message is empty
        }
        // TODO: check if necessary
        if (this.key != null && !this.key.isValid()) {
            handleClose(this.key); // In case the key was invalidated, make this handler close
            return -1;
        }
        if (firstMessage) {
            firstMessage = false; // TODO: check this!
        }
        int writtenBytes = message.length;
        int remainingSpace = remainingSpace();
        if (remainingSpace == 0) {
            return message.length;
        }
        // Store what can be stored
        if (message.length > remainingSpace) {
            byte[] aux = new byte[remainingSpace];
            System.arraycopy(message, 0, aux, 0, remainingSpace);
            message = aux;
            writtenBytes = remainingSpace;
        }
        System.out.println("Sending: " + new String(message)); // TODO: log? remove?
        outputBuffer.put(message); // Stores the message in the output buffer.
        enableWriting();
        return writtenBytes;
    }


    @Override
    public int consumeMessage(byte[] message) {
        if (handlerState == HandlerState.NORMAL) {
            return writeMessage(message);
        }
        return 0; // If this handler's state is not normal, it can't consume more messages.
    }



    /**
     * Performs actions based on the given {@link ParserResponse}
     *
     * @param parserResponse The response through which a decision must be taken.
     */
    protected void handleResponse(ParserResponse parserResponse) { //TODO mandarme ERROR al que lo envio, no al que recibe.

        switch (parserResponse) {
            case XML_ERROR:
                notifyError(XMPPErrors.BAD_FORMAT);
                break;
            case POLICY_VIOLATION:
                notifyError(XMPPErrors.POLICY_VIOLATION);
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
            System.out.println("Read: " + new String(inputBuffer.array(), 0, readBytes)); // TODO: log? remove?
            processReadMessage(inputBuffer.array(), inputBuffer.position());
            metricsProvider.addReadBytes(readBytes);
            // TODO: log amount of read bytes?

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

        outputBuffer.flip(); // Makes the buffer's limit be set to its position, and it position, to 0
        int writtenBytes = 0;
        SocketChannel channel = (SocketChannel) this.key.channel();
        try {
            writtenBytes = channel.write(outputBuffer);
        } catch (IOException e) {
            handleClose(this.key);
        }
        if (!outputBuffer.hasRemaining()) {
            // Disables writing if there is no more data to write
            disableWriting();
            if (mustClose) {
                // If this handler mustClose field is true, it means that it has been requested to close
                // Up to this point, all stored data was already sent, so it's ready to be closed.
                handleClose(this.key);
            }
        }
        System.out.println("Written bytes: " + writtenBytes + " Message: " + new String(outputBuffer.array(), 0, writtenBytes));
        // Makes the buffer's position be set to limit - position, and its limit, to its capacity
        // If no data remaining, it just set the position to 0 and the limit to its capacity.
        outputBuffer.compact();

        // TODO: log amount of written bytes?
        metricsProvider.addSentBytes(writtenBytes);

        afterWrite();

    }

    @Override
    public void handleTimeout(SelectionKey key) {

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


    protected enum HandlerState {
        NORMAL,
        ERROR,
        CLOSE
    }

}
