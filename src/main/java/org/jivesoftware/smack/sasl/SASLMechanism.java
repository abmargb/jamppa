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

package org.jivesoftware.smack.sasl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.xmpp.packet.Packet;

/**
 * Base class for SASL mechanisms. Subclasses must implement these methods:
 * <ul>
 * <li>{@link #getName()} -- returns the common name of the SASL mechanism.</li>
 * </ul>
 * Subclasses will likely want to implement their own versions of these mthods:
 * <li>{@link #authenticate(String, String, String)} -- Initiate authentication
 * stanza using the deprecated method.</li> <li>
 * {@link #authenticate(String, String, CallbackHandler)} -- Initiate
 * authentication stanza using the CallbackHandler method.</li> <li>
 * {@link #challengeReceived(String)} -- Handle a challenge from the server.</li>
 * </ul>
 * 
 * Basic XMPP SASL authentication steps: 1. Client authentication
 * initialization, stanza sent to the server (Base64 encoded): <auth
 * xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='DIGEST-MD5'/> 2. Server
 * sends back to the client the challenge response (Base64 encoded) sample:
 * realm=<sasl server
 * realm>,nonce="OA6MG9tEQGm2hh",qop="auth",charset=utf-8,algorithm=md5-sess 3.
 * The client responds back to the server (Base 64 encoded): sample:
 * username=<userid>,realm=<sasl server realm from
 * above>,nonce="OA6MG9tEQGm2hh", cnonce="OA6MHXh6VqTrRk",nc=00000001,qop=auth,
 * digest-uri=<digesturi>, response=d388dad90d4bbd760a152321f2143af7,
 * charset=utf-8, authzid=<id> 4. The server evaluates if the user is present
 * and contained in the REALM if successful it sends: <response
 * xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/> (Base64 encoded) if not successful
 * it sends: sample: <challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>
 * cnNwYXV0aD1lYTQwZjYwMzM1YzQyN2I1NTI3Yjg0ZGJhYmNkZmZmZA== </challenge>
 * 
 * 
 * 
 * @author Jay Kline
 */
public abstract class SASLMechanism implements CallbackHandler {

    private SASLAuthentication saslAuthentication;
    protected SaslClient sc;
    protected String authenticationId;
    protected String password;
    protected String hostname;

    public SASLMechanism(SASLAuthentication saslAuthentication) {
        this.saslAuthentication = saslAuthentication;
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. Note that this
     * method of authentication is not recommended, since it is very inflexable.
     * Use {@link #authenticate(String, String, CallbackHandler)} whenever
     * possible.
     * 
     * Explanation of auth stanza:
     * 
     * The client authentication stanza needs to include the digest-uri of the
     * form: xmpp/serverName From RFC-2831: digest-uri = "digest-uri" "="
     * digest-uri-value digest-uri-value = serv-type "/" host [ "/" serv-name ]
     * 
     * digest-uri: Indicates the principal name of the service with which the
     * client wishes to connect, formed from the serv-type, host, and serv-name.
     * For example, the FTP service on "ftp.example.com" would have a
     * "digest-uri" value of "ftp/ftp.example.com"; the SMTP server from the
     * example above would have a "digest-uri" value of
     * "smtp/mail3.example.com/example.com".
     * 
     * host: The DNS host name or IP address for the service requested. The DNS
     * host name must be the fully-qualified canonical name of the host. The DNS
     * host name is the preferred form; see notes on server processing of the
     * digest-uri.
     * 
     * serv-name: Indicates the name of the service if it is replicated. The
     * service is considered to be replicated if the client's service-location
     * process involves resolution using standard DNS lookup operations, and if
     * these operations involve DNS records (such as SRV, or MX) which resolve
     * one DNS name into a set of other DNS names. In this case, the initial
     * name used by the client is the "serv-name", and the final name is the
     * "host" component. For example, the incoming mail service for
     * "example.com" may be replicated through the use of MX records stored in
     * the DNS, one of which points at an SMTP server called
     * "mail3.example.com"; it's "serv-name" would be "example.com", it's "host"
     * would be "mail3.example.com". If the service is not replicated, or the
     * serv-name is identical to the host, then the serv-name component MUST be
     * omitted
     * 
     * digest-uri verification is needed for ejabberd 2.0.3 and higher
     * 
     * @param username
     *            the username of the user being authenticated.
     * @param host
     *            the hostname where the user account resides.
     * @param serviceName
     *            the xmpp service location - used by the SASL client in
     *            digest-uri creation serviceName format is: host [ "/"
     *            serv-name ] as per RFC-2831
     * @param password
     *            the password for this account.
     * @throws IOException
     *             If a network error occurs while authenticating.
     * @throws XMPPException
     *             If a protocol error occurs or the user is not authenticated.
     */
    public void authenticate(String username, String host, String serviceName,
            String password) throws IOException, XMPPException {
        // Since we were not provided with a CallbackHandler, we will use our
        // own with the given
        // information

        // Set the authenticationID as the username, since they must be the same
        // in this case.
        this.authenticationId = username;
        this.password = password;
        this.hostname = host;

        String[] mechanisms = { getName() };
        Map<String, String> props = new HashMap<String, String>();
        sc = Sasl.createSaslClient(mechanisms, username, "xmpp", serviceName,
                props, this);
        authenticate();
    }

    /**
     * Same as {@link #authenticate(String, String, String, String)}, but with
     * the hostname used as the serviceName.
     * <p>
     * Kept for backward compatibility only.
     * 
     * @param username
     *            the username of the user being authenticated.
     * @param host
     *            the hostname where the user account resides.
     * @param password
     *            the password for this account.
     * @throws IOException
     *             If a network error occurs while authenticating.
     * @throws XMPPException
     *             If a protocol error occurs or the user is not authenticated.
     * @deprecated Please use
     *             {@link #authenticate(String, String, String, String)}
     *             instead.
     */
    public void authenticate(String username, String host, String password)
            throws IOException, XMPPException {
        authenticate(username, host, host, password);
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. The callback
     * handler will handle any additional information, such as the
     * authentication ID or realm, if it is needed.
     * 
     * @param username
     *            the username of the user being authenticated.
     * @param host
     *            the hostname where the user account resides.
     * @param cbh
     *            the CallbackHandler to obtain user information.
     * @throws IOException
     *             If a network error occures while authenticating.
     * @throws XMPPException
     *             If a protocol error occurs or the user is not authenticated.
     */
    public void authenticate(String username, String host, CallbackHandler cbh)
            throws IOException, XMPPException {
        String[] mechanisms = { getName() };
        Map<String, String> props = new HashMap<String, String>();
        sc = Sasl.createSaslClient(mechanisms, username, "xmpp", host, props,
                cbh);
        authenticate();
    }

    protected void authenticate() throws IOException, XMPPException {
        String authenticationText = null;
        try {
            if (sc.hasInitialResponse()) {
                byte[] response = sc.evaluateChallenge(new byte[0]);
                authenticationText = StringUtils.encodeBase64(response, false);
            }
        } catch (SaslException e) {
            throw new XMPPException("SASL authentication failed", e);
        }

        // Send the authentication to the server
        getSASLAuthentication().send(
                new AuthMechanism(getName(), authenticationText));
    }

    /**
     * The server is challenging the SASL mechanism for the stanza he just sent.
     * Send a response to the server's challenge.
     * 
     * @param challenge
     *            a base64 encoded string representing the challenge.
     * @throws IOException
     *             if an exception sending the response occurs.
     */
    public void challengeReceived(String challenge) throws IOException {
        byte response[];
        if (challenge != null) {
            response = sc
                    .evaluateChallenge(StringUtils.decodeBase64(challenge));
        } else {
            response = sc.evaluateChallenge(new byte[0]);
        }

        Packet responseStanza;
        if (response == null) {
            responseStanza = new Response();
        } else {
            responseStanza = new Response(StringUtils.encodeBase64(response,
                    false));
        }

        // Send the authentication to the server
        getSASLAuthentication().send(responseStanza);
    }

    /**
     * Returns the common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or
     * GSSAPI.
     * 
     * @return the common name of the SASL mechanism.
     */
    protected abstract String getName();

    protected SASLAuthentication getSASLAuthentication() {
        return saslAuthentication;
    }

    /**
     * 
     */
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback ncb = (NameCallback) callbacks[i];
                ncb.setName(authenticationId);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pcb = (PasswordCallback) callbacks[i];
                pcb.setPassword(password.toCharArray());
            } else if (callbacks[i] instanceof RealmCallback) {
                RealmCallback rcb = (RealmCallback) callbacks[i];
                // Retrieve the REALM from the challenge response that the
                // server returned when the client initiated the authentication
                // exchange. If this value is not null or empty, *this value*
                // has to be sent back to the server in the client's response
                // to the server's challenge
                String text = rcb.getDefaultText();
                // The SASL client (sc) created in smack uses rcb.getText when
                // creating the negotiatedRealm to send it back to the server
                // Make sure that this value matches the server's realm
                rcb.setText(text);
            } else if (callbacks[i] instanceof RealmChoiceCallback) {
                // unused
                // RealmChoiceCallback rccb = (RealmChoiceCallback)callbacks[i];
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }

    private static Element createAuthEl(String name, String authenticationText) {
        if (name == null) {
            throw new NullPointerException(
                    "SASL mechanism name shouldn't be null.");
        }
        DefaultElement authEl = new DefaultElement("auth",
                Namespace.get("urn:ietf:params:xml:ns:xmpp-sasl"));
        authEl.addAttribute("mechanism", name);
        if (authenticationText != null) {
            authEl.setText(authenticationText);
        }
        return authEl;
    }

    /**
     * Initiating SASL authentication by select a mechanism.
     */
    public class AuthMechanism extends Packet {

        public AuthMechanism(String name, String authenticationText) {
            super(createAuthEl(name, authenticationText));
        }

        public AuthMechanism(Element el) {
            super(el);
        }

        @Override
        public Packet createCopy() {
            return new AuthMechanism(element.createCopy());
        }
    }

    private static Element createChallengeEl(String data) {
        DefaultElement authEl = new DefaultElement("challenge",
                Namespace.get("urn:ietf:params:xml:ns:xmpp-sasl"));
        if (data != null) {
            authEl.setText(data);
        }
        return authEl;
    }

    /**
     * A SASL challenge stanza.
     */
    public static class Challenge extends Packet {

        public Challenge(String data) {
            super(createChallengeEl(data));
        }

        public Challenge(Element createCopy) {
            super(createCopy);
        }

        @Override
        public Packet createCopy() {
            return new Challenge(this.element.createCopy());
        }
    }

    private static Element createResponseEl(String authenticationText) {
        DefaultElement authEl = new DefaultElement("response",
                Namespace.get("urn:ietf:params:xml:ns:xmpp-sasl"));
        if (authenticationText != null && !authenticationText.isEmpty()) {
            authEl.setText(authenticationText);
        }
        return authEl;
    }

    /**
     * A SASL response stanza.
     */
    public class Response extends Packet {

        public Response() {
            this((String) null);
        }

        public Response(String authenticationText) {
            super(createResponseEl(authenticationText));
        }

        public Response(Element createCopy) {
            super(createCopy);
        }

        @Override
        public Packet createCopy() {
            return new Response(this.element.createCopy());
        }
    }

    /**
     * A SASL success stanza.
     */
    public static class Success extends Packet {

        public Success(Element element) {
            super(element);
        }

        @Override
        public Packet createCopy() {
            return new Success(this.getElement().createCopy());
        }
    }

    /**
     * A SASL failure stanza.
     */
    public static class Failure extends Packet {
        private String condition;

        public Failure(String condition) {
            super(createEl(condition));
            this.condition = condition;
        }

        @SuppressWarnings("unchecked")
        public Failure(Element element) {
            super(element);
            List<Element> conds = element.elements();
            if (!conds.isEmpty()) {
                this.condition = conds.iterator().next().getName();
            }
        }

        private static Element createEl(String condition) {
            Element el = new DefaultElement("failure",
                    Namespace.get("urn:ietf:params:xml:ns:xmpp-sasl"));
            if (condition != null && condition.trim().length() > 0) {
                el.addElement(condition);
            }
            return el;
        }

        /**
         * Get the SASL related error condition.
         * 
         * @return the SASL related error condition.
         */
        public String getCondition() {
            return condition;
        }

        @Override
        public Packet createCopy() {
            return new Failure(this.getElement().createCopy());
        }
    }
}
