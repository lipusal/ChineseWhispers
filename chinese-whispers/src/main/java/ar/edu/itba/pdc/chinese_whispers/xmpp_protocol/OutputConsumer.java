package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol;

/**
 * Created by jbellini on 30/10/16.
 *
 * This interface defines a method to be executed when output was processed.
 * The implementor will consumeOutput output data.
 */
public interface OutputConsumer {


	void consumeMessage(byte[] message);

	default void consumeByte(byte b) {
		byte[] message = new byte[1];
		message[0] = b;
		consumeMessage(message);
	}

}
