package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * Created by jbellini on 1/11/16.
 * <p>
 * This interface defines a method to be executed by negotiator to feed the implementor with negotation messages.
 */
public interface NegotiationConsumer {

    /**
     * Consumes the given negotation message.
     *
     * @param negotiationMessage THe negotiation message to be consumed.
     */
    void consumeNegotiationMessage(byte[] negotiationMessage);
}
