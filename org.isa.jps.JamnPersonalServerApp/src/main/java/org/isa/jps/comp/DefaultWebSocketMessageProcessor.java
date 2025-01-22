/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.logging.Logger;

import org.isa.jps.JamnPersonalServerApp;

/**
 * <pre>
 * Example of how to create a server side websocket message listener.
 * </pre>
 */
public class DefaultWebSocketMessageProcessor {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(DefaultWebSocketMessageProcessor.class.getName());

    /**
     */
    private DefaultWebSocketMessageProcessor() {
    }

    public static void create() {
        JamnPersonalServerApp lServer = JamnPersonalServerApp.getInstance();

        lServer.addWebSocketMessageProcessor((String pConnectionId, byte[] pMessage) -> {
            String lMsg = new String(pMessage);
            LOG.info("WebSocket Message received: " + lMsg);

            return ("ECHO: " + lMsg).getBytes();
        });
    }

}
