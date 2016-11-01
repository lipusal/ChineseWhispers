package ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces;

/**
 * Created by jbellini on 1/11/16.
 * <p>
 * Interface that defines methods that will consume configuration changes
 * when changes of configurations are requested by the administration protocol.
 */
public interface ConfigurationsConsumer {

    /**
     * Sets the L337 property (when {@code true}, system will L337 messages).
     *
     * @param isL337 the L337 new value.
     */
    void setL337Processing(boolean isL337);

    /**
     * Silences the given user (i.e. the user can receive messages but not send them).
     *
     * @param clientJid The user's JID.
     */
    void silenceUser(String clientJid);

    /**
     * Makes the given user not be silenced any more..
     *
     * @param clientJid
     */
    void unSilenceUser(String clientJid);

    /**
     * Makes the system define as default server the one passed in the parameters.
     *
     * @param host The host where the server is running.
     * @param port The port in which the server is listening.
     */
    void setDefaultServer(String host, int port);

    /**
     * Multiplexes an user.
     *
     * @param clientJid The user's JID.
     * @param host      The host in which the user will be multiplexed.
     * @param port      The port in which the host is listening.
     * @return The host and port (as a String with the following format: "host":"port")
     * where the user was being multiplexed before.
     */
    void multiplexUser(String clientJid, String host, int port);

    /**
     * Multiplexes the user to the default server.
     *
     * @param clientJid The user's JID.
     */
    void multiplexToDefaultServer(String clientJid);
}
