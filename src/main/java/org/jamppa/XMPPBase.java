package org.jamppa;

import org.apache.log4j.Logger;
import org.xmpp.component.ComponentException;

public class XMPPBase {

	private static final Logger LOGGER = Logger.getLogger(XMPPBase.class);
	
	public void process() {
		process(false);
	}

	/**
	 * @throws ComponentException
	 * 
	 */
	public void process(boolean block) {
		Runnable componentRunnable = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						LOGGER.fatal("Main loop.", e);
					}
				}
			}
		};

		Thread t = new Thread(componentRunnable,
				"jamppa-hanging-thread");
		if (block) {
			t.run();
		} else {
			t.start();
		}
	}

}
