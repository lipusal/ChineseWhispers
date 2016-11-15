package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.XMLInterpreter;

import java.nio.channels.SelectionKey;

/**
 * This class is in charge of reading and writing with the client or server connected to the channel in the
 * handler's {@link SelectionKey}. Basically, this handler is in charge of proxying.
 * When reading data, it will parse it using an XMLInterpreter, process it with an {@link ApplicationProcessor},
 * and send it to its peerHandler (another {@link XMPPReadWriteHandler}).
 * When receiving a message from its peerHandler, it will write that message to the socket channel
 * given in the handler's {@link SelectionKey}.
 * Note: If the user is silenced, it won't receive or be able to receive any message.
 * <p>
 * Created by jbellini on 3/11/16.
 */
/* package */ class XMPPReadWriteHandler extends XMPPHandler {


    /**
     * XML Parser
     */
    protected XMLInterpreter xmlInterpreter;


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


    /**
     * Sets the peer handler (an {@link XMPPReadWriteHandler} to this handler.
     * Note: Once it's set, it can't be changed.
     *
     * @param peerHandler The {@link XMPPReadWriteHandler} that acts as a peer to this handler.
     */
    /* package */ void setPeerHandler(XMPPReadWriteHandler peerHandler) {
        if (peerHandler == null) {
            throw new IllegalArgumentException();
        }
        if (this.peerHandler != null) {
            throw new IllegalStateException("Can't change the peer handler once it's set.");
        }
        this.peerHandler = peerHandler;
        this.xmlInterpreter = new XMLInterpreter(applicationProcessor, peerHandler);
    }

    @Override
    protected void processReadMessage(byte[] message, int length) {
        if (peerHandler == null) {
            throw new IllegalStateException();
        }
        if (message != null && length > 0) {
            xmlInterpreter.setSilenced(configurationsConsumer.isUserSilenced(clientJid));
            handleResponse(xmlInterpreter.feed(message, length));
        }
    }


    @Override
    protected void beforeRead() {

        if (this.peerHandler == null) {
            throw new IllegalStateException(); // Can't proxy if no peer handler.
        }

        // This handler can read at most the amount of data its XMLInterpreter can hold
        int maxAmountOfRead = xmlInterpreter.remainingSpace();

        // If the XMLInterpreter does not have space...
        if (maxAmountOfRead == 0) {
            disableReading(); // Stops reading if there is no space in its peer handler's output buffer
        }
        inputBuffer.position(0);
        inputBuffer.limit(maxAmountOfRead > inputBuffer.capacity() ? inputBuffer.capacity() : maxAmountOfRead);
    }

    @Override
    protected void afterWrite() {
        if (this.peerHandler == null) {
            throw new IllegalStateException(); // Can't proxy if no peer handler.
        }

        // The handle write method call flip on outputBuffer, which sets its position to 0
        // If the position is greater than 0, it means that, at least, one byte was written.
        // So, the peer handler can read again
        if (outputBuffer.position() > 0) {
            peerHandler.enableReading();
        }

    }

    @Override
    protected void afterNotifyingError() {
        if (peerHandler != null) {
            peerHandler.notifyStreamError(XMPPErrors.INTERNAL_SERVER_ERROR); // TODO: If server sends error?
        }
    }

    @Override
    protected void afterNotifyingClose() {
        if (peerHandler != null) {
            peerHandler.notifyClose();
        }
    }


    @Override
    public void handleTimeout(SelectionKey key) {
        // TODO: check these.
        notifyStreamError(XMPPErrors.CONNECTION_TIMEOUT);
        peerHandler.notifyStreamError(XMPPErrors.CONNECTION_TIMEOUT);
    }



}
