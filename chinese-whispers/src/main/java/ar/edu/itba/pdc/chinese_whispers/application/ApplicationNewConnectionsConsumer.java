package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.NewConnectionsConsumer;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jbellini on 29/10/16.
 * <p>
 * This {@link NewConnectionsConsumer} stores jids in a set.
 */
public class ApplicationNewConnectionsConsumer implements NewConnectionsConsumer {

	private final Set<Object> jids;

	public ApplicationNewConnectionsConsumer() {
		this.jids = new HashSet<>();
	}


	@Override
	public void consumeNewConnection(Object jid) {
		jids.add(jid);
	}
}
