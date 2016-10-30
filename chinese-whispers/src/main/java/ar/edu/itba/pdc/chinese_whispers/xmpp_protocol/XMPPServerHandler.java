package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;


import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xml.XMLInterpreter;

import java.nio.channels.SelectionKey;

/**
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerHandler extends XMPPHandler implements TCPHandler {

	/**
	 * Says how many times the peer connection can be tried.
	 */
	private static int MAX_PEER_CONNECTIONS_TRIES = 3;


	/**
	 * A proxy connection configurator to get server and port to which a user should establish a connection.
	 */
	private final ProxyConfigurationProvider proxyConfigurationProvider;
	/**
	 * The new connections consumer that will be notified when new connections arrive.
	 */
	private final NewConnectionsConsumer newConnectionsConsumer;

	/**
	 * Holds how many peer connection tries have been done.
	 */
	private int peerConnectionTries;


	/**
	 * Constructor.
	 * This constructor will create it's {@link XMPPClientHandler} peer.
	 *
	 * @param applicationProcessor       The application processor.
	 * @param newConnectionsConsumer     The object to be notified when new XMPP connections are established.
	 * @param proxyConfigurationProvider The object to be queried for proxy configurations.
	 */
	/* package */ XMPPServerHandler(ApplicationProcessor applicationProcessor,
	                                NewConnectionsConsumer newConnectionsConsumer,
	                                ProxyConfigurationProvider proxyConfigurationProvider) {
		super(applicationProcessor);
		this.newConnectionsConsumer = newConnectionsConsumer;
		this.peerHandler = new XMPPClientHandler(applicationProcessor, this);
		this.XMLInterpreter = new XMLInterpreter(peerHandler);
		this.proxyConfigurationProvider = proxyConfigurationProvider;
		this.peerConnectionTries = 0;
		// TODO: remember to remove these two
		clientJid = "hola";
		connectPeer();
	}


	/**
	 * Method to be executed to tunnel the client being connected to this server handler into the origin server.
	 */
	/* package */ void connectPeer() {
		if (clientJid == null) {
			throw new IllegalStateException();
		}
		if (peerConnectionTries >= MAX_PEER_CONNECTIONS_TRIES) {
			// TODO: Close connection?
			closeHandler();
			return;
		}
		System.out.print("Trying to connect to origin server...");
		SelectionKey peerKey = TCPSelector.getInstance().
				addClientSocketChannel(proxyConfigurationProvider.getServer(clientJid),
						proxyConfigurationProvider.getServerPort(clientJid),
						(XMPPClientHandler) peerHandler);
		if (peerKey == null) {
			// Connection failed ...
			handleError(key); // Our own key
			return;
		}
		peerHandler.setKey(peerKey);
		peerConnectionTries++;
	}





	@Override
	public boolean handleError(SelectionKey key) {
		// TODO: what should we do in case of an error?
		return true;
	}
}