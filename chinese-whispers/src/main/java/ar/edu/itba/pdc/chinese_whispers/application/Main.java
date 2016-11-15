package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.handlers.AdminAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.ClosingManager;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.StreamErrorsManager;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.XMPPAcceptorHandler;
import org.slf4j.Logger;

import java.time.LocalDateTime;

/**
 * Created by jbellini on 28/10/16.
 */
public class Main {

    private static  int adminProtocolPort;
    private static  int xmppProxyPort;
    private static  int defaultPort;
    private static  String defaultServer;


//    // TODO: get them from parameters
    private static final int ADMIN_PROTOCOL_PORT = 4444;
    private static final int XMPP_PROXY_PORT = 3333;


    public static void main(String[] args) {

//        if(args.length!=4) return;
//        try {
//            adminProtocolPort = Integer.valueOf(args[1]);
//            xmppProxyPort = Integer.valueOf(args[0]);
//            defaultServer = args[2];
//            defaultPort =  Integer.valueOf(args[3]);
//            Configurations.getInstance().setDefaultServer(defaultServer,defaultPort);
//        }catch (Exception e){
//            //TODO make cleaner clean.
//            return;
//        }


        adminProtocolPort = ADMIN_PROTOCOL_PORT;
        xmppProxyPort = XMPP_PROXY_PORT;

		Logger logger = LogHelper.getLogger(Main.class);
		logger.info("Application started at {}", LocalDateTime.now());

        TCPSelector selector = TCPSelector.getInstance();

        XMPPAcceptorHandler acceptorHandler = new XMPPAcceptorHandler(L337Processor.getInstance(),
                ApplicationNewConnectionsConsumer.getInstance(), Configurations.getInstance(), MetricsManager.getInstance());

        logger.info("Trying to bind port {}...", xmppProxyPort);
        try {
            selector.addServerSocketChannel(xmppProxyPort, acceptorHandler); // TODO: check why it's not breaking
        } catch (Throwable e) {
            logger.error("Couldn't bind port {}. Aborting.", xmppProxyPort);
            return;
        }
        logger.info("Successfully bound port {}", xmppProxyPort);
        Configurations configurations = Configurations.getInstance();

        AdminAcceptorHandler administrationAcceptorHandler = new AdminAcceptorHandler(MetricsManager.getInstance(), configurations, configurations);

        logger.info("Trying to bind port {}...", adminProtocolPort);
        try {
            selector.addServerSocketChannel(adminProtocolPort, administrationAcceptorHandler);
        } catch (Throwable e) {
            logger.error("Couldn't bind port {}. Aborting", adminProtocolPort);
            return;
        }
        logger.info("Successfully bound port {}", adminProtocolPort);

        //Initialize tasks
        ClosingManager.getInstance();
        StreamErrorsManager.getInstance();


        // Main loop
        while (true) {
            // Before select tasks...
            selector.doSelect(); // Perform select operations...
            // After select tasks...
        }
    }
}
