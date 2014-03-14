package org.jamppa.client.plugin;

import org.dom4j.Element;
import org.jamppa.client.XMPPClient;
import org.xmpp.packet.Packet;

public interface Plugin {

    public void create(XMPPClient xmppClient);

    public void checkSupport(Element featuresEl);

    public Packet parse(Element el);
    
    public void shutdown();

}
