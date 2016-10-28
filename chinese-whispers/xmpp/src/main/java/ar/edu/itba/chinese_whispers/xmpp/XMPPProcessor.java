package ar.edu.itba.chinese_whispers.xmpp;

import ar.edu.itba.chinese_whispers.connection.TCPSelector;

import java.util.Set;

/**
 * Created by jbellini on 27/10/16.
 */
public class XMPPProcessor {


    XMPPServerHandler handler;

    public XMPPProcessor(XMPPServerHandler handler) {
        this.handler = handler;
    }


    public void processWork() {
        Set<Integer> connections = handler.getConnections();
        for (int connection : connections) {
            byte[] message = handler.getReadMessage(connection);
            if (message != null) {
                changeMessage(message);
                handler.addWriteMessage(connection, message);
            }
        }
    }



    public void changeMessage(byte[] message) {

        if (message != null) {
            for (int i = 0; i < message.length; i++) {
                if (message[i] == 'A') {
                    message[i] = '4';
                } else if (message[i] == 'E') {
                    message[i] = '3';
                } else if (message[i] == 'I') {
                    message[i] = '1';
                } else if (message[i] == '0') {
                    message[i] = '0';
                } else if (message[i] == 'C') {
                    message[i] = '<';
                }
            }
        }
    }



}
