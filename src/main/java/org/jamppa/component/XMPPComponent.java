/*
 * Copyright 2012 buddycloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jamppa.component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jamppa.XMPPBase;
import org.jamppa.component.handler.QueryHandler;
import org.jamppa.component.utils.XMPPUtils;
import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.AbstractComponent;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

/**
 * @author Abmar
 * 
 */
public class XMPPComponent extends AbstractComponent implements AsyncPacketSender {

    private static final Logger LOGGER = Logger.getLogger(XMPPComponent.class);

    private final Map<String, QueryHandler> queryGetHandlers = new HashMap<String, QueryHandler>();
    private final Map<String, QueryHandler> querySetHandlers = new HashMap<String, QueryHandler>();

    private String description;
    private String name;
    private String discoInfoIdentityCategory;
    private String discoInfoIdentityCategoryType;

    private Map<String, PacketCallback> packetCallbacks = new HashMap<String, PacketCallback>();

    private String jid;
    private String password;
    private String server;
    private int port;

    private ExternalComponentManager componentManager;

    /**
     * @param configuration
     */
    public XMPPComponent(String jid, String password, String server, int port) {
    	super(20, 1000, false);
        this.jid = jid;
        this.password = password;
        this.server = server;
        this.port = port;
    }

    public void process() {
        new XMPPBase().process();
    }

    public void process(boolean block) {
        new XMPPBase().process(block);
    }

    public void addSetHandler(QueryHandler queryHandler) {
        queryHandler.setPacketSender(this);
        querySetHandlers.put(queryHandler.getNamespace(), queryHandler);
    }

    public void addGetHandler(QueryHandler queryHandler) {
        queryHandler.setPacketSender(this);
        queryGetHandlers.put(queryHandler.getNamespace(), queryHandler);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xmpp.component.AbstractComponent#handleIQSet(org.xmpp.packet.IQ)
     */
    @Override
    protected IQ handleIQSet(IQ iq) throws Exception {
        return handle(iq, querySetHandlers);
    }

    @Override
    protected IQ handleIQGet(IQ iq) throws Exception {
        return handle(iq, queryGetHandlers);
    }

    private IQ handle(IQ iq, Map<String, QueryHandler> handlers) {
        Element queryElement = iq.getElement().element("query");
        if (queryElement == null) {
            return XMPPUtils.error(iq, "IQ does not contain query element.",
                    LOGGER);
        }

        Namespace namespace = queryElement.getNamespace();

        QueryHandler queryHandler = handlers.get(namespace.getURI());
        if (queryHandler == null) {
            return XMPPUtils.error(iq, "QueryHandler not found for namespace: "
                    + namespace, LOGGER);
        }

        return queryHandler.handle(iq);
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xmpp.component.AbstractComponent#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xmpp.component.AbstractComponent#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected String discoInfoIdentityCategory() {
        return discoInfoIdentityCategory;
    }

    /**
     * @param discoInfoIdentityCategory
     *            the discoInfoIdentityCategory to set
     */
    public void setDiscoInfoIdentityCategory(String discoInfoIdentityCategory) {
        this.discoInfoIdentityCategory = discoInfoIdentityCategory;
    }

    @Override
    protected String discoInfoIdentityCategoryType() {
        return discoInfoIdentityCategoryType;
    }

    /**
     * @param discoInfoIdentityCategoryType
     *            the discoInfoIdentityCategoryType to set
     */
    public void setDiscoInfoIdentityCategoryType(
            String discoInfoIdentityCategoryType) {
        this.discoInfoIdentityCategoryType = discoInfoIdentityCategoryType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * uk.co.rappidcars.PacketSender#sendPacket(org.jivesoftware.smack.packet
     * .Packet)
     */
    @Override
    public void sendPacket(Packet packet) {
        send(packet);
    }

    public void connect() throws ComponentException {
        LOGGER.debug("Initializing XMPP component...");

        this.componentManager = new ExternalComponentManager(server, port);
        componentManager.setSecretKey(jid, password);

        try {
            componentManager.addComponent(jid, this);
        } catch (ComponentException e) {
            LOGGER.fatal("Component could not be started.", e);
            throw e;
        }

        LOGGER.debug("XMPP component initialized.");
    }

    public void disconnect() throws ComponentException {
        componentManager.removeComponent(jid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xmpp.component.AbstractComponent#handleIQResult(org.xmpp.packet.IQ)
     */
    @Override
    protected void handleIQResult(IQ iq) {
    	String packetCallbackId = iq.getID() + "@"
                + iq.getFrom().toBareJID();
		PacketCallback callback = packetCallbacks.get(packetCallbackId);
        if (callback != null) {
            callback.handle(iq);
            packetCallbacks.remove(packetCallbackId);
        }
    }
    
    @Override
    protected void handleIQError(IQ iq) {
    	handleIQResult(iq);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jamppa.component.PacketSender#syncSendPacket(org.xmpp.packet.Packet)
     */
    @Override
    public Packet syncSendPacket(Packet packet) {
        final BlockingQueue<Packet> queue = new ArrayBlockingQueue<Packet>(1);
        packetCallbacks.put(packet.getID() + "@" + packet.getTo().toBareJID(),
                new PacketCallback() {
                    @Override
                    public void handle(Packet packet) {
                        queue.add(packet);
                    }
                });
        send(packet);
        try {
            return queue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            packetCallbacks.remove(packet.getID());
        }
    }
    
    @Override
	public void addPacketCallback(Packet packet, PacketCallback packetCallback) {
		packetCallbacks.put(packet.getID() + "@" + packet.getTo().toBareJID(),
				packetCallback);
	}
}
