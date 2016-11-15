package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * Created by jbellini on 30/10/16.
 * <p>
 * This interface defines a method to be executed when output is processed.
 * The implementor will consume output data.
 */
public interface OutputConsumer {


    /**
     * Consumes the given message.
     *
     * @param message The message to be consumed.
     * @return How many bytes were consumed.
     */
    int consumeMessage(byte[] message);

    /**
     * Returns how much space this consumer can consume.
     *
     * @return The amount of bytes that this consumer can consume.
     */
    int remainingSpace();


}
