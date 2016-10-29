package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;

import java.nio.ByteBuffer;
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
	protected final Deque<byte[]> writeMessages;
	/**
	 * Buffer from to fill when reading
	 */
	protected final ByteBuffer inputBuffer;
	/**
	 * Buffer to fill when writing
	 */
	protected final ByteBuffer outputBuffer;


	protected XMPPHandler() {
		this.readMessages = new LinkedList<>();
		this.writeMessages = new LinkedList<>();
		this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	}

	/**
	 * Returns the next message in the read message queue.
	 *
	 * @return The next read message, or {@code null} if there is no message.
	 */
	public byte[] getReadMessage() {
		return readMessages.poll();
	}

	/**
	 * Adds a message into the write message queue.
	 *
	 * @param message The message to be written.
	 */
	public void addWriteMessage(byte[] message) {
		if (message != null) {
			writeMessages.offer(message);
		}
	}
}
