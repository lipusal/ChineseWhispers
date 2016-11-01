package ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces;

import java.util.List;

/**
 * Created by jbellini on 1/11/16.
 * <p>
 * Interface that defines method to get metrics when these are requested through the administration protocol.
 */
public interface MetricsProvider {

    /**
     * Returns how many bytes where proxied.
     *
     * @return The amount of bytes that passed through the system.
     */
    int transportedBytes();

    /**
     * Returns a list of silenced users (i.e. users that can receive messages but can not send them).
     * @return A list of silenced users' JIDs.
     */
    List<String> silencedUsers();
}
