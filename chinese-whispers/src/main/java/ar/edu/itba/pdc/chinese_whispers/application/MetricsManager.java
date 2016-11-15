package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dgrimau on 03/11/16.
 */
public class MetricsManager implements MetricsProvider {

    /**
     * Number of bytes read by the proxy
     */
    private long readBytes;

    /**
     * Number of bytes sent by the proxy
     */
    private long sentBytes;

    /**
     * Number of bytes read by the proxy for the administration protocol
     */
    private long administrationReadBytes;

    /**
     * Number of bytes sent by the proxy for the administration protocol
     */
    private long administrationSentBytes;

    /**
     * Number of messages silenced by the proxy
     */
    private long numMessagesSilenced;

    /**
     * Total number of connexion to proxy
     */
    private long numAccesses;//TODO check if this was it?

    /**
     * Holds the singleton instance.
     */
    private static MetricsManager singleton;

    private MetricsManager() {
        readBytes=0;
        sentBytes=0;
        numAccesses=0;
    }

    public static MetricsProvider getInstance() {
        if (singleton == null) {
            singleton = new MetricsManager();
        }
        return singleton;
    }

    public Map<String,String> getMetrics(){
        Map<String,String> metrics = new HashMap();
        metrics.put("numAccesses",String.valueOf(numAccesses));
        metrics.put("sentBytes",String.valueOf(sentBytes));
        metrics.put("readBytes",String.valueOf(readBytes));
        metrics.put("administrationReadBytes",String.valueOf(administrationReadBytes));
        metrics.put("administrationSentBytes",String.valueOf(administrationSentBytes));
        metrics.put("numSilencedMessages",String.valueOf(numMessagesSilenced));
        return metrics;
    }

    public void addReadBytes(long readBytes){
        this.readBytes+=readBytes;
    }

    public void addSentBytes(long sentBytes){
        this.sentBytes+=sentBytes;
    }

    public void addAccesses(long numAccesses){
        this.numAccesses+=numAccesses;
    }

    public void addAdministrationReadBytes(long administrationReadBytes){
        this.administrationReadBytes+=administrationReadBytes;
    }

    public void addAdministrationSentBytes(long administrationSentBytes){
        this.administrationSentBytes+=administrationSentBytes;
    }

    public void addNumSilencedMessages(long numMessagesSilenced){
        this.numMessagesSilenced+=numMessagesSilenced;
    }
}
