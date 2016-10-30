package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.ProxyConfigurationProvider;

/**
 * Created by jbellini on 29/10/16.
 */
public class ProxyConfigurator implements ProxyConfigurationProvider {


	/**
	 * A Configuration instance to get global configurations.
	 */
	private final Configurations configurations;

	/**
	 * Singleton instance.
	 */
	private static ProxyConfigurator instance;


	/**
	 * Private constructor to implement singleton instance.
	 */
	private ProxyConfigurator() {
		this.configurations = Configurations.getInstance();
	}

	/**
	 * Singleton getter.
	 *
	 * @return The singleton instance.
	 */
	public static ProxyConfigurator getInstance() {
		if (instance == null) {
			instance = new ProxyConfigurator();
		}
		return instance;
	}

	@Override
	public String getServer(String clientJid) {
		return configurations.getMultiplexedServerHost(clientJid);
	}

	@Override
	public int getServerPort(String clientJid) {
		return configurations.getMultiplexedServerPort(clientJid);
	}
}
