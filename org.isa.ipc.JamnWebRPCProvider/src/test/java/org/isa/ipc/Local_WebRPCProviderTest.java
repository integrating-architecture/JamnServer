package org.isa.ipc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.isa.ipc.sample.web.api.SampleWebApiServices;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JamnWebRPCProvider Unit test.
 */
@DisplayName("Local_WebRPCProviderTest")
@Disabled
public class Local_WebRPCProviderTest {

	private static HttpClient Client;
	private static JamnServer Server;
	private static String ServerURL;

	@BeforeAll
	public static void setupServerAndProvider() throws Exception {
		// create standard Java SE HTTP Client
		Client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

		// create a JamnServer
		Server = new JamnServer();
		// set the server port
		Server.getConfig().setPort(8099);
		// define a server base url variable for the tests
		// e.g. default: http://localhost:8099
		ServerURL = "http://localhost:" + Server.getConfig().getPort();

		// if JavaScript/HTML/Browser calls are needed
		// CORS must be enabled on a LOCAL server address
		// Server.getConfig().setCORSEnabled(true); // required for localhost
		// communication via js fetch

		// WebRPCProvider setup
		// set logging and json tool for the JamnWebRPCProvider
		JamnWebRPCProvider.setLogger(Server.getLoggerFor(JamnWebRPCProvider.class.getName()));

		JamnWebRPCProvider.setJsonTool(new JamnWebRPCProvider.JsonToolWrapper() {
			private final ObjectMapper Jack = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

			@Override
			public <T> T toObject(String pSrc, Class<T> pType) throws Exception {
				return Jack.readValue(pSrc, pType);
			}

			@Override
			public String toString(Object pObj) throws Exception {
				return Jack.writeValueAsString(pObj);
			}
		});

		// create the RPC provider
		JamnWebRPCProvider lWebRPCProvider = new JamnWebRPCProvider();

		// register your RPC-API Services
		lWebRPCProvider.registerApiService(SampleWebApiServices.class);

		// get the actual provider and add it to the server
		Server.addContentProvider("RPCProvider", lWebRPCProvider.getJamnContentProvider());
		// start server
		Server.start();
	}

	@AfterAll
	public static void shutDownServer() {
		Server.stop();
	}

	@Test
	public void testApiEcho() throws Exception {
		String lMsg = "0123456789";
		HttpRequest lRequest = null;
		HttpResponse<String> lResponse = null;

		for (int i = 0; i < 100; i++) {
			lRequest = HttpRequest.newBuilder().uri(new URI(ServerURL + "/api/echo"))
					.headers("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString(lMsg)).build();

			lResponse = Client.send(lRequest, BodyHandlers.ofString());

			String lBody = lResponse.body();
			int lStatus = lResponse.statusCode();

			assertEquals(200, lStatus, "HTTP Status");
			assertEquals("ECHO: " + lMsg, lBody, "HTTP Body");
		}
	}
}