package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;


import ar.edu.itba.pdc.chinese_whispers.connection.TCPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPServerHandler;
import ar.edu.itba.pdc.chinese_whispers.xml.XmlInterpreter;

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
	public XMPPServerHandler(ApplicationProcessor applicationProcessor,
	                         NewConnectionsConsumer newConnectionsConsumer) {
		super();
		this.applicationProcessor = applicationProcessor;
		this.newConnectionsConsumer = newConnectionsConsumer;
		otherEndHandler = new XMPPClientHandler(this);
		xmlInterpreter = new XmlInterpreter(otherEndHandler.writeMessages);
		otherEndHandler.xmlInterpreter=new XmlInterpreter(writeMessages);
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
			XMPPHandler xmppServerHandler= new XMPPServerHandler(applicationProcessor, null);
			SelectionKey serverHandlerKey = channel.register(key.selector(), SelectionKey.OP_READ,xmppServerHandler);
			//TODO this should be done in a TCPCOnnecter or something.
			xmppServerHandler.setKey(serverHandlerKey);

			//TODO set this in other place
			System.out.print("Trying to bind clientSocket port 5222... ");
			try {
				SelectionKey clientHandlerKey = (TCPSelector.getInstance()).addClientSocketChannel("localhost",5222, (TCPClientHandler)xmppServerHandler.otherEndHandler);
				xmppServerHandler.otherEndHandler.setKey(clientHandlerKey);
			} catch (Throwable e) {
				e.printStackTrace();
				System.err.println("ERROR! Couldn't bind!");
				return;
			}



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