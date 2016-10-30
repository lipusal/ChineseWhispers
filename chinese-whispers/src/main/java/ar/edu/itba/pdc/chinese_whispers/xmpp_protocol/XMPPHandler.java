package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.xml.XMLInterpreter;

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
	 */
	private final static String ERROR_RESPONSE = "<stream:error>\n<bad-format\n" +
			"xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n</stream:error>\n</stream:stream>\n";

	private final static String CLOSE_MESSAGE = "</stream>\n";


	// Communication stuff
	/**
	 * Contains messges to be written
	 */
	protected final Deque<Byte> writeMessages;
	/**
	 * Buffer from to fill when reading
	 */
	protected final ByteBuffer inputBuffer;
	/**
	 * Buffer to fill when writing
	 */
	protected final ByteBuffer outputBuffer;
	/**
	 * The handler of the other end of the connection
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
	 * State to tell the connection State.
	 */
	protected ConnectionState connectionState;
	/**
	 * XML Parser
	 */
	protected XMLInterpreter XMLInterpreter; // Should be initialized by subclass.
	/**
	 * Client JID
	 */
	protected String clientJid; // Will be initialized when XMPP client sends "Auth" tag.


	/**
	 * @param applicationProcessor The {@link ApplicationProcessor} that will process data.
	 */
	protected XMPPHandler(ApplicationProcessor applicationProcessor) {
		super(applicationProcessor);
		connectionState = ConnectionState.XMPP_STANZA_STREAM;
		this.writeMessages = new ArrayDeque<>();
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
	 * Makes this {@link XMPPHandler} to be closable (i.e. stop receiving messages, send all unsent messages,
	 * send close message, and close the corresponding key's channel).
	 * Note: Once this method is executed, there is no chance to go back.
	 */
	/* package */ void closeHandler() {
		if (isClosable) {
			return;
		}
		this.isClosable = true;
		// TODO: What happens if handler contains half an xmpp message?
		if (this.key.isValid()) {
			this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_READ); // Invalidates reading
			writeMessage(CLOSE_MESSAGE.getBytes());
		} else {
			handleClose(this.key); // If key is not valid, proceed to close the handler without writing anything
		}
	}

	/**
	 * Saves the message in this handler to be sent.
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
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
	}


	/**
	 * // TODO: Fill this javadoc.
	 *
	 * @param parserResponse The parser's response.
	 */
	protected void handleResponse(ParserResponse parserResponse) { //TODO mandarme ERROR al que lo envio, no al que recibe.
		if (parserResponse == ParserResponse.EVERYTHING_NORMAL) return;
		if (parserResponse == ParserResponse.XML_ERROR) {
			byte[] message = ERROR_RESPONSE.getBytes(); //TODO check.
			writeMessage(message); //TODO change this to send to Q.
		}
		if (parserResponse == ParserResponse.STREAM_CLOSED) {
			//TODO setBooleanSomething to closed. Wait for closure on other end.
		}
	}


	@Override
	public void consumeMessage(byte[] message) {
		writeMessage(message);
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
		}
		if (message != null && message.length > 0) {
			//XMLInterpreter.feed(message);
			peerHandler.writeMessage(message); // TODO: remember to fix this.
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
		handleAfterWrite();
	}

	/**
	 * This method must be executed at the end of the handleWrite method.
	 */
	private void handleAfterWrite() {
		if (writeMessages.isEmpty()) {
			// Turns off the write bit if there are no more messages to write
			this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_WRITE);
			if (isClosable) {
				handleClose(key);
			}
		}
		outputBuffer.clear();
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
			// TODO: send some message before? Note: if yes, we can't close the peer's key now.
			if (this.peerHandler != null) {
				this.peerHandler.closeHandler();
			}
		} catch (IOException e) {

		}
		return false; // TODO: change as specified in javadoc
	}
}
