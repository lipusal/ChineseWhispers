package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;


import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPServerHandler;
import ar.edu.itba.pdc.chinese_whispers.xml.XMPPServerNegotitator;
import ar.edu.itba.pdc.chinese_whispers.xml.XMLInterpreter;


import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.Map;

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
        xmppNegotiator = new XMPPServerNegotitator(negotiatorWriteMessages);

    }


	@Override
	public void handleRead(SelectionKey key) {
        byte[] message = readInputMessage(key);
        if (message != null && message.length > 0) {

            if (connectionState == ConnectionState.XMPP_STANZA_STREAM) {
                sendProcesedStanza(message);
            } else if (connectionState == ConnectionState.XMPP_NEGOTIATION) {

                ParserResponse parserResponse = xmppNegotiator.feed(message);
                if (parserResponse == ParserResponse.NEGOTIATION_END) {

                    //Initialize data obtained
                    connectionState = ConnectionState.XMPP_STANZA_STREAM;
                    Map initialNegotiationParameters = xmppNegotiator.getInitialParameters();
                    peerHandler.xmppNegotiator.setAuthorization(xmppNegotiator.getAuthorization());
                    peerHandler.xmppNegotiator.setInitialParameters(initialNegotiationParameters);
                    if (!initialNegotiationParameters.containsKey("to")) {
                        throw new IllegalStateException();
                        //TODO send error? Send error before this?
                    } else {
                        String authDecoded = new String(Base64.getDecoder().decode(xmppNegotiator.getAuthorization()));
                        String[] authParameters = authDecoded.split("\0");
                        if (authParameters.length != 3) { //Nothing, user, pass
                            throw new IllegalStateException();
                            //TODO send error? Send error before this?
                        }
                        clientJid = authParameters[1] + "@" + initialNegotiationParameters.get("to");
                        //Uncomment this to silence arriving msg. What should be done of clientJID of server?
                        //otherEndHandler.clientJID=clientJID;
                    }

                    //Connect Peer
                    connectPeer();

                    //Generates first message
                    StringBuilder startStream = new StringBuilder();
                    startStream.append("<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" ");
                    for (String attributeKey : xmppNegotiator.getInitialParameters().keySet()) {
                        startStream.append(attributeKey)
                                .append("=\"")
                                .append(xmppNegotiator.getInitialParameters().get(attributeKey))
                                .append("\" ");
                    }
                    startStream.append("> ");
                    System.out.println("Proxy to Server:" + startStream);

                    //Sends first message to otherEndHandler for him to send
                    byte[] bytes = startStream.toString().getBytes();
                    for (byte b : bytes) {
                        peerHandler.negotiatorWriteMessages.offer(b);
                    }

                    //Sets write key of other to writable to send the message
                    //peerHandler.key.interestOps(peerHandler.key.interestOps() | SelectionKey.OP_WRITE);
                }

                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }


        }
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