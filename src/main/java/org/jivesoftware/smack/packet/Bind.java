/**
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smack.packet;

import org.dom4j.Element;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmpp.packet.IQ;

/**
 * IQ packet used by Smack to bind a resource and to obtain the jid assigned by
 * the server. There are two ways to bind a resource. One is simply sending an
 * empty Bind packet where the server will assign a new resource for this
 * connection. The other option is to set a desired resource but the server may
 * return a modified version of the sent resource.
 * <p>
 * 
 * For more information refer to the following <a
 * href=http://www.xmpp.org/specs/rfc3920.html#bind>link</a>.
 * 
 * @author Gaston Dombiak
 */
public class Bind extends IQ {

    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-bind";
    public static final String ELEMENT = "bind";
    private Element bindEl;

    public Bind(Element el) {
        super(el);
        this.bindEl = element.element(ELEMENT);
    }

    public Bind() {
        super();
        setType(IQ.Type.set);
        this.bindEl = element.addElement(ELEMENT, NAMESPACE);
    }

    public String getResource() {
        return bindEl.elementText("resource");
    }

    public void setResource(String resource) {
        PacketParserUtils.updateText(bindEl, "resource", resource);
    }

    public String getJid() {
        return bindEl.elementText("jid");
    }

    public void setJid(String jid) {
        PacketParserUtils.updateText(bindEl, "jid", jid);
    }
}
