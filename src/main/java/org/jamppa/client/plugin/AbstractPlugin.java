package org.jamppa.client.plugin;

import org.dom4j.Element;
import org.jamppa.client.XMPPClient;

public abstract class AbstractPlugin implements Plugin {

    private boolean isSupported;
    private XMPPClient xmppClient;

    @Override
    public void create(XMPPClient xmppClient) {
        this.xmppClient = xmppClient;
    }

    public XMPPClient getXMPPClient() {
        return xmppClient;
    }

    @Override
    public void checkSupport(Element featuresEl) {
        this.isSupported = supports(featuresEl);
    }

    protected boolean isSupported() {
        return isSupported;
    }

    protected abstract boolean supports(Element featuresEl);

}
