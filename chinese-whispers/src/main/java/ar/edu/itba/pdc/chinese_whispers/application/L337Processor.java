package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;

import java.util.logging.Logger;

/**
 * Created by jbellini on 29/10/16.
 * <p>
 * This {@link ApplicationProcessor} changes messages using L337.
 * This class implements singleton pattern.
 */
public class L337Processor implements ApplicationProcessor {


	/**
	 * Holds the system configurations.
	 */
	final private Configurations configurations;

	/**
	 * Holds the singleton instance.
	 */
	private static L337Processor singleton;

	/**
	 * Private constructor to implement singleton pattern.
	 */
	private L337Processor() {
		configurations = Configurations.getInstance();
	}


	public static L337Processor getInstance() {
		if (singleton == null) {
			singleton = new L337Processor();
		}
		return singleton;
	}


	@Override
	public void processMessageBody(StringBuilder stringBuilder, char[] message, boolean isInBodyTag) {
		if (stringBuilder == null || message == null) {
			LogHelper.getLogger(getClass()).warn("A null stringBuilder or message is being passed to l337Processor");
			return;
		}
		//Append l3373d or normal characters as appropriate
		for (char c : message) {
			switch (c) {
				case 'a':
					stringBuilder.append((isInBodyTag && configurations.isProcessL337())? "4": "a");
					break;
				case 'e':
					stringBuilder.append((isInBodyTag && configurations.isProcessL337())? "3": "e");
					break;
				case 'i':
					stringBuilder.append((isInBodyTag && configurations.isProcessL337())? "1": "i");
					break;
				case 'o':
					stringBuilder.append((isInBodyTag && configurations.isProcessL337())? "0": "o");
					break;
				case 'c':
					stringBuilder.append((isInBodyTag && configurations.isProcessL337())? "&lt;": "c");
					break;
				case '<':
					stringBuilder.append("&lt;");
					break;
				case '>':
					stringBuilder.append("&gt;");
					break;
				case '&':
					stringBuilder.append("&amp;");
					break;
				case '\'':
					stringBuilder.append("&apos;");
					break;
				case '\"':
					stringBuilder.append("&quot;");
					break;
				default:
					stringBuilder.append(c);
					break;
			}
		}
	}


	/**
	 * Calculates the final message length after processing the given {@code message}.
	 *
	 * @param message The message to be processed.
	 * @return The final length.
	 */
	private int calculateFinalMessageLength(byte[] message) {
		int count = 0;
		for (byte character : message) {
			if (character == 'C' || character == 'c') {
				count += 3; // To escape the '<' we add "&lt;"
			}
			count++;
		}
		return count;
	}
}
