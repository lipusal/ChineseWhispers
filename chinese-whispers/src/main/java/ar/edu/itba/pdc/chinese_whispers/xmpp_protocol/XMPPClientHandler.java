package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPClientHandler;

import ar.edu.itba.pdc.chinese_whispers.xml.XMPPClientNegotiator;

import ar.edu.itba.pdc.chinese_whispers.xml.XMLInterpreter;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.enums.ConnectionState;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.enums.ParserResponse;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;


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
        xmppNegotiator = new XMPPClientNegotiator(negotiatorWriteMessages);
	}

	@Override
	public void handleRead(SelectionKey key) {
		byte[] message = readInputMessage();
		if (message != null && message.length > 0) {
			if(connectionState == ConnectionState.XMPP_STANZA_STREAM){
				sendProcesedStanza(message);
			}else if(connectionState==ConnectionState.XMPP_NEGOTIATION) {

				ParserResponse parserResponse = xmppNegotiator.feed(message);

				handleResponse(parserResponse);
				if(parserResponse==ParserResponse.NEGOTIATION_END){
                    connectionState=ConnectionState.XMPP_STANZA_STREAM;
					peerHandler.connectionState=ConnectionState.XMPP_STANZA_STREAM;
				}

				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			}

		}
	}


	@Override
	public void handleConnect(SelectionKey key) {
		if (key != this.key) {
			throw new IllegalArgumentException();
		}
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
			} catch (IOException e) {
				System.out.println("Connection refused");
				((XMPPServerHandler) peerHandler).connectPeer(); // Ask peer handler to retry connection

			}
		}
		if (connected) {
			// Makes the key readalbe and writable (in case there where messages waiting for being delivered)
            System.out.println("Connect success! Connected!");
            this.key.interestOps(key.interestOps() | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}else{
            System.out.println("Connect failed! Not Connected!");
        }


		// TODO: Add this key when connected into some set in some future class to have tracking of connections
	}

	@Override
	public boolean handleError(SelectionKey key) {
		return false;
	}
}
