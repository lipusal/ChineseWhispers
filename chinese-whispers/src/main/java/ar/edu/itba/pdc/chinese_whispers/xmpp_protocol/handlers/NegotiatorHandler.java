package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;

/**
 * Created by jbellini on 4/11/16.
 */
public abstract class NegotiatorHandler extends XMPPHandler {


    /**
     * Constructor.
     *
     * @param applicationProcessor The {@link ApplicationProcessor} that will process data.
     * @param metricsProvider
     */
    protected NegotiatorHandler(ApplicationProcessor applicationProcessor, MetricsProvider metricsProvider,
                                ConfigurationsConsumer configurationsConsumer) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
    }

    /**
     * Process done when the XMPP negotiation finishes.
     */
    abstract protected void finishXMPPNegotiation();

    @Override
    protected void processReadMessage(byte[] message) {
        if (message != null && message.length > 0) {
            ParserResponse parserResponse = xmppNegotiator.feed(message);
            handleResponse(parserResponse);
            if (parserResponse == ParserResponse.NEGOTIATION_END) {
                finishXMPPNegotiation();
            }
        }
    }

}
