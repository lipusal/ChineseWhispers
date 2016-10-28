package ar.edu.itba.pdc.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPHandler {

	/**
	 * Handles the read operation.
	 *
	 * @param key The {@link SelectionKey} that contains the channel to be read.
	 */
	void handleRead(SelectionKey key);

	/**
	 * Handles the read operation.
	 *
	 * @param key The {@link SelectionKey} that contains the channel to be written.
	 */
	void handleWrite(SelectionKey key);

	/**
	 * Handles error situations.
	 *
	 * @param key The {@link SelectionKey} that fell into error.
	 * @return {@code true} if the key must be closed, or {@code false} otherwise.
	 */
	boolean handleError(SelectionKey key);
}