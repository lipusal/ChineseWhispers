package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xml.XMLInterpreter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by jbellini on 28/10/16.
 */
public class XMPPClientHandler extends XMPPHandler implements TCPClientHandler {


	/**
	 * Constructor.
	 * Should only be called in {@link XMPPServerHandler}'s constructor.
	 *
	 * @param applicationProcessor The application processor.
	 * @param peerHandler          The {@link XMPPServerHandler} that corresponds to this handler.
	 */
	/* package */ XMPPClientHandler(ApplicationProcessor applicationProcessor, XMPPServerHandler peerHandler) {
		super(applicationProcessor);
		if (peerHandler == null) {
			throw new IllegalArgumentException();
		}
		this.peerHandler = peerHandler;
		this.XMLInterpreter = new XMLInterpreter(peerHandler);
	}


	@Override
	public void handleConnect(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();
		boolean connected = channel.isConnected();
		if (!connected) {
			try {
				if (channel.isConnectionPending()) {
					connected = channel.finishConnect();
				} else {
					InetSocketAddress remote = (InetSocketAddress) channel.getRemoteAddress();
					if (remote == null) {
						throw new IllegalStateException("Remote address wasn't specified."); // TODO: check this
					}
					connected = channel.connect(remote);
				}
			} catch (IOException ignored) {
			}
		}
		if (connected) {
			// If before this there was any other flag turned on, control shouldn't have reached here
			key.interestOps(SelectionKey.OP_READ);
		}


		// TODO: Add this key when connected into some set in some future class to have tracking of connections
	}

	@Override
	public boolean handleError(SelectionKey key) {
		return false;
	}
}
