/* Authored by www.integrating-architecture.de */
package org.isa.ipc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.isa.ipc.sample.web.api.SampleWebApiServices;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JamnWebServiceProvider Unit test.
 */
@DisplayName("Jamn Server WebServiceProvider Test")
public class WebServiceProviderTest {

    private static HttpClient Client;
    private static JamnServer Server;
    private static String ServerURL;

    // JSON Tool
    private static ObjectMapper Jack = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    @BeforeAll
    public static void setupEnvironment() throws Exception {
        // create standard Java SE HTTP Client
        Client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        // create a JamnServer
        Server = new JamnServer(8099);
        // define a server base url variable for the tests
        // e.g. default: http://localhost:8099
        ServerURL = "http://localhost:" + Server.getConfig().getPort();

        // WebServiceProvider setup
        // set logging and json tool for the JamnWebServiceProvider
        JamnWebServiceProvider.setLogger(Server.getLoggerFor(JamnWebServiceProvider.class.getName()));

        JamnWebServiceProvider.setJsonTool(new JamnServer.JsonToolWrapper() {
            @Override
            public <T> T toObject(String pSrc, Class<T> pType) throws Exception {
                return Jack.readValue(pSrc, pType);
            }

            @Override
            public String toString(Object pObj) throws Exception {
                return Jack.writeValueAsString(pObj);
            }
        });

        // create the WebService provider
        JamnWebServiceProvider lWebServiceProvider = new JamnWebServiceProvider();

        // register your Web-API Services
        lWebServiceProvider.registerServices(SampleWebApiServices.class);

        // get the actual provider and add it to the server
        Server.addContentProvider("WebServiceProvider", lWebServiceProvider);
        // start server
        Server.start();
        assertTrue(Server.isRunning(), "Test Server start FAILED");
    }

    @AfterAll
    public static void shutDownServer() {
        Server.stop();
    }

    @Test
    void testApiEcho() throws Exception {
        String lMessage = "Client message for JamnServer API";

        HttpRequest lRequest = HttpRequest.newBuilder().uri(new URI(ServerURL + "/api/echo"))
                .headers("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(lMessage)).build();

        HttpResponse<String> lResponse = Client.send(lRequest, BodyHandlers.ofString());

        assertEquals(200, lResponse.statusCode(), "HTTP Status");
        assertEquals("ECHO: " + lMessage, lResponse.body(), "HTTP Body");
    }

    @Test
    void testGETApiAboutErrorNotFound() throws Exception {
        // Error Case - test for unknown service path
        // status: 404 - Not found
        // log: WebService API Error: [Unsupported WebService Path [/about]]
        HttpRequest lRequest = HttpRequest.newBuilder().uri(new URI(ServerURL + "/about"))
                .headers("Content-Type", "application/json").GET().build();

        HttpResponse<String> lResponse = Client.send(lRequest, BodyHandlers.ofString());
        assertEquals(404, lResponse.statusCode(), "HTTP Status");
    }

    @Test
    void testGETApiAbout() throws Exception {
        HttpRequest lRequest = HttpRequest.newBuilder().uri(new URI(ServerURL + "/api/about"))
                .headers("Content-Type", "application/json").GET().build();

        HttpResponse<String> lResponse = Client.send(lRequest, BodyHandlers.ofString());

        assertEquals(200, lResponse.statusCode(), "HTTP Status");

        SampleWebApiServices.AboutResponse lAbout = Jack.readValue(lResponse.body(),
                SampleWebApiServices.AboutResponse.class);
        assertEquals("0.0.1", lAbout.version, "AboutResponse.version");
    }
}
