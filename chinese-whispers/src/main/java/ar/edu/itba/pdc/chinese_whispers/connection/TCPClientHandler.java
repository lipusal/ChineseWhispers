package ar.edu.itba.pdc.chinese_whispers.connection;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public interface TCPClientHandler extends TCPHandler {

	/**
	 * Handles the connect operation.
	 *
	 * @param key The {@link SelectionKey} that contains the socket channel to be connected.
	 */
	void handleConnect(SelectionKey key);
}