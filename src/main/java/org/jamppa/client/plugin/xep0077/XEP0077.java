package org.jamppa.client.plugin.xep0077;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.client.plugin.AbstractPlugin;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

public class XEP0077 extends AbstractPlugin {

	private static final Logger LOGGER = Logger.getLogger(XEP0077.class);

	public Collection<String> getAccountAttributes() {
		try {
			Registration info = getRegistrationInfo();
			Map<String, String> attributes = info.getAttributes();
			if (attributes != null) {
				return Collections.unmodifiableSet(attributes.keySet());
			}
		} catch (XMPPException xe) {
			LOGGER.error("Error retrieving account attributes from server", xe);
		}
		return Collections.emptySet();
	}

	public String getAccountAttribute(String name) {
		try {
			Registration info = getRegistrationInfo();
			return info.getAttributes().get(name);
		} catch (XMPPException xe) {
			LOGGER.error("Error retrieving account attribute "
					+ name + " info from server", xe);
		}
		return null;
	}

	public void createAccount(String bareJid, String password)
			throws XMPPException {
		if (!isSupported()) {
			throw new XMPPException("Server does not support account creation.");
		}
		Map<String, String> attributes = new HashMap<String, String>();
		for (String attributeName : getAccountAttributes()) {
			attributes.put(attributeName, "");
		}
		createAccount(bareJid, password, attributes);
	}

	public void createAccount(String bareJid, String password,
			Map<String, String> attributes) throws XMPPException {
		if (!isSupported()) {
			throw new XMPPException("Server does not support account creation.");
		}
		Registration reg = new Registration();
		reg.setType(IQ.Type.set);
		XMPPConnection connection = getXMPPClient().getConnection();
		reg.setTo(connection.getServiceName());
		attributes.put("username", new JID(bareJid).getNode());
		attributes.put("password", password);
		reg.setAttributes(attributes);
		SyncPacketSend.getReply(connection, reg);
	}

	public void changePassword(String newPassword) throws XMPPException {
		Registration reg = new Registration();
		reg.setType(IQ.Type.set);
		XMPPConnection connection = getXMPPClient().getConnection();
		reg.setTo(connection.getServiceName());
		Map<String, String> map = new HashMap<String, String>();
		map.put("username", StringUtils.parseName(connection.getUser()));
		map.put("password", newPassword);
		reg.setAttributes(map);
		SyncPacketSend.getReply(connection, reg);
	}

	public void deleteAccount() throws XMPPException {
		XMPPConnection connection = getXMPPClient().getConnection();
		if (!connection.isAuthenticated()) {
			throw new IllegalStateException(
					"Must be logged in to delete a account.");
		}
		Registration reg = new Registration();
		reg.setType(IQ.Type.set);
		reg.setTo(connection.getServiceName());
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("remove", "");
		reg.setAttributes(attributes);
		SyncPacketSend.getReply(connection, reg);
	}
	
	private synchronized Registration getRegistrationInfo()
			throws XMPPException {
		XMPPConnection connection = getXMPPClient().getConnection();
		Registration reg = new Registration();
		reg.setTo(connection.getServiceName());
		return (Registration) SyncPacketSend.getReply(connection, reg);
	}

	@Override
	public Packet parse(Element el) {
		if (!"iq".equals(el.getName())) {
			return null;
		}
		Element query = el.element("query");
		if (query != null && "jabber:iq:register".equals(
				query.getNamespaceURI())) {
			return new Registration(el);
		}
		return null;
	}

	@Override
	public boolean supports(Element featuresEl) {
		Element registerEl = featuresEl.element("register");
		if (registerEl != null && registerEl.getNamespace().getURI().equals(
				"http://jabber.org/features/iq-register")) {
			return true;
        }
		return false;
	}
}
