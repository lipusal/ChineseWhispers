package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

/**
 * Created by jbellini on 29/10/16.
 *
 * Interface that defines methods that are executed to process data.
 * Note: In {@link XMPPServerHandler}, the same instance is used for each connection.
 * TODO: check which class finally contains the processor
 */
public interface ApplicationProcessor {

	/**
	 * Processes a <body> element's content, possibly transforming it.
	 *
	 * @param message The message to be processed.
	 * @return The processed message
	 */
	byte[] processMessageBody(byte[] message);

	/**
	 * Processes a <body> element's content, possibly transforming it.
	 *
	 * @param message The message to be processed.
	 * @return The processed message.
	 */
	default String processMessageBody(String message) {
		return new String(processMessageBody(message.getBytes()));
	}
}
