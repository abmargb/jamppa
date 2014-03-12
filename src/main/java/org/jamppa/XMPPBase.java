package org.jamppa;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.xmpp.component.ComponentException;

public class XMPPBase {

    private static final Logger LOGGER = Logger.getLogger(XMPPBase.class);
    private final Semaphore mutex = new Semaphore(0);

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
                try {
                    mutex.acquire();
                } catch (InterruptedException e) {
                    LOGGER.fatal("Main loop.", e);
                }
            }
        };

        Thread t = new Thread(componentRunnable, "jamppa-hanging-thread");
        if (block) {
            t.run();
        } else {
            t.start();
        }
    }

    public void disconnect() {
        mutex.release();
    }

}
