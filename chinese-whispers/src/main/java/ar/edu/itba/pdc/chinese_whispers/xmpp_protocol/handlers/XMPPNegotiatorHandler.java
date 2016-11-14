package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.BaseNegotiationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;

/**
 * Created by jbellini on 4/11/16.
 */
/* package */ abstract class XMPPNegotiatorHandler extends XMPPHandler {


    /**
     * The {@link BaseNegotiationProcessor} that will perform the negotiation process with xmpp entities.
     */
    private BaseNegotiationProcessor negotiationProcessor;


    /**
     * Constructor
     *
     * @param applicationProcessor The {@link ApplicationProcessor} that will process output data.
     * @param metricsProvider
     * @param configurationsConsumer
     */
    protected XMPPNegotiatorHandler(ApplicationProcessor applicationProcessor,
                                    MetricsProvider metricsProvider,
                                    ConfigurationsConsumer configurationsConsumer) {
        super(applicationProcessor, metricsProvider, configurationsConsumer);
        this.negotiationProcessor = null;
    }

    /**
     * Gets the {@link BaseNegotiationProcessor} of this handler.
     *
     * @return The negotiation processor.
     */
    protected BaseNegotiationProcessor getNegotiationProcessor() {
        return negotiationProcessor;
    }

    /**
     * Sets the {@link BaseNegotiationProcessor} for this handler.
     * Note that once is set, it can't be changed.
     *
     * @param negotiationProcessor The negotiation processor.
     * @throws IllegalArgumentException If an attempt to change the negotiation processor is done once it was set.
     */
    protected void setNegotiationProcessor(BaseNegotiationProcessor negotiationProcessor) {
        if (this.negotiationProcessor != null) {
            throw new IllegalStateException("Can't change the processor once it's set.");
        }
        this.negotiationProcessor = negotiationProcessor;
    }


    /**
     * Process done when the XMPP negotiation finishes.
     */
    abstract protected void finishXMPPNegotiation();


    @Override
    protected void afterNotifyingError() {
        // Do nothing...
    }

    @Override
    protected void afterNotifyingClose() {
        // Do nothing...
    }

    @Override
    protected void processReadMessage(byte[] message, int length) {
        if (message != null && length > 0) {
            ParserResponse parserResponse = negotiationProcessor.feed(message, length);
            handleResponse(parserResponse);
            if (parserResponse == ParserResponse.NEGOTIATION_END) {
                finishXMPPNegotiation();
            }
        }
    }

}
