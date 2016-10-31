package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.AdminAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPServerHandler;

import java.nio.channels.SelectionKey;
import java.util.Base64;

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

		AdminAcceptorHandler administrationAcceptorHandler = new AdminAcceptorHandler();

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
