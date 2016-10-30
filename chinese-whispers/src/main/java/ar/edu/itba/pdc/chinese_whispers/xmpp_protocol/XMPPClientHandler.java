package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xml.XMPPClientNegotiator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by jbellini on 28/10/16.
 */
public class XMPPClientHandler extends XMPPHandler implements TCPClientHandler {


	public XMPPClientHandler(XMPPServerHandler xmppServerHandler) {
		super();
		otherEndHandler= xmppServerHandler;
		xmppNegotiator = new XMPPClientNegotiator(negotiatorWriteMessages);
	}

	@Override
	public void handleRead() {
		byte[] message = readInputMessage();
		if (message != null && message.length > 0) {
			if(connexionState == ConnexionState.XMPP_STANZA_STREAM){
				sendProcesedStanza(message);
			}else if(connexionState==ConnexionState.XMPP_NEGOTIATION) {

				ParserResponse parserResponse = xmppNegotiator.feed(message);

				if(parserResponse==ParserResponse.NEGOTIATION_END){
					connexionState=ConnexionState.XMPP_STANZA_STREAM;
					otherEndHandler.connexionState=ConnexionState.XMPP_STANZA_STREAM;
				}

				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			}

		}
	}

	@Override
	public void handleWrite() {
		super.handleWrite();
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
