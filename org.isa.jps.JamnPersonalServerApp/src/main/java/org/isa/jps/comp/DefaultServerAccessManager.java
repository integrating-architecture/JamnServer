/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.AccessManager;
import org.isa.ipc.JamnServer.HttpHeader;
import org.isa.ipc.JamnServer.RequestMessage;
import org.isa.ipc.JamnServer.ResponseMessage;
import org.isa.jps.JamnPersonalServerApp.Config;

/**
 * Please see: {@link AccessManager}
 * 
 * This class is just a playground.
 */
public class DefaultServerAccessManager implements AccessManager {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(DefaultServerAccessManager.class.getName());

    protected Config config;

    protected Predicate<String> isNotLocalHost = host -> !(host.startsWith("localhost")
            || host.startsWith("127.0.0.1"));

    public DefaultServerAccessManager(Config pConfig) {
        config = pConfig;
    }

    /**
     */
    @Override
    public void processRequestAccess(RequestMessage pRequest, ResponseMessage pResponse)
            throws SecurityException {
        HttpHeader lRequestHeader = pRequest.header();

        if (lRequestHeader.getCookie().isEmpty()) {
            String lJamnCookie = "jamn.server=" + UUID.randomUUID().toString();
            pResponse.header().addSetCookie(lJamnCookie);
        }
    }

}
