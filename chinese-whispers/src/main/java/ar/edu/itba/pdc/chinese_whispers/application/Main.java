package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPClientHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.XMPPServerHandler;

import java.nio.channels.SelectionKey;
import java.util.Base64;

/**
 * Created by jbellini on 28/10/16.
 */
public class Main {


	public static void main(String[] args) {


		System.out.println(new String(Base64.getDecoder().decode("AGRpZWdvAGRpZWdv")));
		TCPSelector selector = TCPSelector.getInstance();
		XMPPServerHandler xmppServerHandler = new XMPPServerHandler(new L337Processor(),
				new ApplicationNewConnectionsConsumer());
        XMPPClientHandler xmppClientHandler = new XMPPClientHandler(null);
        xmppClientHandler.setOtherEndHandler(xmppServerHandler);
        xmppServerHandler.setOtherEndHandler(xmppClientHandler);
		System.out.print("Trying to bind port 3333... "); //TODO deberia haber 1 solo handler que es el connexionsHandler. No deberian llegarle conexiones a los otros.
		try {
            SelectionKey key = selector.addServerSocketChannel(3333, xmppServerHandler);
            xmppServerHandler.setKey(key);
		} catch (Throwable e) {
			System.err.println("ERROR! Couldn't bind!");
			return;
		}
		System.out.print("Trying to bind clientSocket port 5222... ");
		try {
			SelectionKey key = selector.addClientSocketChannel("localhost",5222, xmppClientHandler);
            xmppClientHandler.setKey(key);
		} catch (Throwable e) {
			e.printStackTrace();
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
