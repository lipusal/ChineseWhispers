package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.ApplicationProcessor;

/**
 * Created by jbellini on 29/10/16.
 *
 * This {@link ApplicationProcessor} changes messages using L337.
 */
public class L337Processor implements ApplicationProcessor {


	@Override
	public void processMessageBody(byte[] message) {

		if (message != null) {
			for (int i = 0; i < message.length; i++) {
				switch (message[i]) {
					case 'A':
					case 'a':
						message[i] = '4';
						break;
					case 'E':
					case 'e':
						message[i] = '3';
						break;
					case 'I':
					case 'i':
						message[i] = '1';
						break;
					case 'O':
					case 'o':
						message[i] = '0';
						break;
					case 'C':
					case 'c':
						message[i] = '<';
						break;
				}
			}
		}

	}

	@Override
	public void processMessageBody(String message) {
		message.replace('A', '4').replace('a', '4')
				.replace('E', '3').replace('e', '3')
				.replace('I', '1').replace('i', '1')
				.replace('O', '0').replace('o', '0')
				.replace('C', '<').replace('c', '<');

	}
}
