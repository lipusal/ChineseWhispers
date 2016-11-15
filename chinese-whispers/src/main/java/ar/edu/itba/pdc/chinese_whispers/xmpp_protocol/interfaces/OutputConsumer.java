package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * This interface defines a method to be executed when output is processed.
 * The implementor will consume output data.
 * <p>
 * Created by jbellini on 30/10/16.
 */
public interface OutputConsumer {


    /**
     * Consumes the given message.
     *
     * @param message The message to be consumed.
     */
    void consumeMessage(byte[] message);

}
