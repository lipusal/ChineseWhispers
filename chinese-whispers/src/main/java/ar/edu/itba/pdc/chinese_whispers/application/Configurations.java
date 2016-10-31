package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.ProxyConfigurationProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by drocheg on 29/10/2016.
 * <p>
 * Class that manages system configurations.
 * This class implements the singleton pattern.
 */
// TODO DIEGO NO BORRAR DIEGO DIEGO TODO DIEGO TODO TODO DIEGO
public class Configurations implements ProxyConfigurationProvider {


	/**
	 * States if the system is l337ing.
	 */
	private boolean isL337;
	/**
	 * Stores users that are being silenced.
	 */
	private Set<String> silencedUsers;
	/**
	 * Stores to which server is being multiplexed each user belonging to this map key set.
	 */
	private Map<String, HostAndPort> multiplexedUsers;
	/**
	 * Stores where the default server is listening.
	 */
	private HostAndPort defaultServer;


	/**
	 * Holds the singleton.
	 */
	private static Configurations configurationsInstance;


	/**
	 * Private constructor to implement singleton pattern.
	 */
	private Configurations() {
		silencedUsers = new HashSet<>();
		isL337 = false;
		multiplexedUsers = new HashMap<>();
	}


	public void setDefaultServer(String host, int port) {
		defaultServer = new HostAndPort(host, port);
	}

	/**
	 * Gets the singleton instance.
	 *
	 * @return The singleton instance.
	 */
	public static Configurations getInstance() {
		if (configurationsInstance == null) {
			configurationsInstance = new Configurations();
		}
		return configurationsInstance;
	}

	/**
	 * Returns the l337 property (when {@code true}, system is L337ing messages).
	 *
	 * @return
	 */
	public boolean isL337() {
		return isL337;
	}

	/**
	 * Sets the L337 property (when {@code true}, system will L337 messages).
	 *
	 * @param isL337 the L337 new value.
	 */
	public void setIsL337(boolean isL337) {
		isL337 = isL337;
	}

	/**
	 * Returns {@code true} if the user is silenced (i.e. it can receive messages, but not send).
	 *
	 * @param clientJID The user's JID.
	 * @return {@code true} if the user is silenced, or {@code false} otherwise.
	 */
	public boolean isSilenced(String clientJID) {
		return silencedUsers.contains(clientJID);
	}

	/**
	 * Returns the host in which the user is being multiplexed.
	 *
	 * @param clientJid The user's JID.
	 * @return The host.
	 */
	public String getMultiplexedServerHost(String clientJid) {
		HostAndPort hap = multiplexedUsers.get(clientJid);
		return hap == null ? defaultServer.host : hap.host;
	}

	/**
	 * Returns the port in which the host (to which to user is being multiplexed) is listening.
	 *
	 * @param clientJid The user's JID
	 * @return The port.
	 */
	public Integer getMultiplexedServerPort(String clientJid) {
		HostAndPort hap = multiplexedUsers.get(clientJid);
		return hap == null ? defaultServer.port : hap.port;
	}


	/**
	 * Multiplexes an user.
	 *
	 * @param clientJid The user's JID.
	 * @param host      The host in which the user will be multiplexed.
	 * @param port      The port in which the host is listening.
	 * @return The host and port (as a String with the following format: "host":"port")
	 * where the user was being multiplexed before.
	 */
	public String multiplexUser(String clientJid, String host, int port) {
		if (clientJid == null) {
			// Rest of params are checked when creating HostAndPort object
			throw new IllegalArgumentException();
		}
		HostAndPort hap = multiplexedUsers.get(clientJid);
		if (hap == null) {
			hap = defaultServer;
		}
		multiplexedUsers.put(clientJid, new HostAndPort(host, port));
		return hap.host + ":" + hap.port;
	}


	// ProxyConfigurationProvider

	private int actualPort = 4000; // TODO: for testing! Remove when everything works together

	// TODO: remember fix these two (uncomment commented lines and remove magic results)


	@Override
	public String getServer(String clientJid) {
		return "localhost";
//		return configurations.getMultiplexedServerHost(clientJid);
	}


	@Override
	public int getServerPort(String clientJid) {
		return actualPort++;
//		return configurations.getMultiplexedServerPort(clientJid);
	}




	/**
	 * Class that encapsulates host and port.
	 */
	private static class HostAndPort {
		private final String host;
		private final int port;

		private HostAndPort(String host, int port) {
			if (host == null || host.isEmpty() || port < 0 || port > 0xFFFF) {
				throw new IllegalArgumentException();
			}
			this.host = host;
			this.port = port;
		}
	}
}
