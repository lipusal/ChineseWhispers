package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces;

/**
 * Created by jbellini on 30/10/16.
 * <p>
 * This interface defines a method to be executed when output was processed.
 * The implementor will consumeOutput output data.
 */
public interface OutputConsumer {


	/**
	 * Consumes the given message.
	 *
	 * @param message The message to be consumed.
	 */
	void consumeMessage(byte[] message);

	/**
	 * Consumes the given byte.
	 *
	 * @param b The byte to be consumed.
	 */
	default void consumeByte(byte b) {
		byte[] message = new byte[1];
		message[0] = b;
		consumeMessage(message);
	}

}
