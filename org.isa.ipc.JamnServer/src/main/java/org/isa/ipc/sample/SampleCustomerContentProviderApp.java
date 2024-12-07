/* Authored by www.integrating-architecture.de */

package org.isa.ipc.sample;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.IMAGE_PNG;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_HTML;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnServer.HttpHeader;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * A very basic, hard coded sample of a customer ContentProvider.
 *
 * The sample uses "com.fasterxml.jackson" JSON
 * which is NOT a dependency of the JamnServer implementation.
 *
 * When this sample Server is running
 * - browser: http://localhost:8099 - should show an empty page
 * - browser: http://localhost:8099/info - should show the info.html page
 * - browser: open||drop in the js-fetch-page.html file - should show a json request and response
 * </pre>
 */
public class SampleCustomerContentProviderApp {

    private static final ObjectMapper JSON = new ObjectMapper();
    /**
     */
    private static String WebRoot = "src/test/resources/var/http/sample/";

    private static byte[] readResourceFile(String pFileName) throws Exception {
        String lPath = WebRoot + pFileName;
        return Files.readAllBytes(Paths.get(lPath));
    }

    /**
     */
    public static void main(String[] args) {
        JamnServer lServer = new JamnServer();

        lServer.addContentProvider(JamnServer.SAMPLE_CONTENT_PROVIDER, new SampleContentProvider());
        lServer.getConfig().setCORSEnabled(true); // required for localhost communication via js fetch
        lServer.start();
    }

    /**
     */
    public static class SampleContentProvider implements JamnServer.ContentProvider {

        @Override
        public String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
                String pMethod, String pPath, String pRequestBody, Map<String, String> pRequestAttributes) {

            String lStatus = SC_200_OK;
            HttpHeader lRequest = new HttpHeader(pRequestAttributes);
            HttpHeader lResponse = new HttpHeader(pResponseAttributes);

            try {
                // doing GET
                if (lRequest.isGET()) {
                    byte[] lData = null;
                    if ("/info".equalsIgnoreCase(pPath)) {
                        lData = readResourceFile("info.html");

                        lResponse.setContentType(TEXT_HTML);
                        pResponseContent.write(lData);

                    } else if ("/isa-logo.png".equalsIgnoreCase(pPath)) {
                        lData = readResourceFile("isa-logo.png");

                        lResponse.setContentType(IMAGE_PNG);
                        pResponseContent.write(lData);
                    }
                    // doing a POST with json
                } else if (lRequest.isPOST() && lRequest.hasContentType(APPLICATION_JSON)) {
                    MessageData lResponseMsg = new MessageData();
                    MessageData lMsg = JSON.readValue(pRequestBody, MessageData.class);

                    lResponseMsg.user = "JamnServer";
                    lResponseMsg.message = "Welcome " + lMsg.user + " :-)";

                    lResponse.setContentType(APPLICATION_JSON);
                    pResponseContent.write(JSON.writeValueAsString(lResponseMsg).getBytes());
                }
            } catch (Exception e) {
                lStatus = SC_500_INTERNAL_ERROR;
            }

            return lStatus;
        }
    }

    @SuppressWarnings("unused")
    private static class MessageData {
        public String user = "";
        public String message = "";
    }
}
