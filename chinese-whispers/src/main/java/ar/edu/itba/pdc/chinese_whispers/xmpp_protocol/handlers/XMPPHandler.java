package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NegotiationConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation.XMPPNegotiator;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.XMLInterpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Base XMPP handler that defines methods for sending and writing messages.
 * Each instance of this class will hold a {@link SelectionKey}, which in turn is holding (or will hold soon)
 * the instance.
 * <p>
 * Created by jbellini on 28/10/16.
 */
public abstract class XMPPHandler extends BaseHandler implements TCPHandler, OutputConsumer, NegotiationConsumer {

    // Constants
    /**
     * The buffers size.
     */
    protected static final int BUFFER_SIZE = 1024;
    /**
     * String to be sent when detecting error.
     * It does not contain </stream:stream> because close_message is always sent when closing.
     */
    private final static String XML_ERROR_RESPONSE = "<stream:error>\n<bad-format\n" +
            "xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n</stream:error>\n";
    /**
     * String to be sent to finish communication
     */
    private final static String CLOSE_MESSAGE = "</stream:stream>\n";


    // Communication stuff
    /**
     * Contains messages to be written by this handler.
     */
    protected final Deque<Byte> writeMessages;
    /**
     * Contains messages to be written by the negotiator.
     */
    protected final Deque<Byte> negotiatorWriteMessages;
    /**
     * Buffer to fill with read data.
     */
    protected final ByteBuffer inputBuffer;
    /**
     * Buffer to fill with data to be written.
     */
    protected final ByteBuffer outputBuffer;
    /**
     *
     */
    protected boolean firstMessage;
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
     */
    protected boolean isClosable;



    /**
     * XML Parser
     */
    protected XMLInterpreter xmlInterpreter; // Should be initialized by subclass.
    /**
     * Client JID
     */
    protected String clientJid; // Will be initialized when XMPP client sends "Auth" tag.
    /**
     * The {@link XMPPNegotiator} that will handle the XMPP negotiation at the beginning of the XMPP connection.
     */
    protected XMPPNegotiator xmppNegotiator;


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
        this.writeMessages = new ArrayDeque<>();
        this.negotiatorWriteMessages = new ArrayDeque<>();
        this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.isClosable = false;
        firstMessage =true;
    }


    /**
     * Sets the {@link SelectionKey} for this handler.
     *
     * @param key
     */
    /* package */ void setKey(SelectionKey key) {
        this.key = key;
    }


    /**
     * Method to be executed once a message is received (i.e. when a message is read).
     *
     * @param message The recently read message.
     */
    abstract protected void processReadMessage(byte[] message);


    /**
     * Saves the given {@code message} in this handler to be sent when possible.
     *
     * @param message The message to be sent.
     */
    /* package */ void writeMessage(byte[] message) {
        writeDeque(writeMessages, message);
    }

    /**
     * Saves the given {@code message} in the negotiator queue to be sent when possible.
     *
     * @param message The message to be sent.
     */
    /* package */ void writeToNegotiator(byte[] message) {
        writeDeque(negotiatorWriteMessages, message);
    }

    /**
     * Writes the given {@code message} in the given {@code deque}.
     *
     * @param deque   The {@link Deque} in which the message must be stored.
     * @param message The message to be stored.
     */
    private void writeDeque(Deque<Byte> deque, byte[] message) {
        if(firstMessage) firstMessage=false;
        if (deque == null || message == null) {
            throw new IllegalArgumentException();
        }
        for (Byte b : message) {
            deque.offer(b);
        }
        // Note that if the key is invalidated before writing the message,
        // the queue will store the message until the key is a valid one.
        if (key.isValid() && message.length > 0) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }


    @Override
    public void consumeMessage(byte[] message) {
        writeMessage(message);
    }

    @Override
    public void consumeNegotiationMessage(byte[] negotiationMessage) {
        writeMessage(negotiationMessage);
    }

    /**
     * Do stuff before being closed
     */
    /* package */ abstract void beforeClose();

    /**
     * Makes this {@link XMPPHandler} to be closable (i.e. stop receiving messages, send all unsent messages,
     * send close message, and close the corresponding key's channel).
     * It also closes the peer handler.
     * Note: Once this method is executed, there is no chance to go back.
     */
    /* package */ void closeHandler() {
        if (isClosable) {
            return;
        }
        this.isClosable = true;
        if (key == null) return;
        beforeClose();
        // TODO: What happens if handler contains half an xmpp message?
        if (this.key.isValid()) {
            this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_READ); // Invalidates reading
            writeMessage(CLOSE_MESSAGE.getBytes()); //TODO send error when doing this because there was an error.
        } else {
            handleClose(this.key); // If key is not valid, proceed to close the handler without writing anything
        }

        // Close also peer handler
        if (this.peerHandler != null) {
            peerHandler.closeHandler();
        }
    }


    /**
     * // TODO: Fill this javadoc.
     *
     * @param parserResponse The parser's response.
     */
    protected void handleResponse(ParserResponse parserResponse) { //TODO mandarme ERROR al que lo envio, no al que recibe.
        if (parserResponse == ParserResponse.EVERYTHING_NORMAL) {
            return;
        }
        if (parserResponse == ParserResponse.XML_ERROR) {
            System.out.println("handleResponse XML_ERROR: ");
            byte[] message = XML_ERROR_RESPONSE.getBytes(); //TODO check.
            if(firstMessage) writeMessage("<stream:stream>".getBytes()); //test
            writeMessage(message); //TODO change this to send to Q.
            closeHandler();
        }
        if (parserResponse == ParserResponse.STREAM_CLOSED) {
            System.out.println("handleResponse STREAM_CLOSED: ");
        }
        if(parserResponse== ParserResponse.NEGOTIATION_ERROR){ //message is sent in the negotiator
            closeHandler();
        }
    }


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

        SocketChannel channel = (SocketChannel) this.key.channel();
        byte[] message = null;
        inputBuffer.clear();
        int readBytes=0;
        try {
            readBytes = channel.read(inputBuffer);
            if (readBytes >= 0) {

                message = new byte[readBytes];
                if (readBytes > 0) { // If a message was actually read ...
                    System.arraycopy(inputBuffer.array(), 0, message, 0, readBytes);
                }
            } else if (readBytes == -1) {
                handleClose(this.key);
            }
        } catch (IOException ignored) {
            // I/O error (for example, connection reset by peer)
            // TODO: Check what we do here...
        }
        if (message != null && message.length > 0) {
            System.out.print("readBytes : " + readBytes+" message: ");
            metricsProvider.addReadBytes(readBytes);
            System.out.println(new String(message));
            processReadMessage(message);
        }
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


        doWriteMessage(writeMessages);
        doWriteMessage(negotiatorWriteMessages);


        if (writeMessages.isEmpty() && negotiatorWriteMessages.isEmpty()) {
            // Turns off the write bit if there are no more messages to write
            this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_WRITE);
            if (isClosable) {
                handleClose(key);
            }
        }
        outputBuffer.clear(); // TODO: remove if we'll use the buffer to store not sent data.
    }


    /**
     * Method that writes the content in the given {@code deque} in the handler's channel.
     *
     * @param deque The deque containing the bytes to be transmited.
     */
    private void doWriteMessage(Deque<Byte> deque) {
        if (deque == null) {
            throw new IllegalArgumentException();
        }
        if (this.key == null) {
            throw new IllegalStateException();
        }

        if (!deque.isEmpty()) {
            byte[] message;
            if (deque.size() > BUFFER_SIZE) {
                message = new byte[BUFFER_SIZE];
            } else {
                message = new byte[deque.size()];
            }
            for (int i = 0; i < message.length; i++) {
                message[i] = deque.poll();
            }
            if (message.length > 0) {
                int writtenBytes = 0;
                SocketChannel channel = (SocketChannel) this.key.channel();

                outputBuffer.clear();
                outputBuffer.put(message);
                outputBuffer.flip();
                try {
                    writtenBytes = channel.write(outputBuffer);


                    // Return the rest of the message to the deque... TODO: Use the buffer
                    if (writtenBytes < message.length) {
                        for (int i = message.length - 1; i >= writtenBytes; i--)
                            deque.push(message[i]);
                    }
                } catch (IOException e) {
                    writtenBytes = outputBuffer.limit() - outputBuffer.position();//TODO check
                    byte[] restOfMessage = new byte[message.length - writtenBytes];
                    System.arraycopy(message, writtenBytes, restOfMessage, 0, restOfMessage.length);
                    // TODO: we are not doing anything with this message
                }
                //TODO delete this:
                System.out.print("written bytes: " + writtenBytes + " message: ");
                System.out.println(new String(message));
                metricsProvider.addSentBytes(writtenBytes);
            }
        }
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
            // TODO: what should we do here?
            return false;
        }
        return true;
    }

}
