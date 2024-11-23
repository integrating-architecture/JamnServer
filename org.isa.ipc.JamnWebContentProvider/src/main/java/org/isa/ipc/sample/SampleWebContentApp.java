/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebContentProvider;
import org.isa.ipc.JamnWebContentProvider.FileEnricher;
import org.isa.ipc.JamnWebContentProvider.WebFile;
import org.isa.ipc.sample.util.ExprString;

/**
 * <pre>
 * A test app to start a JamnServer with a Web Content Provider.
 * The sample also uses a customer ExtensionFactory to perform file injection at load time.
 * Browser: http://localhost:8099/
 * </pre>
 */
public class SampleWebContentApp {

    /**
     */
    public static void main(String[] args) {
        // create a Jamn server
        JamnServer lServer = new JamnServer();

        // create a customized ExtensionFactory for the content provider
        JamnWebContentProvider.setExtensionFactory(new SampleExtensionFactory());
        // create the provider with a webroot
        // no leading slash because relative path
        JamnWebContentProvider lWebContentProvider = new JamnWebContentProvider("src/test/resources/var/http/sample");
        // add to server
        lServer.addContentProvider(lWebContentProvider);

        // start server
        lServer.start();
    }

    /*********************************************************
     * The factory sample.
     *********************************************************/
    /**
     */
    private static class SampleExtensionFactory extends JamnWebContentProvider.ExtensionFactory {

        /**
         * We only customize the FileEnricher
         */
        public FileEnricher createFileEnricher() {
            return new SampleFileEnricher();
        }

        /**
         * The sample file enricher looks for a TEMPLATE_MARKER at the head/top of a
         * webfile. If this marker is present all value expressions like ${sometext} get
         * replaced with values or an empty string.
         */
        private static class SampleFileEnricher implements FileEnricher, ExprString.ValueProvider {
            // the marker is expected as a file type comment e.g. like
            // <!--isa.template-->, /*isa.template*/
            // at the beginning of the file
            private static final String TEMPLATE_MARKER = "jamn.web.template";
            private static int MarkLen = TEMPLATE_MARKER.length() + 10;// marklen + greatest commntlen + surcharge
            private static Charset Encoding = Charset.forName("UTF-8");

            @Override
            public void enrich(WebFile pFile) throws Exception {
                String lContent = "";
                // only process if text format and a TEMPLATE_MARKER is present
                if (pFile.isTextFormat() && hasTemplateMarker(pFile)) {
                    lContent = new String(pFile.getData(), Encoding);
                    lContent = new TmplString(lContent, this).build();
                    pFile.setData(lContent.getBytes(Encoding));
                }
            }

            /**
             * Read the first MarkLen bytes of a file and ckeck for the template marker.
             */
            private boolean hasTemplateMarker(WebFile pFile) throws Exception {
                String lHead = "";
                byte[] lBuffer = new byte[MarkLen];
                if (pFile.getData() != null && pFile.getData().length > MarkLen) {
                    System.arraycopy(pFile.getData(), 0, lBuffer, 0, MarkLen);
                    lHead = new String(lBuffer, Encoding);
                }
                return lHead.contains(TEMPLATE_MARKER);
            }

            /**
             * ExprString value provider interface.
             */
            @Override
            public String getValue(String pKey) {
                if ("server.version".equals(pKey)) {
                    return "0.0.1-DVLP";
                }
                if ("index.title".equals(pKey)) {
                    return "Sample WebContent";
                }
                return "";
            }

            /**
             */
            private static class TmplString extends ExprString {
                {
                    ExprPattern = Pattern.compile("\\$\\{(\\w.+)\\}");
                }
                public TmplString(String pTemplate, ValueProvider pProvider) {
                    super(pTemplate, pProvider);
                }
            }
        }
    }
}
