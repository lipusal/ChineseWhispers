package ar.edu.itba.chinese_whispers.application;

import ar.edu.itba.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.chinese_whispers.connection.TCPServerSelector;
import ar.edu.itba.chinese_whispers.xmpp.XMPPServerHandler;
import ar.edu.itba.chinese_whispers.xmpp.XMPPServerSelector;

import java.io.IOException;

/**
 * Created by jbellini on 21/10/16.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        XMPPServerSelector xmppServerSelector = new XMPPServerSelector();

        xmppServerSelector.addServerChannel(9000);
        while (true) {
            xmppServerSelector.doSelect();
            xmppServerSelector.performOperatons();
        }

    }
}
