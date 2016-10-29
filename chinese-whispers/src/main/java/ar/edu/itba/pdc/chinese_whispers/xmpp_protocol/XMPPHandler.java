package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.application.Configurations;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import com.sun.deploy.xml.XMLParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by jbellini on 28/10/16.
 */
public abstract class XMPPHandler implements TCPHandler {

	/**
	 * The buffers size.
	 */
	protected static final int BUFFER_SIZE = 1024;


	/**
	 * Contains read messages
	 */
	protected final Deque<byte[]> readMessages;
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
	 * State to tell the conexion State.
	 */
	protected ConnexionState connexionState;
	/**
	 * XML Parser
	 */
	protected XMLParser xmlParser; //TODO change this to OUR xmlParser.
	/**
	 * Client JID
	 */
	protected String clientJID;

	/**
	 * Configurations Manager
	 */
	Configurations configurationsManager;


	protected XMPPHandler() {
		connexionState = ConnexionState.XMPP_STANZA_STREAM;
		this.readMessages = new LinkedList<>(); //TODO destroy this and create bytebuffer for parser.
		this.writeMessages = new ArrayDeque<>();
		this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		configurationsManager = Configurations.getConfigurations();
//		xmlParser = new XMLParser(writeMessages);
	}

	/**
	 * Returns the next message in the read message queue.
	 *
	 * @return The next read message, or {@code null} if there is no message.
	 */
	public byte[] getReadMessage() {
		return readMessages.poll();
	}

	@Override
	public void handleRead(SelectionKey key) {
		if(connexionState == ConnexionState.XMPP_STANZA_STREAM){
			SocketChannel channel = (SocketChannel) key.channel();
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
			inputBuffer.clear();
			if (message != null && message.length > 0) {
				for(byte b : message){
					writeMessages.offer(b);
					System.out.print(b);
				}
				System.out.println();
//				ParserResponse parserResponse = xmlParser.parse(message,configurationsManager.isSilenced(clientJID), configurationsManager.isL337());
//				handleResponse(parserResponse);
				//readMessages.offer(message); //TODO change this to a call to parser.
			}
		}

	}

	protected void handleResponse(ParserResponse parserResponse){
		if(parserResponse==ParserResponse.EVERYTHING_NORMAL) return;
		if(parserResponse==ParserResponse.XML_ERROR){
			StringBuffer errorResponse = new StringBuffer();
			errorResponse.append("<stream:error>\n" +
					"        <bad-format\n" +
					"            xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>\n" +
					"      </stream:error>\n" +
					"      </stream:stream>");
			byte[] message = errorResponse.toString().getBytes();//TODO check.
			readMessages.offer(message); //TODO change this to send to Q.
		}
		if (parserResponse==ParserResponse.STREAM_CLOSED){
			//TODO setBooleanSomething to closed. Wait for closure on other end.
		}
	}

	@Override
	public void handleWrite(SelectionKey key) {
		byte[] message;
		if(writeMessages.size()>BUFFER_SIZE){
			message = new byte[BUFFER_SIZE];
		}else{
			message = new  byte[writeMessages.size()];
		}
		for(int i=0; i<message.length; i++){
			message[i] = writeMessages.poll();
		}
		if (message.length>0) {
			SocketChannel channel = (SocketChannel) key.channel();
			outputBuffer.clear();
			outputBuffer.put(message);
			outputBuffer.flip();
			try {
				do {
					channel.write(outputBuffer);
				} while (outputBuffer.hasRemaining()); // Continue writing if message wasn't totally written TODO check if this is not blocking.
			} catch (IOException e) {
				int bytesSent = outputBuffer.limit() - outputBuffer.position();
				byte[] restOfMessage = new byte[message.length - bytesSent];
				System.arraycopy(message, bytesSent, restOfMessage, 0, restOfMessage.length);
			}
			if (writeMessages.isEmpty()) {
				// Turns off the write bit if there are no more messages to write
				key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE); // TODO: check how we turn on and off
			}
			outputBuffer.clear();
		}
	}
}
