/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.HttpHeader;
import org.isa.ipc.JamnServer.SecurityController;
import org.isa.jps.JamnPersonalServerApp.Config;

/**
 * 
 */
public class JPSSecurityController implements SecurityController {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(JPSSecurityController.class.getName());

    protected Config config;

    protected Predicate<String> isNotLocalHost = host -> !(host.startsWith("localhost")
            || host.startsWith("127.0.0.1"));

    public JPSSecurityController(Config pConfig) {
        config = pConfig;
    }

    /**
     */
    @Override
    public void checkAccess(final Map<String, String> pRequestAttributes) throws SecurityException {
        boolean denied = true;
        HttpHeader lHeader = new HttpHeader(pRequestAttributes);
        String lPath = lHeader.getPath();

        // check only if NOT localhost
        if (!isNotLocalHost.test(lHeader.getHost())) {

            if (lPath.contains("login") || lPath.contains("logo.png") || lPath.contains("favicon.ico")) {
                denied = false;
            }
        }
        LOG.info(String.format("Access [%s] [%s]", (denied ? "denied" : "granted"), lPath));
    }

}
