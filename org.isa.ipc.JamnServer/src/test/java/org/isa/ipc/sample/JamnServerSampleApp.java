/* Authored by iqbserve.de */
package org.isa.ipc.sample;

import org.isa.ipc.JamnServer;

/**
 * <pre>
 * Run the basic JamnServer with the rudimentary sample ContentProvider.
 *  - browser call:  http://localhost:8099/info
 * </pre>
 */
public class JamnServerSampleApp {

    /**
     */
    public static void main(String[] args) {
        JamnServer lServer = new JamnServer(8099);
        // add the sample content provider

        lServer.addContentProvider("ContentProvider", new RudimentaryContentProvider());

        // enable CORS - required for manually browser js fetch via local html file
        lServer.getConfig().setCORSEnabled(true);

        lServer.start();

    }

}
