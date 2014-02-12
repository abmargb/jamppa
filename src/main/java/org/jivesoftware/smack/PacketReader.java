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

package org.jivesoftware.smack;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Element;
import org.dom4j.io.XPPPacketReader;
import org.jivesoftware.smack.Connection.ListenerWrapper;
import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.ping.packet.Ping;
import org.jivesoftware.smack.sasl.SASLMechanism.Challenge;
import org.jivesoftware.smack.sasl.SASLMechanism.Failure;
import org.jivesoftware.smack.sasl.SASLMechanism.Success;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Roster;

/**
 * Listens for XML traffic from the XMPP server and parses it into packet objects.
 * The packet reader also invokes all packet listeners and collectors.<p>
 *
 * @see Connection#createPacketCollector
 * @see Connection#addPacketListener
 * @author Matt Tucker
 */
class PacketReader {

    private static Logger log = Logger.getLogger(PacketReader.class.getName());
    
    private Thread readerThread;
    private ExecutorService listenerExecutor;

    private XMPPConnection connection;
    private XPPPacketReader innerReader;
    volatile boolean done;

    private String connectionID = null;

    protected PacketReader(final XMPPConnection connection) {
        this.connection = connection;
        this.init();
    }

    /**
     * Initializes the reader in order to be used. The reader is initialized during the
     * first connection and when reconnecting due to an abruptly disconnection.
     */
    protected void init() {
        done = false;
        connectionID = null;

        readerThread = new Thread() {
            public void run() {
                parsePackets(this);
            }
        };
        readerThread.setName("Smack Packet Reader (" + connection.connectionCounterValue + ")");
        readerThread.setDaemon(true);

        // Create an executor to deliver incoming packets to listeners. We'll use a single
        // thread with an unbounded queue.
        listenerExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {

            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable,
                        "Smack Listener Processor (" + connection.connectionCounterValue + ")");
                thread.setDaemon(true);
                return thread;
            }
        });

        resetParser();
    }

    /**
     * Starts the packet reader thread and returns once a connection to the server
     * has been established. A connection will be attempted for a maximum of five
     * seconds. An XMPPException will be thrown if the connection fails.
     *
     * @throws XMPPException if the server fails to send an opening stream back
     *      for more than five seconds.
     */
    synchronized public void startup() throws XMPPException {
        readerThread.start();
        // Wait for stream tag before returning. We'll wait a couple of seconds before
        // giving up and throwing an error.
        try {
            // A waiting thread may be woken up before the wait time or a notify
            // (although this is a rare thing). Therefore, we continue waiting
            // until either a connectionID has been set (and hence a notify was
            // made) or the total wait time has elapsed.
            int waitTime = SmackConfiguration.getPacketReplyTimeout();
            wait(3 * waitTime);
        }
        catch (InterruptedException ie) {
            // Ignore.
        }
        if (connectionID == null) {
            throw new XMPPException("Connection failed. No response from server.");
        }
        else {
            connection.connectionID = connectionID;
        }
    }

    /**
     * Shuts the packet reader down.
     */
    public void shutdown() {
        // Notify connection listeners of the connection closing if done hasn't already been set.
        if (!done) {
            for (ConnectionListener listener : connection.getConnectionListeners()) {
                try {
                    listener.connectionClosed();
                }
                catch (Exception e) {
                    // Catch and print any exception so we can recover
                    // from a faulty listener and finish the shutdown process
                    log.log(Level.SEVERE, "Error in listener while closing connection", e);
                }
            }
        }
        done = true;

        // Shut down the listener executor.
        listenerExecutor.shutdown();
    }

    /**
     * Resets the parser using the latest connection's reader. Reseting the parser is necessary
     * when the plain connection has been secured or when a new opening stream element is going
     * to be sent by the server.
     */
    private void resetParser() {
        try {
            innerReader = new XPPPacketReader();
            innerReader.setXPPFactory(XmlPullParserFactory.newInstance());
            innerReader.getXPPParser().setInput(connection.reader);
            XmlPullParser xpp = innerReader.getXPPParser();
            for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG;) {
                eventType = xpp.next();
            }
            parseStreamStart(xpp);
        } catch (Exception xppe) {
            log.log(Level.WARNING, "Error while resetting parser", xppe);
        }
    }

	private void parseStreamStart(XmlPullParser parser) {
		if ("jabber:client".equals(parser.getNamespace(null))) {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).equals("id")) {
					connectionID = parser.getAttributeValue(i);
					if (!"1.0".equals(parser.getAttributeValue("", "version"))) {
						releaseConnectionIDLock();
					}
				} else if (parser.getAttributeName(i).equals("from")) {
					connection.config.setServiceName(parser
							.getAttributeValue(i));
				}
			}
		}
	}

	/**
     * Parse top-level packets in order to process them further.
     *
     * @param thread the thread that is being used by the reader to parse incoming packets.
     */
    private void parsePackets(Thread thread) {
        try {
			while (!done) {
				Element doc = innerReader.parseDocument().getRootElement();
				if (doc == null) {
					connection.disconnect();
					continue;
				}
				Packet packet = null;
				String tag = doc.getName();
				if ("message".equals(tag)) {
					packet = new Message(doc);
				} else if ("presence".equals(tag)) {
					packet = new Presence(doc);
				} else if ("iq".equals(tag)) {
					packet = parseIQ(doc);
				} else if ("error".equals(tag)) {
					throw new XMPPException(PacketParserUtils.parseStreamError(doc));
				} else if ("features".equals(tag)) {
					parseFeatures(doc);
				} else if ("proceed".equals(tag)) {
					connection.proceedTLSReceived();
					resetParser();
				} else if ("failure".equals(tag)) {
					parseFailure(doc);
				} else if ("challenge".equals(tag)) {
					parseChallenge(doc);
				} else if ("success".equals(tag)) {
					parseSuccess(doc);
				} else if ("compressed".equals(tag)) {
                    connection.startStreamCompression();
                    resetParser();
				} else {
					throw new XmlPullParserException(
							"Unknown packet type was read: " + tag);
				}
				if (packet != null) {
					processPacket(packet);
				}
			}
		} catch (Exception e) {
            if (!(done || connection.isSocketClosed())) {
                connection.notifyConnectionError(e);
            }
        }
    }

	private void parseChallenge(Element doc) throws IOException {
		String challengeData = doc.getText();
		processPacket(new Challenge(doc));
		connection.getSASLAuthentication().challengeReceived(challengeData);
	}

	private void parseSuccess(Element doc) throws IOException {
		processPacket(new Success(doc));
		connection.packetWriter.openStream();
		resetParser();
		connection.getSASLAuthentication().authenticated();
	}
    
	private void parseFailure(Element failureEl) throws Exception {
        if ("urn:ietf:params:xml:ns:xmpp-tls".equals(failureEl.getNamespace())) {
            throw new Exception("TLS negotiation has failed");
        } else if ("http://jabber.org/protocol/compress".equals(failureEl.getNamespace())) {
            connection.streamCompressionDenied();
        } else {
            final Failure failure = new Failure(failureEl);
            processPacket(failure);
            connection.getSASLAuthentication().authenticationFailed(failure.getCondition());
        }
	}

	private IQ parseIQ(Element doc) {
		Element pingEl = doc.element(Ping.ELEMENT);
		if (pingEl != null && pingEl.getNamespace().equals(Ping.NAMESPACE)) {
			return new Ping(doc);
		}
		
		Element bindEl = doc.element(Bind.ELEMENT);
		if (bindEl != null && bindEl.getNamespace().equals(Bind.NAMESPACE)) {
			return new Bind(doc);
		}
		
		Element query = doc.element("query");
		if (query != null) {
			if ("jabber:iq:roster".equals(query.getNamespaceURI())) {
				return new Roster(doc);
			}
			if ("jabber:iq:auth".equals(query.getNamespaceURI())) {
				return new Authentication(doc);
			}
			if ("jabber:iq:register".equals(query.getNamespaceURI())) {
				return new Registration(doc);
			}
		}
		
		return new IQ(doc);
	}

    /**
     * Releases the connection ID lock so that the thread that was waiting can resume. The
     * lock will be released when one of the following three conditions is met:<p>
     *
     * 1) An opening stream was sent from a non XMPP 1.0 compliant server
     * 2) Stream features were received from an XMPP 1.0 compliant server that does not support TLS
     * 3) TLS negotiation was successful
     *
     */
    synchronized private void releaseConnectionIDLock() {
        notify();
    }

    /**
     * Processes a packet after it's been fully parsed by looping through the installed
     * packet collectors and listeners and letting them examine the packet to see if
     * they are a match with the filter.
     *
     * @param packet the packet to process.
     */
    private void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }

        // Loop through all collectors and notify the appropriate ones.
        for (PacketCollector collector: connection.getPacketCollectors()) {
            collector.processPacket(packet);
        }

        // Deliver the incoming packet to listeners.
        listenerExecutor.submit(new ListenerNotification(packet));
    }

    private void parseFeatures(Element doc) throws Exception {
    	boolean startTLSReceived = false;
        Element startTLSEl = doc.element("starttls");
        if (startTLSEl != null && startTLSEl.getNamespace().equals(
        		"urn:ietf:params:xml:ns:xmpp-tls")) {
        	startTLSReceived = true;
        	connection.startTLSReceived(startTLSEl.element("required") != null);
        }
        
        Element mechanismsEl = doc.element("mechanisms");
        if (mechanismsEl != null && mechanismsEl.getNamespace().equals(
        		"urn:ietf:params:xml:ns:xmpp-sasl")) {
        	connection.getSASLAuthentication().setAvailableSASLMethods(
        			PacketParserUtils.parseMechanisms(mechanismsEl));
        }
        
        Element bindEl = doc.element("bind");
		if (bindEl != null && bindEl.getNamespace().equals(
				"urn:ietf:params:xml:ns:xmpp-bind")) {
			connection.getSASLAuthentication().bindingRequired();
        }
		
		Element cEl = doc.element("c");
		if (cEl != null && cEl.getNamespace().equals(
				"http://jabber.org/protocol/caps")) {
			String node = doc.attributeValue("node");
            String ver = doc.attributeValue("ver");
            if (ver != null && node != null) {
                String capsNode = node + "#" + ver;
                connection.setServiceCapsNode(capsNode);
            }
		}
		
		Element sessionEl = doc.element("session");
		if (sessionEl != null && sessionEl.getNamespace().equals(
				"urn:ietf:params:xml:ns:xmpp-session")) {
			connection.getSASLAuthentication().sessionsSupported();
        }
		
		Element verEl = doc.element("ver");
		if (verEl != null && verEl.getNamespace().equals(
				"urn:xmpp:features:rosterver")) {
            connection.setRosterVersioningSupported();
        }
		
		Element compressionEl = doc.element("compression");
		if (compressionEl != null && compressionEl.getNamespace().equals(
				"http://jabber.org/features/compress")) {
			connection.setAvailableCompressionMethods(
					PacketParserUtils.parseCompressionMethods(compressionEl));
        }
		
		Element registerEl = doc.element("register");
		if (registerEl != null && registerEl.getNamespace().equals(
				"http://jabber.org/features/iq-register")) {
			connection.getAccountManager().setSupportsAccountCreation(true);
        }
		
		if (!connection.isSecureConnection()) {
			if (!startTLSReceived && connection.getConfiguration().getSecurityMode() == 
					ConnectionConfiguration.SecurityMode.required) {
				throw new XMPPException(
						"Server does not support security (TLS), "
								+ "but security required by connection configuration.",
						new XMPPError(XMPPError.Condition.forbidden));
			}
		}

		if (!startTLSReceived || connection.getConfiguration().getSecurityMode() == 
				ConnectionConfiguration.SecurityMode.disabled) {
			releaseConnectionIDLock();
		}
    }

    /**
     * A runnable to notify all listeners of a packet.
     */
    private class ListenerNotification implements Runnable {

        private Packet packet;

        public ListenerNotification(Packet packet) {
            this.packet = packet;
        }

        public void run() {
            for (ListenerWrapper listenerWrapper : connection.recvListeners.values()) {
                try {
                    listenerWrapper.notifyListener(packet);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Exception in packet listener", e);
                }
            }
        }
    }
}
