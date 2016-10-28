package ar.edu.itba.chinese_whispers.application;

import ar.edu.itba.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.chinese_whispers.xmpp.XMPPProcessor;
import ar.edu.itba.chinese_whispers.xmpp.XMPPServerHandler;

import java.io.IOException;

/**
 * Created by jbellini on 21/10/16.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        XMPPServerHandler handler = new XMPPServerHandler();

        TCPSelector selector = TCPSelector.getInstance();
        selector.addServerSocketChannel(9000, handler);
        selector.addServerSocketChannel(4000, handler);
        XMPPProcessor processor = new XMPPProcessor(handler);

        while (true) {
            selector.doSelect();
            processor.processWork();
        }

    }
}
