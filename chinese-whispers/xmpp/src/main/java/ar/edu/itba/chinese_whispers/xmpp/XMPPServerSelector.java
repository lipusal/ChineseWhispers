package ar.edu.itba.chinese_whispers.xmpp;

import ar.edu.itba.chinese_whispers.connection.TCPServerHandler;
import ar.edu.itba.chinese_whispers.connection.TCPServerSelector;

import java.io.IOException;

/**
 * Created by jbellini on 27/10/16.
 */
public class XMPPServerSelector extends TCPServerSelector {

    public XMPPServerSelector() throws IOException {
        super(new XMPPServerHandler());
    }

    protected XMPPServerHandler getTCPHandler() {
        return (XMPPServerHandler) super.getTCPHandler();
    }

    public void performOperatons() {
        getTCPHandler().performOperations();
    }

    public void printConnections() {
        getTCPHandler().printConnections();
    }
}
