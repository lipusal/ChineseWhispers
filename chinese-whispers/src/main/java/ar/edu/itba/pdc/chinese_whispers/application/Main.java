package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.handlers.AdminAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.ClosingManager;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.ErrorManager;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.XMPPAcceptorHandler;
import org.slf4j.Logger;

import java.time.LocalDateTime;

import java.util.Date;

/**
 * Created by jbellini on 28/10/16.
 */
public class Main {


    // TODO: get them from parameters
    private static final int ADMIN_PROTOCOL_PORT = 4444;
    private static final int XMPP_PROXY_PORT = 3333;


    public static void main(String[] args) {

		Logger logger = LogHelper.getLogger(Main.class);
		logger.info("Application started at {}", LocalDateTime.now());

        TCPSelector selector = TCPSelector.getInstance();

        XMPPAcceptorHandler acceptorHandler = new XMPPAcceptorHandler(L337Processor.getInstance(),
                ApplicationNewConnectionsConsumer.getInstance(), Configurations.getInstance(), MetricsManager.getInstance());

        logger.info("Trying to bind port {}...", XMPP_PROXY_PORT);
        try {
            selector.addServerSocketChannel(XMPP_PROXY_PORT, acceptorHandler); // TODO: check why it's not breaking
        } catch (Throwable e) {
            logger.error("Couldn't bind port {}. Aborting.", XMPP_PROXY_PORT);
            return;
        }
        logger.info("Successfully bound port {}", XMPP_PROXY_PORT);
        Configurations configurations = Configurations.getInstance();

        AdminAcceptorHandler administrationAcceptorHandler = new AdminAcceptorHandler(MetricsManager.getInstance(), configurations, configurations);

        logger.info("Trying to bind port {}...", ADMIN_PROTOCOL_PORT);
        try {
            selector.addServerSocketChannel(ADMIN_PROTOCOL_PORT, administrationAcceptorHandler);
        } catch (Throwable e) {
            logger.error("Couldn't bind port {}. Aborting", ADMIN_PROTOCOL_PORT);
            return;
        }
        logger.info("Successfully bound port {}", ADMIN_PROTOCOL_PORT);

        //Initialize tasks
        ClosingManager.getInstance();
        ErrorManager.getInstance();

        // Main loop
        while (true) {
            // Before select tasks...
            selector.doSelect(); // Perform select operations...
            // After select tasks...
        }
    }
}
