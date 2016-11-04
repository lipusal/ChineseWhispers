package ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces;


import java.util.List;
import java.util.Map;

/**
 * Created by jbellini on 1/11/16.
 * <p>

 */
public interface MetricsProvider {

    public Map<String,String> getMetrics();

    public void addReadBytes(long readBytes);

    public void addSentBytes(long sentBytes);

    public void addAccesses(long numAccesses);

    /**
     * Returns a list of silenced users (i.e. users that can receive messages but can not send them).
     * @return A list of silenced users' JIDs.
     */
    //  List<String> silencedUsers();
}
