package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;

/**
 * Created by jbellini on 29/10/16.
 */
public abstract class BaseHandler implements TCPHandler {

	// Application stuff
	/**
	 * Application processor to process data.
	 */
	protected final ApplicationProcessor applicationProcessor;



	protected BaseHandler(ApplicationProcessor applicationProcessor) {
		if (applicationProcessor == null) {
			throw new IllegalArgumentException();
		}
		this.applicationProcessor = applicationProcessor;
	}


}
