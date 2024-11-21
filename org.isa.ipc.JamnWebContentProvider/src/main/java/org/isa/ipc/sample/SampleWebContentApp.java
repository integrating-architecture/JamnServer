/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebContentProvider;

/**
 * <pre>
 * A test app for starting a JamnServer with a web content provider.
 * </pre>
 */
public class SampleWebContentApp {

    public static void main(String[] args) {
        // create a Jamn server
        JamnServer lServer = new JamnServer();

        // create the JamnWebContentProvider with a webroot
        // no leading slash because relative path
        JamnWebContentProvider lWebContentProvider = new JamnWebContentProvider("src/test/resources/var/http/sample");
        // add to server
        lServer.addContentProvider(lWebContentProvider);

        // start server
        lServer.start();

    }

}
