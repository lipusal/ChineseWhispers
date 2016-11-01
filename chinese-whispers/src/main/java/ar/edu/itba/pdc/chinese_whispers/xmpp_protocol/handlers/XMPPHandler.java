package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation.XMPPNegotiator;


import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.XMLInterpreter;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by jbellini on 28/10/16.
 */
public abstract class XMPPHandler extends BaseHandler implements TCPHandler, OutputConsumer {

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


	// XMPP stuff
	/**
	 * State to tell the XMPP connection state.
	 */
	protected ConnectionState connectionState;
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
	 * @param applicationProcessor The {@link ApplicationProcessor} that will process data.
	 */
	protected XMPPHandler(ApplicationProcessor applicationProcessor) {
		super(applicationProcessor);
		connectionState = ConnectionState.XMPP_NEGOTIATION;
		this.writeMessages = new ArrayDeque<>();
		this.negotiatorWriteMessages = new ArrayDeque<>();
		this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.isClosable = false;
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




	protected void sendProcessedStanza(byte[] message) {
//		xmlInterpreter.setL337ed(Configurations.getInstance().isL337());

		// TODO: Un client debe chequear si el usuario esta silenciado?
//		xmlInterpreter.setSilenced(Configurations.getInstance().isSilenced(clientJid));
		xmlInterpreter.feed(message);
		// TODO: remove this. The XMLInterpreter calls the method consumeMessage that will set the key's interestOps.
//		peerHandler.key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
	}

	/**
	 * Saves the message in this handler to be sent when possible.
	 *
	 * @param message The message to be sent.
	 */
	/* package */ void writeMessage(byte[] message) {
		if (message == null) {
			throw new IllegalArgumentException();
		}
		for (Byte b : message) {
			writeMessages.offer(b);
		}
		// Note that if the key is invalidated before writing the message,
		// this handler will store the message until the key is a valid one.
		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	@Override
	public void consumeMessage(byte[] message) {
		writeMessage(message);
	}


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
		// TODO: What happens if handler contains half an xmpp message?
		if (this.key != null && this.key.isValid()) {
			this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_READ); // Invalidates reading
			writeMessage(CLOSE_MESSAGE.getBytes());
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
		if (parserResponse == ParserResponse.EVERYTHING_NORMAL) return;
		if (parserResponse == ParserResponse.XML_ERROR) {
			System.out.println("handleResponse XML_ERROR: ");
			byte[] message = XML_ERROR_RESPONSE.getBytes(); //TODO check.
			writeMessage(message); //TODO change this to send to Q.
			closeHandler();
		}
		if (parserResponse == ParserResponse.STREAM_CLOSED) {
			System.out.println("handleResponse STREAM_CLOSED: ");

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
		try {
			int readBytes = channel.read(inputBuffer);
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
			processReadMessage(message);
		}
	}


	@Override
	public void handleWrite(SelectionKey key) {
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
				SocketChannel channel = (SocketChannel) this.key.channel();
				outputBuffer.clear();
				outputBuffer.put(message);
				outputBuffer.flip();
				try {
					do {
						channel.write(outputBuffer);
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
		afterWriting();
	}

	/**
	 * This method must be executed at the end of the handleWrite method.
	 */
	private void afterWriting() {
		if (writeMessages.isEmpty()) {
			// Turns off the write bit if there are no more messages to write
			this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_WRITE);
			if (isClosable) {
				handleClose(key);
			}
		}
		outputBuffer.clear();
	}




    public void handleWrites(SelectionKey key) {// TODO: check how we turn on and off
        if(connectionState==ConnectionState.XMPP_STANZA_STREAM){
            System.out.println("Bytes written for XMPP_STANZA_STREAM: "+writeQ(writeMessages));
        }
        //Needs to always happen to send the succes msg.
        System.out.println("Bytes written for XMPP_NEGOTIATION: "+writeQ(negotiatorWriteMessages));

        if ((connectionState != ConnectionState.XMPP_STANZA_STREAM || writeMessages.isEmpty())
                && negotiatorWriteMessages.isEmpty()){
            this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_WRITE);
            if (isClosable) {
                handleClose(key);
            }
        }
        outputBuffer.clear();
    }

    private int writeQ(Deque<Byte> writeMessages) {
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
                SocketChannel channel = (SocketChannel) this.key.channel();
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

        return byteWritten;
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

		}
		return false; // TODO: change as specified in javadoc
	}

}
