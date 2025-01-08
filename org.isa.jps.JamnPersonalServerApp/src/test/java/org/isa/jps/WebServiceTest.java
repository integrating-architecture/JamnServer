/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.JsonToolWrapper;
import org.isa.jps.comp.DefaultWebServices.ShellRequest;
import org.isa.jps.comp.DefaultWebServices.ShellResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 
 */
@DisplayName("JamnPersonalServerApp WebService Test")
public class WebServiceTest {

    private static Logger LOG = Logger.getLogger(WebServiceTest.class.getName());

    private static JamnPersonalServerApp ServerApp;
    private static String ServerURL;
    private static HttpClient Client;
    private static JsonToolWrapper Json;

    @BeforeAll
    public static void setupEnvironment() throws Exception {
        ServerApp = JamnPersonalServerApp.getInstance();
        ServerApp.start(new String[] {});
        assertTrue(ServerApp.isRunning(), "Test Server start FAILED");

        Json = ServerApp.getJsonTool();
        ServerURL = "http://localhost:" + ServerApp.getConfig().getPort();

        Client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    @AfterAll
    public static void shutDownServer() {
        ServerApp.stop();
    }

    @Test
    void testShellService() throws Exception {
        ShellRequest lShellRequest = new ShellRequest("dir").setWorkingDir("/temp");
        String lData = Json.toString(lShellRequest);

        HttpRequest lRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(lData))
                .uri(new URI(ServerURL + ShellRequest.Path))
                .headers("Content-Type", ShellRequest.ContentType)
                .build();

        HttpResponse<String> lResponse = Client.send(lRequest, BodyHandlers.ofString());
        assertEquals(200, lResponse.statusCode(), "Bad http status");

        lData = lResponse.body();
        ShellResponse lShellResponse = Json.toObject(lData, ShellResponse.class);
        LOG.info(lShellResponse.getOutput().iterator().next());
    }
}
