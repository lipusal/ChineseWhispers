package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.XMLInterpreter;

import java.nio.channels.SelectionKey;

/**
 * This class is in charge of reading and writing with the client or server connected to the channel in the
 * handler's {@link SelectionKey}.
 * When reading data, it will parse it using an XMLInterpreter, process it with an {@link ApplicationProcessor},
 * and send it to its peerHandler (another {@link XMPPReadWriteHandler}).
 * When receiving a message from its peerHandler, it will write that message to the socket channel
 * given in the handler's {@link SelectionKey}.
 * Note: If the user is silenced, it won't receive or be able to receive any message.
 * <p>
 * Created by jbellini on 3/11/16.
 */
public class XMPPReadWriteHandler extends XMPPHandler implements TCPHandler {


    /* package */ XMPPReadWriteHandler(ApplicationProcessor applicationProcessor,
                                       MetricsProvider metricsProvider,
                                       ConfigurationsConsumer configurationsConsumer,
                                       String clientJid,
                                       SelectionKey key) {
        this(applicationProcessor, metricsProvider, configurationsConsumer, clientJid, key, null);
    }

    /* package */ XMPPReadWriteHandler(ApplicationProcessor applicationProcessor,
                                       MetricsProvider metricsProvider,
                                       ConfigurationsConsumer configurationsConsumer,
                                       String clientJid,
                                       SelectionKey key,
                                       XMPPReadWriteHandler peerHandler) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        this.clientJid = clientJid;
        this.key = key;
        this.peerHandler = peerHandler;
        if (peerHandler != null) {
            this.xmlInterpreter = new XMLInterpreter(applicationProcessor, peerHandler);
        }
    }


    /* package */ void setPeerHandler(XMPPReadWriteHandler peerHandler) {
        this.peerHandler = peerHandler;
        this.xmlInterpreter = new XMLInterpreter(applicationProcessor, peerHandler);
    }

    @Override
    protected void processReadMessage(byte[] message) {
        if (peerHandler == null) {
            throw new IllegalArgumentException();
        }
        if (message != null && message.length > 0) {
            xmlInterpreter.setSilenced(configurationsConsumer.isUserSilenced(clientJid));
            xmlInterpreter.feed(message);
        }
    }

    @Override
    void beforeClose() {

    }


}
