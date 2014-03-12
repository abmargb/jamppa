/**
 * Copyright 2012 Florian Schmaus
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

package org.jivesoftware.smack.ping.packet;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class Ping extends IQ {

    public static final String NAMESPACE = "urn:xmpp:ping";
    public static final String ELEMENT = "ping";

    public Ping() {
        element.addElement(ELEMENT, NAMESPACE);
    }

    public Ping(String to) {
        this();
        setTo(to);
        setType(IQ.Type.get);
    }

    public Ping(Element doc) {
        super(doc);
    }

}
