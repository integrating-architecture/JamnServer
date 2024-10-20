/*Authored by www.integrating-architecture.de*/

package org.isa.ipc.web.api;

import org.isa.ipc.JamnWebRPCProvider;
import org.isa.ipc.JamnWebRPCProvider.Processor;
import org.isa.ipc.JamnWebRPCProvider.WebRPCService;
import org.isa.ipc.JamnWebRPCProvider.Response;

/**
 * <pre>
 * A WebRPC Service for the JamnWebRPCProvider must be implemented as one class
 * annotated with the @WebRPCService annotation.
 * The class must provide
 *  - one method annotated with the @Processor annotation
 *  - one data structure class annotated with the @Response annotation that is serializable to JSON
 * 
 * However, you can easily adapt and change these construction rules based on annotation as you like.
 * </pre>
 */
@WebRPCService(path = "/api/about", methods = { "GET", "POST" }, contentType = JamnWebRPCProvider.HTTPVAL_CONTENT_TYPE_JSON)
public class About {

	/**
	 * <pre>
	 * The @Processor method may have any name but MUST return an object of
	 * type @Response This is checked at runtime by the Provider. 
	 * Regardless of the name, this method is responsible for creating the response.
	 * </pre>
	 */
	@Processor()
	public AboutData sendServerInfos() {
		AboutData lResponse = new AboutData();

		// here lays the "business logic"
		lResponse.name = "JamnServer";
		lResponse.version = "0.0.1";
		lResponse.descr = "Just Another Micro Node Server";

		return lResponse;
	}

	/**
	 * The class annotated with @Response defines the data structure which is send
	 * as the response to the client. The class must be serializable to JSON.
	 */
	@Response()
	public static class AboutData {
		protected String name = "";
		protected String version = "";
		protected String descr = "";
	}
}
