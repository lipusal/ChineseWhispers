package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.handlers.AdminAcceptorHandler;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.AuthenticationProvider;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPSelector;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.XMPPAcceptorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created by jbellini on 28/10/16.
 */
public class Main {


    // TODO: get them from parameters
    private static final int ADMIN_PROTOCOL_PORT = 4444;
    private static final int XMPP_PROXY_PORT = 5223;


    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("Application started at " + new Date());

        TCPSelector selector = TCPSelector.getInstance();

        XMPPAcceptorHandler acceptorHandler = new XMPPAcceptorHandler(L337Processor.getInstance(),
                ApplicationNewConnectionsConsumer.getInstance(), Configurations.getInstance(), MetricsManager.getInstance());

        logger.info("Trying to bind port " + XMPP_PROXY_PORT + "...");
        try {
            selector.addServerSocketChannel(5223, acceptorHandler); // TODO: check why it's not breaking
        } catch (Throwable e) {
            logger.debug("Error! Couldn't bind port " + XMPP_PROXY_PORT + ". Aborting");
            return;
        }
        logger.info("Successfully bond port " + XMPP_PROXY_PORT);
        Configurations configurations = Configurations.getInstance();

        AdminAcceptorHandler administrationAcceptorHandler = new AdminAcceptorHandler(MetricsManager.getInstance(), configurations, configurations);

        logger.info("Trying to bind port " + ADMIN_PROTOCOL_PORT + "...");
        try {
            selector.addServerSocketChannel(4444, administrationAcceptorHandler);
        } catch (Throwable e) {
            logger.debug("Error! Couldn't bind port " + ADMIN_PROTOCOL_PORT + ". Aborting");
            return;
        }
        logger.info("Successfully bond port " + ADMIN_PROTOCOL_PORT);

        // Main loop
        while (true) {
            // Before select tasks...
            selector.doSelect(); // Perform select operations...
            // After select tasks...
        }
    }
}
