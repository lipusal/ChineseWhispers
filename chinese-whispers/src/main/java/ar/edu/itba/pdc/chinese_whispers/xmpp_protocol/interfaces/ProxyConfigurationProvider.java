package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * Created by jbellini on 29/10/16.
 *
 * This interface defines methods to set up the proxy connections.
 */
public interface ProxyConfigurationProvider {

	/**
	 * Returns server to which the user must be connected to.
	 *
	 * @param clientJid The user's JID.
	 * @return The host to which the user must be connected.
	 */
	String getServer(String clientJid);

	/**
	 * Returns the port in which the server to which the user will be connected to is listening.
	 *
	 * @param clientJid The user's JID.
	 * @return The port.
	 */
	int getServerPort(String clientJid);
}
