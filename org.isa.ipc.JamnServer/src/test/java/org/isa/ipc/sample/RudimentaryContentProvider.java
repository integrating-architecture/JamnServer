/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.IMAGE_PNG;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_HTML;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnServer.HttpHeader;

/**
 * 
 */
public class RudimentaryContentProvider implements JamnServer.ContentProvider {

    private static final Logger LOG = Logger.getLogger(RudimentaryContentProvider.class.getName());

    public static String TestJsonResponseMessage = "{\"user\":\"JamnServer\",\"message\":\"Welcome John :-)\"}";

    // just a helper
    private static byte[] readFile(String pFileName) throws IOException {
        return Files.readAllBytes(Paths.get("src/test/resources/http/", pFileName));
    }

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
                    lData = readFile("info.html");

                    lResponse.setContentType(TEXT_HTML);
                    pResponseContent.write(lData);

                } else if ("/isa-logo.png".equalsIgnoreCase(pPath)) {
                    lData = readFile("isa-logo.png");

                    lResponse.setContentType(IMAGE_PNG);
                    pResponseContent.write(lData);
                }

            } else if (lRequest.isPOST()
                    && lRequest.hasContentType(APPLICATION_JSON)
                    && "/wsapi".equalsIgnoreCase(pPath)) {
                // doing a POST with json - simplified
                // WITHOUT using a JSON Tool to avoid the dependency
                LOG.info(pRequestBody);

                lResponse.setContentType(APPLICATION_JSON);
                pResponseContent.write(TestJsonResponseMessage.getBytes());
            }
        } catch (Exception e) {
            lStatus = SC_500_INTERNAL_ERROR;
        }

        return lStatus;
    }

    @SuppressWarnings("unused")
    private static class MessageData {
        public String user = "";
        public String message = "";
    }
}
