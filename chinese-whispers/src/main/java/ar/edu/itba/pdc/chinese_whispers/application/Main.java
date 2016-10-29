package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPServerHandler;

/**
 * Created by jbellini on 28/10/16.
 */
public class Main {


	public static void main(String[] args) {


		TCPSelector selector = TCPSelector.getInstance();
		XMPPServerHandler xmppServerHandler = new XMPPServerHandler(new L337Processor(),
				new ApplicationNewConnectionsConsumer());
		System.out.print("Trying to bind port 9000... ");
		try {
			selector.addServerSocketChannel(9000, xmppServerHandler);
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
