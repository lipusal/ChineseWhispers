package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPServerHandler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by jbellini on 29/10/16.
 * <p>
 * A {@link TCPServerHandler} that only implements the {@link TCPServerHandler#handleAccept(SelectionKey)} method.
 * It can be used to separate logic of accepting and reading/writing in a {@link TCPServerHandler}.
 */
public class XMPPAcceptorHandler extends BaseHandler implements TCPServerHandler {


	/**
	 * The new connections consumer to pass it to each new {@link XMPPServerHandler}
	 */
	private final NewConnectionsConsumer newConnectionsConsumer;
	/**
	 * A proxy connection configurator to pass it to each new {@link XMPPServerHandler}.
	 */
	private final ProxyConfigurationProvider proxyConfigurationProvider;


	public XMPPAcceptorHandler(ApplicationProcessor applicationProcessor,
	                           NewConnectionsConsumer newConnectionsConsumer,
	                           ProxyConfigurationProvider proxyConfigurationProvider) {
		super(applicationProcessor);
		this.proxyConfigurationProvider = proxyConfigurationProvider;
		this.newConnectionsConsumer = newConnectionsConsumer;
	}


	@Override
	public void handleRead(SelectionKey key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void handleWrite(SelectionKey key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean handleError(SelectionKey key) {
		// TODO: what should we do in case of an error?
		return true;
	}

	@Override
	public boolean handleClose(SelectionKey key) {
		// TODO: What should we do when clossing the acceptor? Close all connection? Let them finish when they want?
		return true;
	}

	@Override
	public void handleAccept(SelectionKey key) {
		try {
			SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
			channel.configureBlocking(false);
			XMPPServerHandler handler = new XMPPServerHandler(applicationProcessor,
					newConnectionsConsumer,
					proxyConfigurationProvider);
			// The handler assigned to accepted sockets won't accept new connections, it will read and write
			// (it's writable upon creation because it might be created with data in its write messages queue)
			SelectionKey newKey = channel.register(key.selector(),
					SelectionKey.OP_READ | SelectionKey.OP_WRITE, handler);
			handler.setKey(newKey);

			// TODO: Add this new key into some set in some future class to have tracking of connections
			// TODO this should be done in a TCPCOnnecter or something.
		} catch (IOException ignored) {
		}
	}
}
