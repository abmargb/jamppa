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

package org.jivesoftware.smack.util;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Element;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XMPPError;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Utility class that helps to parse packets. Any parsing packets method that
 * must be shared between many clients must be placed in this utility class.
 * 
 * @author Gaston Dombiak
 */
public class PacketParserUtils {
	private static Logger logger = Logger.getLogger(PacketParserUtils.class
			.getName());

	public static String parseContentDepth(XmlPullParser parser, int depth)
			throws XmlPullParserException, IOException {
		StringBuffer content = new StringBuffer();
		while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == depth)) {
			content.append(parser.getText());
		}
		return content.toString();
	}

	/**
	 * Parses stream error packets.
	 * 
	 * @param doc
	 *            the XML parser.
	 * @return an stream error packet.
	 * @throws Exception
	 *             if an exception occurs while parsing the packet.
	 */
	public static StreamError parseStreamError(Element el) throws IOException,
			XmlPullParserException {
		String code = null;
		Element condEl = (Element) el.elements().iterator().next();
		if (condEl.getNamespace().equals(StreamError.NAMESPACE)) {
			code = condEl.getName();
		}
		String text = condEl.elementText("text");
		return new StreamError(code, text);
	}
	
	/**
     * Parse the available SASL mechanisms reported from the server.
     *
     * @param mechanismsEl the XML parser, positioned at the start of the mechanisms stanza.
     * @return a collection of Stings with the mechanisms included in the mechanisms stanza.
     * @throws Exception if an exception occurs while parsing the stanza.
     */
    @SuppressWarnings("unchecked")
	public static Collection<String> parseMechanisms(Element mechanismsEl) throws Exception {
        List<Element> mechanisms = mechanismsEl.elements("mechanism");
        List<String> mechanismsStr = new LinkedList<String>();
        for (Element mechanismEl : mechanisms) {
			mechanismsStr.add(mechanismEl.getText());
		}
        return mechanismsStr;
    }

    /**
     * Parse the available compression methods reported from the server.
     *
     * @param compressionEl the XML parser, positioned at the start of the compression stanza.
     * @return a collection of Stings with the methods included in the compression stanza.
     * @throws Exception if an exception occurs while parsing the stanza.
     */
    @SuppressWarnings("unchecked")
	public static Collection<String> parseCompressionMethods(Element compressionEl)
            throws IOException, XmlPullParserException {
        List<Element> methodsEls = compressionEl.elements("method");
        List<String> methodsStr = new LinkedList<String>();
        for (Element methodEl : methodsEls) {
        	methodsStr.add(methodEl.getText());
		}
        return methodsStr;
    }

	/**
	 * Parses error sub-packets.
	 * 
	 * @param parser
	 *            the XML parser.
	 * @return an error sub-packet.
	 * @throws Exception
	 *             if an exception occurs while parsing the packet.
	 */
	public static XMPPError parseError(XmlPullParser parser) throws Exception {
		final String errorNamespace = "urn:ietf:params:xml:ns:xmpp-stanzas";
		String errorCode = "-1";
		String type = null;
		String message = null;
		String condition = null;
		List<PacketExtension> extensions = new ArrayList<PacketExtension>();

		// Parse the error header
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			if (parser.getAttributeName(i).equals("code")) {
				errorCode = parser.getAttributeValue("", "code");
			}
			if (parser.getAttributeName(i).equals("type")) {
				type = parser.getAttributeValue("", "type");
			}
		}
		boolean done = false;
		// Parse the text and condition tags
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals("text")) {
					message = parser.nextText();
				} else {
					// Condition tag, it can be xmpp error or an application
					// defined error.
					String elementName = parser.getName();
					String namespace = parser.getNamespace();
					if (errorNamespace.equals(namespace)) {
						condition = elementName;
					} else {
						extensions.add(parsePacketExtension(elementName,
								namespace, parser));
					}
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (parser.getName().equals("error")) {
					done = true;
				}
			}
		}
		// Parse the error type.
		XMPPError.Type errorType = XMPPError.Type.CANCEL;
		try {
			if (type != null) {
				errorType = XMPPError.Type.valueOf(type.toUpperCase());
			}
		} catch (IllegalArgumentException iae) {
			logger.log(Level.SEVERE,
					"Could not find error type for " + type.toUpperCase(), iae);
		}
		return new XMPPError(Integer.parseInt(errorCode), errorType, condition,
				message, extensions);
	}

	/**
	 * Parses a packet extension sub-packet.
	 * 
	 * @param elementName
	 *            the XML element name of the packet extension.
	 * @param namespace
	 *            the XML namespace of the packet extension.
	 * @param parser
	 *            the XML parser, positioned at the starting element of the
	 *            extension.
	 * @return a PacketExtension.
	 * @throws Exception
	 *             if a parsing error occurs.
	 */
	public static PacketExtension parsePacketExtension(String elementName,
			String namespace, XmlPullParser parser) throws Exception {
		DefaultPacketExtension extension = new DefaultPacketExtension(
				elementName, namespace);
		boolean done = false;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				String name = parser.getName();
				// If an empty element, set the value with the empty string.
				if (parser.isEmptyElementTag()) {
					extension.setValue(name, "");
				}
				// Otherwise, get the the element text.
				else {
					eventType = parser.next();
					if (eventType == XmlPullParser.TEXT) {
						String value = parser.getText();
						extension.setValue(name, value);
					}
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (parser.getName().equals(elementName)) {
					done = true;
				}
			}
		}
		return extension;
	}

	public static Object parseWithIntrospection(String elementName,
			Class<?> objectClass, XmlPullParser parser) throws Exception {
		boolean done = false;
		Object object = objectClass.newInstance();
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				String name = parser.getName();
				String stringValue = parser.nextText();
				PropertyDescriptor descriptor = new PropertyDescriptor(name,
						objectClass);
				// Load the class type of the property.
				Class<?> propertyType = descriptor.getPropertyType();
				// Get the value of the property by converting it from a
				// String to the correct object type.
				Object value = decode(propertyType, stringValue);
				// Set the value of the bean.
				descriptor.getWriteMethod().invoke(object, value);
			} else if (eventType == XmlPullParser.END_TAG) {
				if (parser.getName().equals(elementName)) {
					done = true;
				}
			}
		}
		return object;
	}

	/**
	 * Decodes a String into an object of the specified type. If the object type
	 * is not supported, null will be returned.
	 * 
	 * @param type
	 *            the type of the property.
	 * @param value
	 *            the encode String value to decode.
	 * @return the String value decoded into the specified type.
	 * @throws Exception
	 *             If decoding failed due to an error.
	 */
	private static Object decode(Class<?> type, String value) throws Exception {
		if (type.getName().equals("java.lang.String")) {
			return value;
		}
		if (type.getName().equals("boolean")) {
			return Boolean.valueOf(value);
		}
		if (type.getName().equals("int")) {
			return Integer.valueOf(value);
		}
		if (type.getName().equals("long")) {
			return Long.valueOf(value);
		}
		if (type.getName().equals("float")) {
			return Float.valueOf(value);
		}
		if (type.getName().equals("double")) {
			return Double.valueOf(value);
		}
		if (type.getName().equals("java.lang.Class")) {
			return Class.forName(value);
		}
		return null;
	}
	
	public static void updateText(Element parentEl, String elName, String text) {
		Element el = parentEl.element(elName);
        if (el == null) {
        	el = parentEl.addElement(elName);
        }
        el.setText(text);
	}

}
