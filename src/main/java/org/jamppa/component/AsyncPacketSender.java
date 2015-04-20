package org.jamppa.component;

import org.jamppa.component.PacketCallback;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.Packet;

public interface AsyncPacketSender extends PacketSender {
	
	public void addPacketCallback(Packet packet, PacketCallback packetCallback);

}
