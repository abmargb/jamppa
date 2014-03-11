jamppa
======

jamppa started as a fork from [smack](https://github.com/igniterealtime/Smack) and [whack](http://fisheye.igniterealtime.org/browse/whack/trunk), but it uses [tinder](http://fisheye.igniterealtime.org/browse/tinder/trunk), and consequently DOM, for XML manipulation, which makes the developer's life easier.

## Writing components

A jamppa component uses handlers triggered by namespaces, so that you don't rely on a single method to process all incoming IQs. All components should extend XMPPComponent and then use the addGetHandler() and addSetHandler() methods to include handlers.

Handlers should extend the AbstractQueryHandler class.

```java
public static void main(String[] args) throws ComponentException {
	UppercaseComponent component = new UppercaseComponent("uppercase.test.com", 
		"password", "localhost", 5347);
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
```

## Writing clients

jamppa, just like [sleekxmpp](http://sleekxmpp.com/), works with a plugin architecture, and only the XMPP core is implemented in the standard client.

```java
XMPPClient client = new XMPPClient("client@test.com", 
				"password", "localhost", 5222);

XEP0077 register = new XEP0077();
client.registerPlugin(register);

client.connect();
register.createAccount("client@test.com", "password");

client.login();
client.process(false);

IQ iq = new IQ(Type.get);
iq.setTo("uppercase.test.com");
iq.getElement()
		.addElement("query", "uppercase")
		.addElement("content")
		.setText("hello world");

IQ response = (IQ) client.syncSend(iq);
System.out.println(response.toXML());
client.disconnect();
```
