package ar.edu.itba.pdc.chinese_whispers.application;

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


		System.out.println(new String(Base64.getDecoder().decode("AGRpZWdvAGRpZWdv")));
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

		//TODO borrar esta bulllshiit
//        for(int i=0; i<1000000000; i++);

		System.out.println("\t[Done]");
		while (true) {
			// Before select tasks...
			selector.doSelect(); // Perform select operations...
			// After select tasks...
		}
	}
}
