package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * Created by jbellini on 29/10/16.
 * <p>
 * This interface defines a method that will be executed when a new XMPP connection is established.
 * Upper layers can implement this interface in order to know when new XMPP connections are established.
 * For example, if a user is silenced or multiplexed, when calling this method,
 * the upper layer (the one deciding what to do which each user) can store users connected to the proxy.
 */
public interface NewConnectionsConsumer {


    /**
     * Consumes new XMPP connections.
     *
     * @param clientJid The user's JID
     */
    void consumeNewConnection(String clientJid);

}
