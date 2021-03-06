package org.jamppa.component.uppercase;

import org.jamppa.component.XMPPComponent;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.IQ;

public class Main {

    public static void main(String[] args) throws ComponentException {
        UppercaseComponent component = new UppercaseComponent(
                "uppercase.test.com", "password", "localhost", 5347);
        component.connect();
        component.process(true);
    }

    private static class UppercaseComponent extends XMPPComponent {

        public UppercaseComponent(String jid, String password, String server,
                int port) {
            super(jid, password, server, port);
            addHandlers();
        }

        private void addHandlers() {
            addGetHandler(new AbstractQueryHandler("uppercase") {
                @Override
                public IQ handle(IQ query) {
                    IQ resultIQ = IQ.createResultIQ(query);
                    String originalContent = query.getElement()
                            .element("query").elementText("content");
                    resultIQ.getElement().addElement("query", getNamespace())
                            .addElement("content")
                            .setText(originalContent.toUpperCase());
                    return resultIQ;
                }
            });
        }
    }
}
