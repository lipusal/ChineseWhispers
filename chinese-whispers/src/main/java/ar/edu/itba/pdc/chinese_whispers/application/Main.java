package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.handlers.AdminAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.AuthenticationProvider;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.XMPPAcceptorHandler;

/**
 * Created by jbellini on 28/10/16.
 */
public class Main {


	public static void main(String[] args) {


		TCPSelector selector = TCPSelector.getInstance();

		XMPPAcceptorHandler acceptorHandler = new XMPPAcceptorHandler(L337Processor.getInstance(),
				ApplicationNewConnectionsConsumer.getInstance(), Configurations.getInstance());

		System.out.print("Trying to bind port 3333... ");
		try {
            selector.addServerSocketChannel(3333, acceptorHandler);
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't bind!");
			return;
		}
		Configurations configurations = Configurations.getInstance();

		AdminAcceptorHandler administrationAcceptorHandler = new AdminAcceptorHandler(configurations, configurations, configurations);

		System.out.print("Trying to bind port 4444... ");
		try {
			selector.addServerSocketChannel(4444, administrationAcceptorHandler);
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't bind!");
			return;
		}
		System.out.println("\t[Done]");
		while (true) {
			// Before select tasks...
			selector.doSelect(); // Perform select operations...
			// After select tasks...
		}
	}
}
