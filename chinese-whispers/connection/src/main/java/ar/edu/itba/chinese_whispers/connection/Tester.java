package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;

/**
 * Created by jbellini on 26/10/16.
 */
public class Tester {

    public static void main(String[] args) throws IOException {
        TCPHandler handler = new TCPHandler();
        handler.addServerSocket(9000);
        handler.startListening();
    }
}
