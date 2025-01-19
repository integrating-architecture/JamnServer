/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.logging.Logger;

import org.isa.jps.JamnPersonalServerApp;

/**
 * <pre>
 * </pre>
 */
public class DefaultWebSocketMessageConsumer {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(DefaultWebSocketMessageConsumer.class.getName());

    /**
     */
    private DefaultWebSocketMessageConsumer() {
    }

    public static void create() {
        JamnPersonalServerApp lServer = JamnPersonalServerApp.getInstance();

        lServer.addWebSocketMessageConsumer((String pConnectionId, byte[] pMessage) -> {
            String lMsg = new String(pMessage);
            LOG.info("WebSocket Message received: " + lMsg);

            return ("ECHO: " + lMsg).getBytes();
        });
    }

}
