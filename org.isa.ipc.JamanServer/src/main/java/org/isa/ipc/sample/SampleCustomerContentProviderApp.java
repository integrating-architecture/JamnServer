/*Authored by www.integrating-architecture.de*/

package org.isa.ipc.sample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnServer.ContentProviderDispatcher;
import org.isa.ipc.http.HttpHeader;

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
 * - browser: http://localhost:8099/info - should show the info-page
 * - browser: open||drop in the js-fetch-page.html file - should show a json request and response
 * </pre>
 */
public class SampleCustomerContentProviderApp {

	private static final ObjectMapper JSON = new ObjectMapper();

	/**
	 */
	public static void main(String[] args) {
		JamnServer lServer = new JamnServer();

		String lProviderID = SampleContentProvider.class.getName();
		lServer.addContentProvider(lProviderID, new SampleContentProvider());
		lServer.setContentProviderDispatcher(new ContentProviderDispatcher() {
			@Override
			public String getContentProviderIDFor(Map<String, String> pRequestAttributes) {
				return lProviderID;
			}});
		lServer.getConfig().setCORSEnabled(true); // required for localhost communication via js fetch
		lServer.start();
	}

	/**
	 */
	public static class SampleContentProvider implements JamnServer.ContentProvider, JamnServer.HttpConstants {

		@Override
		public String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
				String pMethod, String pPath, String pRequestBody, Map<String, String> pRequestAttributes) {

			String lStatus = HTTP_200_OK;
			HttpHeader lRequest = new HttpHeader(pRequestAttributes);
			HttpHeader lResponse = new HttpHeader(pResponseAttributes);

			try {
				// doing GET
				if (lRequest.isGET()) {
					byte[] lData = null;
					if ("/info".equalsIgnoreCase(pPath)) {
						lData = readResourceFile("/info-page.html");

						lResponse.setCONTENT_TYPE_HTML();
						pResponseContent.write(lData);

					} else if ("/isa-logo.png".equalsIgnoreCase(pPath)) {
						lData = readResourceFile("/isa-logo.png");

						lResponse.setCONTENT_TYPE_IMG_PNG();
						pResponseContent.write(lData);
					}
					// doing a POST with json
				} else if (lRequest.isPOST() && lRequest.hasCONTENT_TYPE_JSON()) {
					Message lResponseMsg = new Message();
					Message lMsg = JSON.readValue(pRequestBody, Message.class);

					lResponseMsg.user = "JamnServer";
					lResponseMsg.message = "Welcome " + lMsg.user + " :-)";

					lResponse.setCONTENT_TYPE_JSON();
					pResponseContent.write(JSON.writeValueAsString(lResponseMsg).getBytes());
				}
			} catch (Exception e) {
				lStatus = HTTP_500_INTERNAL_ERROR;
			}

			return lStatus;
		}
	}

	@SuppressWarnings("unused")
	private static class Message {
		public String user = "";
		public String message = "";
	}

	/**
	 * @throws IOException
	 */
	private static byte[] readResourceFile(String pName) throws IOException {
		ByteArrayOutputStream lResult = new ByteArrayOutputStream();
		InputStream lInput = SampleCustomerContentProviderApp.class.getResourceAsStream(pName);
		byte[] lBuffer = new byte[1024];
		for (int length; (length = lInput.read(lBuffer)) != -1;) {
			lResult.write(lBuffer, 0, length);
		}
		return lResult.toByteArray();
	}

}
