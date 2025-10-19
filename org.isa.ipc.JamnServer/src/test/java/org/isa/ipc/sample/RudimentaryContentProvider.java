/* Authored by iqbserve.de */
package org.isa.ipc.sample;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.IMAGE_PNG;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_HTML;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnServer.RequestMessage;
import org.isa.ipc.JamnServer.ResponseMessage;

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
    public void handleContentProcessing(RequestMessage pRequest, ResponseMessage pResponse) {

        String lPath = pRequest.getPath();

        try {
            // doing GET
            if (pRequest.isMethod("GET")) {
                byte[] lData = null;
                if ("/info".equalsIgnoreCase(lPath)) {
                    lData = readFile("info.html");

                    pResponse.setContentType(TEXT_HTML);
                    pResponse.writeToContent(lData);

                } else if ("/isa-logo.png".equalsIgnoreCase(lPath)) {
                    lData = readFile("isa-logo.png");

                    pResponse.setContentType(IMAGE_PNG);
                    pResponse.writeToContent(lData);
                }

            } else if (pRequest.isMethod("POST")
                    && pRequest.hasContentType(APPLICATION_JSON)
                    && "/wsapi".equalsIgnoreCase(lPath)) {
                // doing a POST with json - simplified
                // WITHOUT using a JSON Tool to avoid the dependency
                LOG.info(pRequest.body());

                pResponse.setContentType(APPLICATION_JSON);
                pResponse.writeToContent(TestJsonResponseMessage.getBytes());
            }
        } catch (Exception e) {
            pResponse.setStatus(SC_500_INTERNAL_ERROR);
        }

        pResponse.setStatus(SC_200_OK);
    }

    @SuppressWarnings("unused")
    private static class MessageData {
        public String user = "";
        public String message = "";
    }
}
