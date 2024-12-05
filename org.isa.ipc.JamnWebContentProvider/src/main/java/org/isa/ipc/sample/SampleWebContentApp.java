/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebContentProvider;
import org.isa.ipc.JamnWebContentProvider.DefaultFileEnricher;

/**
 * Run a JamnServer with the sample content provider;
 */
public class SampleWebContentApp {

    /**
     */
    public static void main(String[] args) {
        // create a Jamn server
        JamnServer lServer = new JamnServer();

        // create the provider with a webroot
        // no leading slash because relative path
        JamnWebContentProvider lWebContentProvider = JamnWebContentProvider
                .Builder("src/test/resources/var/http/sample")
                .setConfig(lServer.getConfig())
                .setFileEnricher(new DefaultFileEnricher(new SampleFileEnricherValueProvider()))
                .build();
        // add it to server
        lServer.addContentProvider(JamnServer.SAMPLE_CONTENT_PROVIDER, lWebContentProvider);

        // start server
        lServer.start();
    }
}
