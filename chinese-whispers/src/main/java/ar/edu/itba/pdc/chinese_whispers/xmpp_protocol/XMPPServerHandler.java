package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;


import ar.edu.itba.pdc.chinese_whispers.connection.TCPServerHandler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerHandler extends XMPPHandler implements TCPServerHandler {


	// TODO: move it to super class???
	/**
	 * The application processor that processes incoming data.
	 */
	private final ApplicationProcessor applicationProcessor;

	/**
	 * The new connections consumer that will be notified when new connections arrive.
	 */
	private final NewConnectionsConsumer newConnectionsConsumer;


	/**
	 * Constructor. All parameters can be {@code null}. When any parameter is {@code null}, that object win't be used.
	 *
	 * @param applicationProcessor   The application processor that processes incoming data. Can be {@code null}.
	 * @param newConnectionsConsumer The new connections connsumer that will be notified when new connections arrive.
	 *                               Can be {@code null}.
	 */
	public XMPPServerHandler(ApplicationProcessor applicationProcessor,
	                         NewConnectionsConsumer newConnectionsConsumer) {

		this.applicationProcessor = applicationProcessor;
		this.newConnectionsConsumer = newConnectionsConsumer;
	}


	@Override
	public void handleRead(SelectionKey key) {
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
			} else if (readBytes == -1) {
				channel.close(); // Channel reached end of stream
			}
		} catch (IOException ignored) {
			// I/O error (for example, connection reset by peer)
		}
		inputBuffer.clear();
		if (message != null && message.length > 0) {
			readMessages.offer(message);
		}


	}

	@Override
	public void handleWrite(SelectionKey key) {
		byte[] message = writeMessages.poll();
		if (message != null) {
			if (message.length > BUFFER_SIZE) {
				byte[] actualMessage = new byte[BUFFER_SIZE];
				System.arraycopy(message, 0, actualMessage, 0, actualMessage.length);
				byte[] wontBeSentMessage = new byte[message.length - actualMessage.length];
				System.arraycopy(message, actualMessage.length, wontBeSentMessage, 0, wontBeSentMessage.length);
				writeMessages.push(wontBeSentMessage);
				message = actualMessage;
			}
			SocketChannel channel = (SocketChannel) key.channel();
			outputBuffer.clear();
			outputBuffer.put(message);
			outputBuffer.flip();
			try {
				do {
					channel.write(outputBuffer);
				} while (outputBuffer.hasRemaining()); // Continue writing if message wasn't totally written
			} catch (IOException e) {
				int bytesSent = outputBuffer.limit() - outputBuffer.position();
				byte[] restOfMessage = new byte[message.length - bytesSent];
				System.arraycopy(message, bytesSent, restOfMessage, 0, restOfMessage.length);
				writeMessages.push(restOfMessage);
			}
			if (writeMessages.isEmpty()) {
				// Turns off the write bit if there are no more messages to write
				key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE); // TODO: check how we turn on and off
			}
			outputBuffer.clear();
		}
	}

	@Override
	public void handleAccept(SelectionKey key) {
		try {
			SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
			channel.configureBlocking(false);
			// The handler assigned to accepted sockets won't accept new connections
			channel.register(key.selector(), SelectionKey.OP_READ, new XMPPServerHandler(applicationProcessor, null));

			// TODO: Add this new key into some set in some future class to have tracking of connections

		} catch (IOException ignored) {
		}
	}

	@Override
	public boolean handleError(SelectionKey key) {
		// TODO: what should we do in case of an error?
		return true;
	}
}