package org.jamppa.component.handler;

import org.junit.Test;
import org.xmpp.packet.IQ;

public class AbstractQueryHandlerTest {

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorNullNamespace() {
		new AbstractQueryHandler(null) {
			@Override
			public IQ handle(IQ query) {
				return null;
			}
		};
	}
	
}
