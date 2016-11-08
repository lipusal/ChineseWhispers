package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;

/**
 * Base {@link TCPHandler} from which all XMPP handlers should extend.
 * It has some fields that will be used by all subclasses.
 * <p>
 * Created by jbellini on 29/10/16.
 */
public abstract class BaseHandler implements TCPHandler {

    // Application stuff
    /**
     * Application processor to process data.
     */
    protected final ApplicationProcessor applicationProcessor;

    /**
     * The metric manager to give or ask metrics.
     */
    protected final MetricsProvider metricsProvider;

    /**
     * A proxy connection configurator to get server and port to which a user should establish a connection.
     */
    protected final ConfigurationsConsumer configurationsConsumer;


    /**
     * Constructor.
     *
     * @param applicationProcessor   An object that can process XMPP messages bodies.
     * @param configurationsConsumer An object that can be queried about which server each user must connect to.
     * @param metricsProvider        An object that manages the system metrics.
     */
    protected BaseHandler(ApplicationProcessor applicationProcessor, MetricsProvider metricsProvider,
                          ConfigurationsConsumer configurationsConsumer) {
        if (applicationProcessor == null) {
            throw new IllegalArgumentException();
        }
        this.applicationProcessor = applicationProcessor;
        this.metricsProvider = metricsProvider;
        this.configurationsConsumer = configurationsConsumer;
    }


}
