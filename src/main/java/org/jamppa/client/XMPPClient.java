package org.jamppa.client;

import org.jamppa.XMPPBase;
import org.jamppa.client.plugin.Plugin;
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
	private XMPPBase base = new XMPPBase();
	
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
	
	public void registerPlugin(Plugin plugin) throws XMPPException {
		plugin.create(this);
		this.connection.registerPlugin(plugin);
	}
	
	public XMPPConnection getConnection() {
		return connection;
	}
	
	public void login() throws XMPPException {
		this.connection.login(jid.getNode(), password);
	}
	
	public void process() throws ComponentException {
		base.process();
	}

	public void process(boolean block) {
		base.process(block);
	}
	
	public JID getJid() {
		return jid;
	}
	
	public String getPassword() {
		return password;
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

	public void disconnect() {
		connection.disconnect();
		base.disconnect();
	}
}
