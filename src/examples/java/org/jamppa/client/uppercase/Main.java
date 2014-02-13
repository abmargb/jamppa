package org.jamppa.client.uppercase;

import org.jamppa.client.XMPPClient;
import org.jamppa.client.plugin.xep0077.XEP0077;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;

public class Main {

	public static void main(String[] args) throws XMPPException, ComponentException {
		XMPPClient client = new XMPPClient("client@test.com", 
				"password", "localhost", 5222);
		
		XEP0077 register = new XEP0077();
		client.registerPlugin(register);
		
		client.connect();
		try {
			register.createAccount("client@test.com", "password");
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
		client.login();
		client.process(false);
		
		IQ iq = new IQ(Type.get);
		iq.setTo("uppercase.test.com");
		iq.getElement()
				.addElement("query", "uppercase")
				.addElement("content")
				.setText("hello world");
		
		IQ response = (IQ) client.syncSend(iq);
		System.out.println(response.toXML());
		client.disconnect();
	}
}
