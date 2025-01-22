/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

import org.isa.jps.JamnPersonalServerApp.Config;

/**
 * 
 */
public class ChildProcessWebSocketConnection {

    protected static Logger LOG = Logger.getLogger(ChildProcessWebSocketConnection.class.getName());

    protected Config config;
    protected WebSocket wsClient;
    protected String childId;
    protected String socketUrl;

    public ChildProcessWebSocketConnection(Config pConfig) {
        config = pConfig;
        childId = pConfig.getJPSChildId();
        socketUrl = config.getJPSParentUrl();
    }

    /**
     */
    public void connect() {

        wsClient = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(socketUrl), new WSEventListener())
                .join();
    }

    /***************************************************************************
     ***************************************************************************/
    /**
     */
    private class WSEventListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            LOG.info(() -> String.format("Child Process WebSocket connection established [%s]", childId));
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    }

}
