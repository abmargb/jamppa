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
import org.jivesoftware.smack.util.StringUtils;
import org.xmpp.packet.IQ;

/**
 * Authentication packet, which can be used to login to a XMPP server as well as
 * discover login information from the server.
 */
public class Authentication extends IQ {

    private Element queryEl;

    /**
     * Create a new authentication packet. By default, the packet will be in
     * "set" mode in order to perform an actual authentication with the server.
     * In order to send a "get" request to get the available authentication
     * modes back from the server, change the type of the IQ packet to "get":
     * <p/>
     * <p>
     * <tt>setType(IQ.Type.GET);</tt>
     */
    public Authentication() {
        super();
        setType(IQ.Type.set);
        queryEl = element.addElement("query", "jabber:iq:auth");
        queryEl.addElement("username");
        queryEl.addElement("digest");
        queryEl.addElement("password");
        queryEl.addElement("resource");
    }

    public Authentication(Element el) {
        super(el);
        queryEl = element.element("query");
    }

    /**
     * Returns the username, or <tt>null</tt> if the username hasn't been sent.
     * 
     * @return the username.
     */
    public String getUsername() {
        return queryEl.elementText("username");
    }

    /**
     * Sets the username.
     * 
     * @param username
     *            the username.
     */
    public void setUsername(String username) {
        PacketParserUtils.updateText(queryEl, "username", username);
    }

    /**
     * Returns the plain text password or <tt>null</tt> if the password hasn't
     * been set.
     * 
     * @return the password.
     */
    public String getPassword() {
        return queryEl.elementText("password");
    }

    /**
     * Sets the plain text password.
     * 
     * @param password
     *            the password.
     */
    public void setPassword(String password) {
        PacketParserUtils.updateText(queryEl, "password", password);
    }

    /**
     * Returns the password digest or <tt>null</tt> if the digest hasn't been
     * set. Password digests offer a more secure alternative for authentication
     * compared to plain text. The digest is the hex-encoded SHA-1 hash of the
     * connection ID plus the user's password. If the digest and password are
     * set, digest authentication will be used. If only one value is set, the
     * respective authentication mode will be used.
     * 
     * @return the digest of the user's password.
     */
    public String getDigest() {
        return queryEl.elementText("digest");
    }

    /**
     * Sets the digest value using a connection ID and password. Password
     * digests offer a more secure alternative for authentication compared to
     * plain text. The digest is the hex-encoded SHA-1 hash of the connection ID
     * plus the user's password. If the digest and password are set, digest
     * authentication will be used. If only one value is set, the respective
     * authentication mode will be used.
     * 
     * @param connectionID
     *            the connection ID.
     * @param password
     *            the password.
     * @see org.jivesoftware.smack.Connection#getConnectionID()
     */
    public void setDigest(String connectionID, String password) {
        setDigest(StringUtils.hash(connectionID + password));
    }

    /**
     * Sets the digest value directly. Password digests offer a more secure
     * alternative for authentication compared to plain text. The digest is the
     * hex-encoded SHA-1 hash of the connection ID plus the user's password. If
     * the digest and password are set, digest authentication will be used. If
     * only one value is set, the respective authentication mode will be used.
     * 
     * @param digest
     *            the digest, which is the SHA-1 hash of the connection ID the
     *            user's password, encoded as hex.
     * @see org.jivesoftware.smack.Connection#getConnectionID()
     */
    public void setDigest(String digest) {
        PacketParserUtils.updateText(queryEl, "digest", digest);
    }

    /**
     * Returns the resource or <tt>null</tt> if the resource hasn't been set.
     * 
     * @return the resource.
     */
    public String getResource() {
        return queryEl.elementText("resource");
    }

    /**
     * Sets the resource.
     * 
     * @param resource
     *            the resource.
     */
    public void setResource(String resource) {
        PacketParserUtils.updateText(queryEl, "resource", resource);
    }
}
