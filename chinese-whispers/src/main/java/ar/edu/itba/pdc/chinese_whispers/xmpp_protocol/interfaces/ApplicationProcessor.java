package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;


import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.XMPPServerHandler;


/**
 * Created by jbellini on 29/10/16.
 * <p>
 * Interface that defines methods that are executed to process data.
 * Note: In {@link XMPPServerHandler} the same instance is used for each connection.
 * TODO: check which class finally contains the processor
 */
public interface ApplicationProcessor {


    /**
     * Method to be executed when parsing content that is part of a body.
     *
     * @param stringBuilder The string builder to build the final message.
     * @param message       The message to be parsed.
     */
    void processMessageBody(StringBuilder stringBuilder, char[] message);

    /**
     * Method to be executed when parsing content that is part of a body.
     *
     * @param message The message to be processed.
     */
    @Deprecated
    byte[] processMessageBody(byte[] message);

    /**
     * Method to be executed when parsing content that is part of a body.
     *
     * @param message The message to be processed.
     */
    @Deprecated
    void processMessageBody(String message);
}
