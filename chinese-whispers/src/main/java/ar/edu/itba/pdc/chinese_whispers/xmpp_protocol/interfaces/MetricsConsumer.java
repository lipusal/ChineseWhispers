package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * Created by jbellini on 1/11/16.
 *
 * Interface that defines method to inform abount the use of the system.
 */
@Deprecated
public interface MetricsConsumer {


    /**
     * Tell the implementor that the given {@code amountOfBytes} were transferred.
     * @param amountOfBytes
     */
    void transferredBytes(int amountOfBytes);
}
