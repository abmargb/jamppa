package org.jamppa.client;

import org.jamppa.XMPPBase;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

public class XMPPClient {

	private JID jid;
	private XMPPConnection connection;
	private String password;

	public XMPPClient(String jid, String password, String host, int port) {
		this.jid = new JID(jid);
		this.password = password;
		ConnectionConfiguration cc = new ConnectionConfiguration(
				host, port, this.jid.getDomain());
		this.connection = new XMPPConnection(cc);
	}
	
	public void connect() throws XMPPException {
		this.connection.connect();
	}
	
	public void register() throws XMPPException {
		this.connection.getAccountManager().createAccount(
				jid.getNode(), password);
	}
	
	public void login() throws XMPPException {
		this.connection.login(jid.getNode(), password);
	}
	
	public void process() throws ComponentException {
		new XMPPBase().process();
	}

	public void process(boolean block) {
		new XMPPBase().process(block);
	}
	
	public void send(Packet packet) {
		connection.sendPacket(packet);
	}
	
	public Packet syncSend(Packet packet) throws XMPPException {
		return SyncPacketSend.getReply(connection, packet);
	}
	
	public void on(PacketFilter filter, PacketListener callback) {
		connection.addPacketListener(callback, filter);
	}
}
