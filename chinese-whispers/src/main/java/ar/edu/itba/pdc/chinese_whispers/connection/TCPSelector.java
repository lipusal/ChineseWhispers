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
 * Contains a set of tasks to be performed in case no IO events were triggered when selecting.
 */
public final class TCPSelector {


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
	 * Contains the singleton.
	 */
	private static TCPSelector instance;


	/**
	 * Private constructor (for singleton pattern).
	 *
	 * @throws IOException If the selector couldn't be opened.
	 */
	private TCPSelector() throws IOException {
		this.selector = Selector.open();
		this.tasks = new HashSet<>();
		this.connectionTries = new HashMap<>();
	}


	/**
	 * Gets the singleton instance.
	 *
	 * @return The only instance of this class.
	 */
	public static TCPSelector getInstance() {
		if (instance == null) {
			try {
				instance = new TCPSelector();
			} catch (IOException ignored) {
				// Will return null if IOException is thrown
			}
		}
		return instance;
	}


	/**
	 * Adds a task to be performed when no IO operations where selected.
	 *
	 * @param task The task to be performed.
	 */
	public void addTask(Runnable task) {
		tasks.add(task);
	}

	/**
	 * Removes the given task from the set of tasks to be performed when no IO operations where selected.
	 *
	 * @param task
	 */
	public void removeTaks(Runnable task) {
		tasks.remove(task);
	}

	/**
	 * Adds a server socket channel to this selector.
	 *
	 * @param port    The port in which the server socket channel will be bound and listen for incoming connections.
	 * @param handler A {@link TCPServerHandler} to handle selected IO operations.
	 * @return The {@link SelectionKey} representing the new connection if the socket channel was bound,
	 * or {@code null} otherwise.
	 */
	public SelectionKey addServerSocketChannel(int port, TCPServerHandler handler) {
		if (port < 0 || port > 0xFFFF || handler == null) {
			throw new IllegalArgumentException();
		}
		try {
			// Will throw exception if the socket couldn't be opened.
			ServerSocketChannel channel = ServerSocketChannel.open();
			// Will throw exception if the socket couldn't bind, or if the socket is already bound.
			channel.socket().bind(new InetSocketAddress(port));
			// Will throw exception is the channel was closed (can't happen this)
			channel.configureBlocking(false);
			// Will throw exception if the channel was closed (can't happen this)
			return channel.register(selector, SelectionKey.OP_ACCEPT, handler);
		} catch (IOException e) {
			return null;
		}

	}


	/**
	 * Adds a client socket channel to this selector, and starts connecting it (in non-blocking mode).
	 *
	 * @param host    The host to be connected with.
	 * @param port    The port in which the host is listening.
	 * @param handler A {@link TCPClientHandler} to handle selected IO operations.
	 * @return The {@link SelectionKey} representing the new connection, if the socket could start connecting,
	 * or {@code null} otherwise.
	 */
	public SelectionKey addClientSocketChannel(String host, int port, TCPClientHandler handler) {
		if (host == null || host.isEmpty() || port < 0 || port > 0xFFFF || handler == null) {
			throw new IllegalArgumentException();
		}
		try {
			// Will throw exception if the socket couldn't be opened.
			SocketChannel channel = SocketChannel.open();
			// Will throw exception is the channel was closed (can't happen this)
			channel.configureBlocking(false);
			// Will throw exception if connection couldn't start
			channel.connect(new InetSocketAddress(host, port));
			// Will throw exception if the channel was closed (can't happen this)
			return channel.register(selector, SelectionKey.OP_CONNECT, handler);
		} catch (IOException e) {
			return null;
		}

	}


	/**
	 * Performs IO operations. If no IO event was triggered, the added tasks will be performed.
	 *
	 * @return {@code true} if IO events where triggered, or {@code false} otherwise.
	 */
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
					System.err.println("HEY MAN!!"); // TODO: remove this!
					continue;
				}

				// From now on, only valid keys are here...
				// One operation per select for any given key
				if (key.isAcceptable()) {
					// Key can only be acceptable if it's channel is a server socket channel
					((TCPServerHandler) handler).handleAccept(key);
				} else if (key.isConnectable()) {
					// Key can only be connectable if it's channel is a client socket channel
					((TCPClientHandler) handler).handleConnect(key);
					// Key could have been invalidated if connection was refused...
					if (key.isValid()) {
						afterTryingConnection(key); // Check if connection was established ...
					}
				} else if (key.isReadable()) {
					handler.handleRead(key);
				} else if (key.isWritable()) {
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


	/**
	 * Checks if the connection was established for the channel of the given {@link SelectionKey}.
	 * If it wasn't the amount of tries are updated.
	 * If that amount of tries is greater or equals to MAX_CONNECTION_TRIES, then the given {@code key} is cancelled.
	 *
	 * @param key The {@link SelectionKey} whose channel must be checked it it was connected.
	 */
	private void afterTryingConnection(SelectionKey key) {
		if (((SocketChannel) key.channel()).isConnectionPending()) {
			Integer tries = connectionTries.get(key);
			if (tries == null) {
				tries = 0;
			}
			tries++;
			if (tries >= MAX_CONNECTION_TRIES) {
				key.cancel();
				connectionTries.remove(key);
			} else {
				connectionTries.put(key, tries);
			}
		}
	}

}