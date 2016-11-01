package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NewConnectionsConsumer;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jbellini on 29/10/16.
 * <p>
 * This {@link NewConnectionsConsumer} stores connected users in a set.
 * This class implements singleton pattern.
 */
public class ApplicationNewConnectionsConsumer implements NewConnectionsConsumer {


	private final Set<Object> connectedUsers;


	private static ApplicationNewConnectionsConsumer singleton;

	/**
	 * Private constructor to implement singleton pattern.
	 */
	private ApplicationNewConnectionsConsumer() {
		this.connectedUsers = new HashSet<>();
	}


	public static ApplicationNewConnectionsConsumer getInstance() {
		if (singleton == null) {
			singleton = new ApplicationNewConnectionsConsumer();
		}
		return singleton;
	}




	@Override
	public void consumeNewConnection(String clientJid) {
		connectedUsers.add(clientJid);
	}
}
