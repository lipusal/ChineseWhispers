package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.xml.XmlInterpreter;

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
	private final static String errorResponse = "<stream:error>\n<bad-format\n" +
			"xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n</stream:error>\n</stream:stream>\n";


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


	// XMPP stuff
	/**
	 * State to tell the connection State.
	 */
	protected ConnectionState connectionState;
	/**
	 * XML Parser
	 */
	protected XmlInterpreter xmlInterpreter; //TODO change this to OUR xmlParser.
	/**
	 * Client JID
	 */
	protected String clientJid;


	/**
	 * @param applicationProcessor The {@link ApplicationProcessor} that will process data.
	 */
	protected XMPPHandler(ApplicationProcessor applicationProcessor) {
		super(applicationProcessor);
		connectionState = ConnectionState.XMPP_STANZA_STREAM;
		this.writeMessages = new ArrayDeque<>();
		this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	}


	/* package */ void setKey(SelectionKey key) {
		this.key = key;
	}

	/**
	 * Saves the message in this handler to be sent.
	 *
	 * @param message The message to be sent.
	 */
	private void writeMessage(byte[] message) {
		if (message == null) {
			throw new IllegalArgumentException();
		}
		for (Byte b : message) {
			writeMessages.offer(b);
		}
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
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
				// TODO: if readBytes is smaller than BUFFER_SIZE, should we retry reading?
				// TODO: we can read again to see if a new message arrived till total amount of read bytes == BUFFER_SIZE
				// TODO: "Diego": Para mi NO, lee lo que hay y listo. Despues volves si hay más en el otro ciclo.
			} else if (readBytes == -1) {
				channel.close(); // Channel reached end of stream
			}
		} catch (IOException ignored) {
			// I/O error (for example, connection reset by peer)
		}
		if (message != null && message.length > 0) {
			xmlInterpreter.feed(message);
			// TODO: xmlInterpreter should have something to get parsed messages, in order to give them to the peerHandler
//			peerHandler.key.interestOps(this.key.interestOps() | SelectionKey.OP_WRITE);
		}

	}


	protected void handleResponse(ParserResponse parserResponse) {
		if (parserResponse == ParserResponse.EVERYTHING_NORMAL) return;
		if (parserResponse == ParserResponse.XML_ERROR) {
			StringBuffer errorResponse = new StringBuffer();
			errorResponse.append("<stream:error>\n" +
					"        <bad-format\n" +
					"            xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n" +
					"      </stream:error>\n" +
					"      </stream:stream>");
			byte[] message = errorResponse.toString().getBytes();//TODO check.

			writeMessage(message); //TODO change this to send to Q.
		}
		if (parserResponse == ParserResponse.STREAM_CLOSED) {
			//TODO setBooleanSomething to closed. Wait for closure on other end.
		}
	}

	@Override
	public void handleWrite(SelectionKey key) {
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
				// TODO check if this is not blocking.
				while (outputBuffer.hasRemaining()); // Continue writing if message wasn't totally written
			} catch (IOException e) {
				int bytesSent = outputBuffer.limit() - outputBuffer.position();
				byte[] restOfMessage = new byte[message.length - bytesSent];
				System.arraycopy(message, bytesSent, restOfMessage, 0, restOfMessage.length);
			}
		}
		if (writeMessages.isEmpty()) {
			// Turns off the write bit if there are no more messages to write
			this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_WRITE); // TODO: check how we turn on and off
		}
		outputBuffer.clear();
	}
}
