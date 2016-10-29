package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.ApplicationProcessor;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by jbellini on 29/10/16.
 *
 * This {@link ApplicationProcessor} changes messages using L337.
 */
public class L337Processor implements ApplicationProcessor {


	@Override
	public byte[] processMessageBody(byte[] message) {
		ByteBuffer buff = ByteBuffer.allocate(message.length*4);
		for(byte b : message) {
			buff.put(transform(b));
		}
		return buff.array();
	}

	/**
	 * Transforms a single-byte character into a specified leet character. Note that 1 character may be transformed into
	 * multiple characters.
	 *
	 * @param character The single-byte character to transform.
	 * @return The transformed character(s).
	 */
	private byte[] transform(byte character) {
		switch (character) {
			case 'A':
			case 'a':
				return new byte[] {'4'};
			case 'E':
			case 'e':
				return new byte[] {'3'};
			case 'I':
			case 'i':
				return new byte[] {'1'};
			case 'O':
			case 'o':
				return new byte[] {'0'};
			case 'C':
			case 'c':
				//XML-escape the Byte
				return new byte[] {'&', 'l', 't', ';'};
			default:
				return new byte[] {character};
		}
	}
}
