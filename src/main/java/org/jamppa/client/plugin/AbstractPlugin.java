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
    
    protected void setSupported(boolean isSupported) {
        this.isSupported = isSupported;
    }

    @Override
    public void shutdown() {
    	isSupported = false;
    }
    
    protected abstract boolean supports(Element featuresEl);

}
