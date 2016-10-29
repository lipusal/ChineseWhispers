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
		XMPPServerHandler xmppServerHandler = new XMPPServerHandler();
		System.out.print("Trying to bind port 9000... ");
		try {
			selector.addServerSocketChannel(9000, xmppServerHandler);
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't bind!");
			return;
		}
		System.out.println("\t[Done]");
		try {
			selector.addClientSocketChannel("localhost", 4000, new XMPPClientHandler());
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't connect to localhost:4000!");
			return;
		}
		System.out.println("\t[Done]");

		try {
			selector.addClientSocketChannel("localhost", 4001, new XMPPClientHandler());
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't connect to localhost:4001!");
			return;
		}
		System.out.println("\t[Done]");

		try {
			selector.addClientSocketChannel("localhost", 4002, new XMPPClientHandler());
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't connect to localhost:4002!");
			return;
		}
		System.out.println("\t[Done]");

		try {
			selector.addClientSocketChannel("localhost", 4003, new XMPPClientHandler());
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't connect to localhost:4003!");
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
