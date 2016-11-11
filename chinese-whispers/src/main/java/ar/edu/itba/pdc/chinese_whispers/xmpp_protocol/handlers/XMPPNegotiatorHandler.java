package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation.XMPPNegotiator;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;

/**
 * Created by jbellini on 4/11/16.
 */
/* package */ abstract class XMPPNegotiatorHandler extends XMPPHandler {


    /**
     * The {@link XMPPNegotiator} that will handle the XMPP negotiation at the beginning of the XMPP connection.
     */
    protected XMPPNegotiator xmppNegotiator;

    /**
     * Constructor.
     *
     * @param applicationProcessor The {@link ApplicationProcessor} that will process data.
     * @param metricsProvider
     */
    protected XMPPNegotiatorHandler(ApplicationProcessor applicationProcessor, MetricsProvider metricsProvider,
                                    ConfigurationsConsumer configurationsConsumer) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
    }

    /**
     * Process done when the XMPP negotiation finishes.
     */
    abstract protected void finishXMPPNegotiation();

    @Override
    protected void processReadMessage(byte[] message, int length) {
        if (message != null && length > 0) {
            ParserResponse parserResponse = xmppNegotiator.feed(message,length);
            handleResponse(parserResponse);
            if (parserResponse == ParserResponse.NEGOTIATION_END) {
                finishXMPPNegotiation();
            }
        }
    }

}
