package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;

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
			throw new IllegalArgumentException(); // TODO: Or should we just return
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
				default:
					stringBuilder.append(c);
					break;
			}
		}
	}

	@Override
	public byte[] processMessageBody(byte[] message) {
		if (message == null) {
			return null;
		}
		if (configurations.isProcessL337()) {
			return message; // Don't do anything if L337 is set off.
		}

		byte[] finalMessage = new byte[calculateFinalMessageLength(message)];

		for (int i = 0; i < message.length; i++) {
			switch (message[i]) {
				case 'A':
				case 'a':
					finalMessage[i] = '4';
					break;
				case 'E':
				case 'e':
					finalMessage[i] = '3';
					break;
				case 'I':
				case 'i':
					finalMessage[i] = '1';
					break;
				case 'O':
				case 'o':
					finalMessage[i] = '0';
					break;
				case 'C':
				case 'c':
					finalMessage[i++] = '&';
					finalMessage[i++] = 'l';
					finalMessage[i++] = 't';
					finalMessage[i] = ';';
					break;
                case '<':
                    finalMessage[i++] = '&';
                    finalMessage[i++] = 'l';
                    finalMessage[i++] = 't';
                    finalMessage[i] = ';';
                    break;
                case '>':
                    finalMessage[i++] = '&';
                    finalMessage[i++] = 'g';
                    finalMessage[i++] = 't';
                    finalMessage[i] = ';';
                    break;
                case '&':
                    finalMessage[i++] = '&';
                    finalMessage[i++] = 'a';
                    finalMessage[i++] = 'm';
                    finalMessage[i++] = 'p';
                    finalMessage[i] = ';';
                    break;
				default:
					finalMessage[i] = message[i];
					break;
			}
		}
		return finalMessage;
	}

	@Override
	public void processMessageBody(String message) {
		if (configurations.isProcessL337()) {
			return; // Don't do anything if L337 is set off.
		}
		message.replace('A', '4').replace('a', '4')
				.replace('E', '3').replace('e', '3')
				.replace('I', '1').replace('i', '1')
				.replace('O', '0').replace('o', '0')
				.replace("C", "&lt;").replace("c", "&lt;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");

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
