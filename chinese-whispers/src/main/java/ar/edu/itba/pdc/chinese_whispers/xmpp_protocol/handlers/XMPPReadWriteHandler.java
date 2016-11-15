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


    private final static long XMPP_TIMEOUT = 10 * 60000; // 10 minutes


    /**
     * XML Parser
     */
    protected XMLInterpreter xmlInterpreter;

    /**
     * Holds the time in which the last read was performed.
     * It is used to decide if timeout event must close the connection.
     * (Might timeout from one side, but not the other).
     */
    private long lastReadTimestamp;


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
        lastReadTimestamp = System.currentTimeMillis();
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

    /**
     * Returns when was performed the last read activity
     *
     * @return The last read activity timestamp.
     */
    private long getLastReadTimestamp() {
        return lastReadTimestamp;
    }

    /**
     * Sets a new timestamp for the read activity.
     *
     * @param lastReadTimestamp The new timestamp.
     */
    protected void setLastReadTimestamp(long lastReadTimestamp) {
        if (lastReadTimestamp < this.lastReadTimestamp) {
            throw new IllegalArgumentException();
        }
        this.lastReadTimestamp = lastReadTimestamp;
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
    protected void afterWrite() {
        if (this.peerHandler == null) {
            throw new IllegalStateException(); // Can't proxy if no peer handler.
        }
        if (outputBuffers.size() <= (MAX_AMOUNT_OF_BUFFERS_IN_THE_QUEUE / 2)) {
            peerHandler.enableReading();
        }

    }

    @Override
    protected void checkReadingKeyAfterPosting() {
        if (outputBuffers.size() >= MAX_AMOUNT_OF_BUFFERS_IN_THE_QUEUE && peerHandler != null) {
            peerHandler.disableReading();
        }
    }

    @Override
    public void handleRead(SelectionKey key) {
        super.handleRead(key);
        this.lastReadTimestamp = System.currentTimeMillis();
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
        long currentTime = System.currentTimeMillis();
        long peerLastReadActivity = ((XMPPReadWriteHandler) peerHandler).getLastReadTimestamp();
        if (currentTime - peerLastReadActivity <= XMPP_TIMEOUT) {
            ((XMPPReadWriteHandler) peerHandler).setLastReadTimestamp(currentTime);
            return;
        }
        // TODO: check these.
        notifyStreamError(XMPPErrors.CONNECTION_TIMEOUT);
        peerHandler.notifyStreamError(XMPPErrors.CONNECTION_TIMEOUT);
    }


}
