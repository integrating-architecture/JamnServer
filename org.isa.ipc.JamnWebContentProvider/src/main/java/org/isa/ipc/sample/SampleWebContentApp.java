/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebContentProvider;
import org.isa.ipc.JamnWebContentProvider.FileEnricher;

/**
 * Run a JamnServer with the sample content provider;
 */
public class SampleWebContentApp {

    /**
     */
    public static void main(String[] args) {
        // create a Jamn server
        JamnServer lServer = new JamnServer();

        // set a customized ExtensionFactory for the content provider
        JamnWebContentProvider.setExtensionFactory(new JamnWebContentProvider.ExtensionFactory() {
            /**
             * Enable file enrichment with the default FileEnricher and a customer
             * ValueProvider for templating.
             */
            @Override
            public FileEnricher createFileEnricher() {
                return new JamnWebContentProvider.DefaultFileEnricher(new SampleFileEnricherValueProvider());
            }
        });

        // create the provider with a webroot
        // no leading slash because relative path
        JamnWebContentProvider lWebContentProvider = new JamnWebContentProvider("src/test/resources/var/http/sample");
        // add it to server
        lServer.addContentProvider(lWebContentProvider);

        // start server
        lServer.start();
    }
}
