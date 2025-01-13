/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import java.util.logging.Logger;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebSocketProvider;
import org.isa.ipc.JamnWebSocketProvider.WsoMessageConsumer;

/**
 * 
 */
public class SampleWebSocketApp {

    /**
     */
    public static void main(String[] args) {

        // create a Jamn server
        JamnServer lServer = new JamnServer();

        JamnWebSocketProvider lWebSocketProvider = JamnWebSocketProvider.Builder().setConfig(lServer.getConfig())
                .build();

        lWebSocketProvider.addMessageConsumer(new WsoMessageConsumer() {

            @Override
            public String getTopic() {
                return "/wsapi";
            }

            @Override
            public void onMessage(String pConnectionId, byte[] pMessage) {
                String lMsg = new String(pMessage);
                Logger.getGlobal().info("Message received: " + lMsg);

                lWebSocketProvider.createMessageSenderFor(pConnectionId, getTopic()).send(("ECHO: " + lMsg).getBytes());
            }
        });

        lServer.addContentProvider(JamnServer.WEBSOCKET_PROVIDER, lWebSocketProvider);

        lServer.start();
    }

}
