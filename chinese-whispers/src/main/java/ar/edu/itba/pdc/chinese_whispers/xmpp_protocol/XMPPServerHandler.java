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
	public XMPPServerHandler(SelectionKey key, ApplicationProcessor applicationProcessor,
	                         NewConnectionsConsumer newConnectionsConsumer) {

		super(key);
		this.applicationProcessor = applicationProcessor;
		this.newConnectionsConsumer = newConnectionsConsumer;
	}


	@Override
	public void handleRead() {
		super.handleRead();
	}

	@Override
	public void handleWrite() {
		super.handleWrite();
	}

	@Override
	public void handleAccept(SelectionKey key) {
		try {
			SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
			channel.configureBlocking(false);
			// The handler assigned to accepted sockets won't accept new connections
			XMPPHandler xmppHandler= new XMPPServerHandler(null,applicationProcessor, null);
			SelectionKey key2 = channel.register(key.selector(), SelectionKey.OP_READ,xmppHandler);
			//TODO this should be done in a TCPCOnnecter or something.
			xmppHandler.setKey(key2);
			//key2.interestOps(SelectionKey.OP_READ);
			//TODO delete this and do it better. Only for testing now.
			xmppHandler.setOtherEndHandler(this.otherEndHandler);
			otherEndHandler.setOtherEndHandler(xmppHandler);

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