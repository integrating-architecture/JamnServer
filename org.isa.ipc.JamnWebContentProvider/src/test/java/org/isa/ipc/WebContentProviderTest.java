/* Authored by www.integrating-architecture.de */
package org.isa.ipc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JamnWebContentProvider Unit test.
 */
@DisplayName("Jamn Server WebContentProvider Test")
public class WebContentProviderTest {
    private static HttpClient Client;
    private static JamnServer Server;
    private static String ServerURL;

    @BeforeAll
    public static void setupEnvironment() throws Exception {
        // create standard Java SE HTTP Client
        Client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        // create a JamnServer
        Server = new JamnServer(8099);
        // define a server base url variable for the tests
        // e.g. default: http://localhost:8099
        ServerURL = "http://localhost:" + Server.getConfig().getPort();

        // create the JamnWebContentProvider with a webroot
        // no leading slash because relative path
        JamnWebContentProvider lWebContentProvider = JamnWebContentProvider
                .Builder("src/test/resources/var/http/sample").build();
        // add to server
        Server.addContentProvider(JamnServer.SAMPLE_CONTENT_PROVIDER, lWebContentProvider);

        // start server
        Server.start();
        assertTrue(Server.isRunning(), "Test Server start FAILED");
    }

    @AfterAll
    public static void shutDownServer() {
        Server.stop();
    }

    @Test
    void testRoot() throws Exception {
        String lPath = "/";

        HttpRequest lRequest = HttpRequest.newBuilder().uri(new URI(ServerURL + lPath))
                .headers("Content-Type", "text/html").GET().build();

        HttpResponse<String> lResponse = Client.send(lRequest, BodyHandlers.ofString());

        assertEquals(200, lResponse.statusCode(), "HTTP Status");
    }

}
