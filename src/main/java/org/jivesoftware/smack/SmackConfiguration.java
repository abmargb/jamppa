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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.parsing.ExceptionThrowingCallback;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;

/**
 * Represents the configuration of Smack. The configuration is used for:
 * <ul>
 *      <li> Initializing classes by loading them at start-up.
 *      <li> Getting the current Smack version.
 *      <li> Getting and setting global library behavior, such as the period of time
 *          to wait for replies to packets from the server. Note: setting these values
 *          via the API will override settings in the configuration file.
 * </ul>
 *
 * Configuration settings are stored in META-INF/smack-config.xml (typically inside the
 * smack.jar file).
 * 
 * @author Gaston Dombiak
 */
public final class SmackConfiguration {
    private static final String SMACK_VERSION = "3.4.0";
    
    private static int packetReplyTimeout = 5000;
    private static int keepAliveInterval = 30000;
    private static List<String> defaultMechs = new ArrayList<String>();

    private static boolean localSocks5ProxyEnabled = true;
    private static int localSocks5ProxyPort = 7777;
    private static int packetCollectorSize = 5000;
    
    private static boolean initialized = false;

    /**
     * The default parsing exception callback is {@link ExceptionThrowingCallback} which will
     * throw an exception and therefore disconnect the active connection.
     */
    private static ParsingExceptionCallback defaultCallback = new ExceptionThrowingCallback();

    /**
     * This automatically enables EntityCaps for new connections if it is set to true
     */
    private static boolean autoEnableEntityCaps = true;

    private SmackConfiguration() {
    }

    /**
     * Loads the configuration from the smack-config.xml file.<p>
     * 
     * So far this means that:
     * 1) a set of classes will be loaded in order to execute their static init block
     * 2) retrieve and set the current Smack release
     */
    
    /**
     * Returns the Smack version information, eg "1.3.0".
     * 
     * @return the Smack version information.
     */
    public static String getVersion() {
        return SMACK_VERSION;
    }

    /**
     * Returns the number of milliseconds to wait for a response from
     * the server. The default value is 5000 ms.
     * 
     * @return the milliseconds to wait for a response from the server
     */
    public static int getPacketReplyTimeout() {
        initialize();
        
        // The timeout value must be greater than 0 otherwise we will answer the default value
        if (packetReplyTimeout <= 0) {
            packetReplyTimeout = 5000;
        }
        return packetReplyTimeout;
    }

    /**
     * Sets the number of milliseconds to wait for a response from
     * the server.
     * 
     * @param timeout the milliseconds to wait for a response from the server
     */
    public static void setPacketReplyTimeout(int timeout) {
        initialize();

        if (timeout <= 0) {
            throw new IllegalArgumentException();
        }
        packetReplyTimeout = timeout;
    }

    /**
     * Returns the number of milleseconds delay between sending keep-alive
     * requests to the server. The default value is 30000 ms. A value of -1
     * mean no keep-alive requests will be sent to the server.
     *
     * @return the milliseconds to wait between keep-alive requests, or -1 if
     *      no keep-alive should be sent.
     */
    public static int getKeepAliveInterval() {
        initialize();
        return keepAliveInterval;
    }

    /**
     * Sets the number of milleseconds delay between sending keep-alive
     * requests to the server. The default value is 30000 ms. A value of -1
     * mean no keep-alive requests will be sent to the server.
     *
     * @param interval the milliseconds to wait between keep-alive requests,
     *      or -1 if no keep-alive should be sent.
     */
    public static void setKeepAliveInterval(int interval) {
        initialize();
        keepAliveInterval = interval;
    }

    /**
     * Gets the default max size of a packet collector before it will delete 
     * the older packets.
     * 
     * @return The number of packets to queue before deleting older packets.
     */
    public static int getPacketCollectorSize() {
        initialize();
    	return packetCollectorSize;
    }

    /**
     * Sets the default max size of a packet collector before it will delete 
     * the older packets.
     * 
     * @param The number of packets to queue before deleting older packets.
     */
    public static void setPacketCollectorSize(int collectorSize) {
        initialize();
    	packetCollectorSize = collectorSize;
    }
    
    /**
     * Add a SASL mechanism to the list to be used.
     *
     * @param mech the SASL mechanism to be added
     */
    public static void addSaslMech(String mech) {
        initialize();

        if(! defaultMechs.contains(mech) ) {
            defaultMechs.add(mech);
        }
    }

   /**
     * Add a Collection of SASL mechanisms to the list to be used.
     *
     * @param mechs the Collection of SASL mechanisms to be added
     */
    public static void addSaslMechs(Collection<String> mechs) {
        initialize();

        for(String mech : mechs) {
            addSaslMech(mech);
        }
    }

    /**
     * Remove a SASL mechanism from the list to be used.
     *
     * @param mech the SASL mechanism to be removed
     */
    public static void removeSaslMech(String mech) {
        initialize();
        defaultMechs.remove(mech);
    }

   /**
     * Remove a Collection of SASL mechanisms to the list to be used.
     *
     * @param mechs the Collection of SASL mechanisms to be removed
     */
    public static void removeSaslMechs(Collection<String> mechs) {
        initialize();
        defaultMechs.removeAll(mechs);
    }

    /**
     * Returns the list of SASL mechanisms to be used. If a SASL mechanism is
     * listed here it does not guarantee it will be used. The server may not
     * support it, or it may not be implemented.
     *
     * @return the list of SASL mechanisms to be used.
     */
    public static List<String> getSaslMechs() {
        return Collections.unmodifiableList(defaultMechs);
    }

    /**
     * Returns true if the local Socks5 proxy should be started. Default is true.
     * 
     * @return if the local Socks5 proxy should be started
     */
    public static boolean isLocalSocks5ProxyEnabled() {
        initialize();
        return localSocks5ProxyEnabled;
    }

    /**
     * Sets if the local Socks5 proxy should be started. Default is true.
     * 
     * @param localSocks5ProxyEnabled if the local Socks5 proxy should be started
     */
    public static void setLocalSocks5ProxyEnabled(boolean localSocks5ProxyEnabled) {
        initialize();
        SmackConfiguration.localSocks5ProxyEnabled = localSocks5ProxyEnabled;
    }

    /**
     * Return the port of the local Socks5 proxy. Default is 7777.
     * 
     * @return the port of the local Socks5 proxy
     */
    public static int getLocalSocks5ProxyPort() {
        initialize();
        return localSocks5ProxyPort;
    }

    /**
     * Sets the port of the local Socks5 proxy. Default is 7777. If you set the port to a negative
     * value Smack tries the absolute value and all following until it finds an open port.
     * 
     * @param localSocks5ProxyPort the port of the local Socks5 proxy to set
     */
    public static void setLocalSocks5ProxyPort(int localSocks5ProxyPort) {
        initialize();
        SmackConfiguration.localSocks5ProxyPort = localSocks5ProxyPort;
    }

    /**
     * Check if Entity Caps are enabled as default for every new connection
     * @return
     */
    public static boolean autoEnableEntityCaps() {
        initialize();
        return autoEnableEntityCaps;
    }

    /**
     * Set if Entity Caps are enabled or disabled for every new connection
     * 
     * @param true if Entity Caps should be auto enabled, false if not
     */
    public static void setAutoEnableEntityCaps(boolean b) {
        initialize();
        autoEnableEntityCaps = b;
    }

    /**
     * Set the default parsing exception callback for all newly created connections
     *
     * @param callback
     * @see ParsingExceptionCallback
     */
    public static void setDefaultParsingExceptionCallback(ParsingExceptionCallback callback) {
        initialize();
        defaultCallback = callback;
    }

    /**
     * Returns the default parsing exception callback
     * 
     * @return the default parsing exception callback
     * @see ParsingExceptionCallback
     */
    public static ParsingExceptionCallback getDefaultParsingExceptionCallback() {
        initialize();
        return defaultCallback;
    }

    /*
     * Order of precedence for config file is VM arg, setConfigXXX methods and embedded default file location.
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

}
