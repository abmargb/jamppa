package org.jamppa.component.handler;

import org.xmpp.packet.IQ;

public abstract class AsyncQueryHandler extends AbstractQueryHandler {

	public AsyncQueryHandler(String namespace) {
		super(namespace);
	}

	@Override
	public final IQ handle(IQ query) {
		handleAsync(query);
		return null;
	}
	
	protected abstract void handleAsync(IQ query);

}
