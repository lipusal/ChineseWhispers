package ar.edu.itba.pdc.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by jbellini on 27/10/16.
 * <p>
 * Class that implements methods that must be done before, during, and after any IO Operation
 * This methods should be called accordingly in the handlers' methods.
 */
public class BasicIOOperations {

	// TODO: Check params in all methods

	/**
	 * Cleans the buffer contained in the given {@code key}, in order to read the most.
	 *
	 * @param key The {@link SelectionKey} that holds the channel to be read.
	 */
	public static void beforeRead(SelectionKey key) {
		((ByteBuffer) key.attachment()).clear();    // Sets the buffer position to zero, and its limit to its capacity,
	}

	/**
	 * Performs the reading operation, returning the message read as a byte array.
	 * <p>
	 * If zero bytes were read, the resultant byte array will be of length zero.
	 * If channel reached end of stream, or IO errors occurred, null is returned.
	 *
	 * @param key The {@link SelectionKey} that holds the channel to be read.
	 * @return The message read as a byte array on success, or null if errors occurred or end of stream was reached.
	 */
	public static byte[] doRead(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buf = (ByteBuffer) key.attachment();
		int readBytes;
		byte[] message = null;
		try {
			readBytes = channel.read(buf);
			if (readBytes >= 0) {
				message = new byte[readBytes];
				if (readBytes > 0) { // If a message was actually read ...
					System.arraycopy(buf.array(), 0, message, 0, readBytes);
				}
				// TODO: if readBytes is smaller than BUFFER_SIZE, should we retry reading?
				// TODO: we can read again to see if a new message arrived till total amount of read bytes == BUFFER_SIZE
			} else if (readBytes == -1) {
				channel.close(); // Channel reached end of stream
			}
		} catch (IOException ignored) {
		}
		return message; // If reached end of stream, or IO error occurred, null is returned
		// Otherwise, a byte[] of size readBytes is returned (possibly of size 0)
	}

	/**
	 * Cleans the buffer contained in the given {@code key}, leaving it empty of the next one to use it.
	 *
	 * @param key The {@link SelectionKey} that holds the read channel.
	 */
	public static void afterRead(SelectionKey key) {
		((ByteBuffer) key.attachment()).clear();    // Sets the buffer position to zero, and its limit to its capacity,
	}

	/**
	 * Cleans the buffer contained in the given {@code key}, in order to write the most.
	 * Also returns the amount of bytes that won't be written of the given {@code message}.
	 * If the message will be written totally, zero is returned.
	 *
	 * @param key     The {@link SelectionKey} that holds the channel to be written.
	 * @param message The message to analyze.
	 */
	public static int beforeWrite(SelectionKey key, byte[] message) {
		((ByteBuffer) key.attachment()).clear();
		return message.length > TCPSelector.BUFFER_SIZE ? message.length - TCPSelector.BUFFER_SIZE : 0;
	}

	/**
	 * Performs the writing operation, returning the total bytes written.
	 * <p>
	 * If message is too long (i.e. bigger than {@link TCPSelector#BUFFER_SIZE}), -1 is returned.
	 * Otherwise, the total amount of bytes written is returned.
	 *
	 * @param key The {@link SelectionKey} that holds the channel to be written.
	 * @return The total amount of written bytes, or -1 if the message was too long for writing it.
	 */
	public static int doWrite(SelectionKey key, byte[] message) {
		if (message.length > TCPSelector.BUFFER_SIZE) {
			return -1;
			// TODO: throw exception? send first part only?
		}
		ByteBuffer buf = (ByteBuffer) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();
		buf.put(message);
		buf.flip();
		int bytesSent = 0;
		try {
			do {
				bytesSent += channel.write(buf);
			} while (buf.hasRemaining()); // Continue writing if message wasn't totally written
		} catch (IOException e) {
			return bytesSent;
		}
		key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
		return bytesSent;

	}

	/**
	 * Cleans the buffer contained in the given {@code key}, leaving it empty of the next one to use it.
	 *
	 * @param key The {@link SelectionKey} that holds the written channel.
	 */
	public static void afterWrite(SelectionKey key) {
		((ByteBuffer) key.attachment()).clear();
	}


	/**
	 * Nothing must be done before accepting a new connection.
	 *
	 * @param key The {@link SelectionKey} that holds the socket that must accept the new connection.
	 */
	public static void beforeAccept(SelectionKey key) {
		// Nothing to be done ...
	}

	/**
	 * Accepts the new connection.
	 *
	 * @param key The {@link SelectionKey} that holds the socket that must accept the new connection.
	 * @return The {@link SelectionKey} that represents the new connection,
	 * or {@code null} if the connections wasn't accepted
	 */
	public static SelectionKey doAccept(SelectionKey key) {
		SelectionKey newKey = null;
		try {
			SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
			channel.configureBlocking(false);
			newKey = channel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(TCPSelector.BUFFER_SIZE));
		} catch (IOException ignored) {
		}
		return newKey;
	}

	/**
	 * Nothing must be done after accepting a new connection.
	 *
	 * @param key The {@link SelectionKey} that holds the socket that must accept the new connection.
	 */
	public static void afterAccept(SelectionKey key) {
		// Nothing to be done ...
	}

	/**
	 * Nothing must be done before connecting to a server.
	 *
	 * @param key The {@link SelectionKey} that holds the socket that must be connected.
	 */
	public static void beforeConnect(SelectionKey key) {
		// Nothing to be done ...
	}

	/**
	 * Establishes the connection with the server specified in the remote address of the given {@code key}'s channel.
	 *
	 * @param key The {@link SelectionKey} that holds the socket that must be connected.
	 * @return {@code true} if the connection was established, or {@code false} otherwise.
	 */
	public static boolean doConnect(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();
		boolean connected = channel.isConnected();
		if (!connected) {
			try {
				if (channel.isConnectionPending()) {
					connected = channel.finishConnect();
				} else {
					InetSocketAddress remote = (InetSocketAddress) channel.getRemoteAddress();
					if (remote == null) {
						throw new IllegalStateException("Remote address wasn't specified.");
					}
					connected = channel.connect(remote);
				}
			} catch (IOException ignored) {
			}
		}
		if (connected) {
			key.interestOps(SelectionKey.OP_READ);
			key.attach(ByteBuffer.allocate(TCPSelector.BUFFER_SIZE));
		}
		return connected;
	}

	/**
	 * Nothing must be done adter connecting to a server.
	 *
	 * @param key The {@link SelectionKey} that holds the socket that was connected.
	 */
	public static void afterConnect(SelectionKey key) {
		// Nothing to be done ...
	}
}