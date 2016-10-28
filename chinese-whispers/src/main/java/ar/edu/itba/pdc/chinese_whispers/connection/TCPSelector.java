package ar.edu.itba.pdc.chinese_whispers.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jbellini on 27/10/16.
 * <p>
 * This class handles all IO operations.
 * Implements the singleton pattern to have only one selector in all the project.
 * To register a channel, a {@link TCPHandler} must be registered with it
 * as the select operation uses it to perform operations.
 */
public final class TCPSelector {

	/**
	 * The byte buffer size.
	 */
	/* package */ static final int BUFFER_SIZE = 512;
	/**
	 * Timeout for the select operation.
	 */
	private static final int TIMEOUT = 3000;


	/**
	 * Amount of connection tries till key is cancelled.
	 */
	private static final int MAX_CONNECTION_TRIES = 10;

	/**
	 * The selector to perform IO operations.
	 */
	private final Selector selector;
	/**
	 * Contains tasks to be run when channel selection times out without selecting anything.
	 */
	private final Set<Runnable> tasks;
	/**
	 * Contains connectable keys that didn't connect yet, saving how many tries were done.
	 */
	private final Map<SelectionKey, Integer> connectionTries;


	/**
	 * Contains the singleton
	 */
	private static TCPSelector instance;


	private TCPSelector() throws IOException {
		this.selector = Selector.open();
		this.tasks = new HashSet<>();
		this.connectionTries = new HashMap<>();
	}


	public static TCPSelector getInstance() {
		if (instance == null) {
			try {
				instance = new TCPSelector();
			} catch (IOException ignored) {
			}
		}
		return instance;
	}


	public void addTask(Runnable task) {
		tasks.add(task);
	}

	public void removeTaks(Runnable task) {
		tasks.remove(task);
	}


	public boolean addServerSocketChannel(int port, TCPServerHandler handler) throws IOException {
		if (port < 0 || port > Short.MAX_VALUE || handler == null) {
			throw new IllegalArgumentException();
		}
		try {
			ServerSocketChannel channel;
			channel = ServerSocketChannel.open();
			channel.socket().bind(new InetSocketAddress(port));
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_ACCEPT, handler);
		} catch (IOException e) {
			return false;
		}
		return true;
	}


	public boolean addClientSocketChannel(String host, int port, TCPClientHandler handler) {
		if (host == null || host.equals("") || port < 0 || port > Short.MAX_VALUE || handler == null) {
			throw new IllegalArgumentException();
		}
		try {
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(host, port));
			channel.register(selector, SelectionKey.OP_CONNECT, handler);
		} catch (IOException e) {
			return false;
		}
		return true;
	}


	public boolean doSelect() {
		try {
			if (selector.select(TIMEOUT) == 0) {
				// No IO operation ...
				tasks.forEach(Runnable::run);
				return false;
			}
		} catch (IOException e) {
			return false;
		}

		// If control reached here, there are IO Operations pending to be handled
		for (SelectionKey key : selector.selectedKeys()) {
			try {

				TCPHandler handler = (TCPHandler) key.attachment();

				// Shouldn't be null, but in case ...
				if (handler == null) {
					key.cancel();
					continue;
				}
				if (key.isAcceptable()) {
					// Key only can be acceptable if it's channel is a server socket channel
					((TCPServerHandler) handler).handleAccept(key);
				}

				if (key.isConnectable()) {
					// Key only can be connectable if it's channel is a client socket channel
					((TCPClientHandler) handler).handleConnect(key);
					// Connection wasn't established
					checkConnectionTries(key);
				}

				if (key.isReadable()) {
					handler.handleRead(key);
				}

				if (key.isValid() && key.isWritable()) {
					handler.handleWrite(key);
				}
			} catch (Throwable e) {
				// If any error occurred, don't crash ...
				e.printStackTrace();

				// If the attachment is null, the attachment is not a subclass of TCPHandler,
				// or the handleError method returned true, the key is cancelled.
				if (key.attachment() == null || !(key.attachment() instanceof TCPHandler)
						|| ((TCPHandler) key.attachment()).handleError(key)) {
					key.cancel();
				}


			}
		}
		selector.selectedKeys().clear();
		return true;
	}


	private void checkConnectionTries(SelectionKey key) {
		if (((SocketChannel) key.channel()).isConnectionPending()) {
			Integer tries = connectionTries.get(key);
			if (tries == null) {
				tries = 1;
			}
			if (tries >= MAX_CONNECTION_TRIES) {
				key.cancel();
			} else {
				connectionTries.put(key, tries++);
			}
		}
	}

}