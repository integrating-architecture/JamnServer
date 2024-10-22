/*Authored by www.integrating-architecture.de*/

package org.isa.ipc.sample.web.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.isa.ipc.JamnWebRPCProvider;
import org.isa.ipc.JamnWebRPCProvider.WebRPCService;

/**
 * <pre>
 * A minimal WebRPCService example.
 * Since this JamnWebService-Provider implementation does NOT strictly follow the REST paradigm
 * the get and post methods are used like:
 * - GET = NO parameter but accessible just by url even from browser 
 * - POST = parameter supported but accessible only from a "fetch" like function
 * </pre>
 */
public class SampleServerApiServices {

	public static final String PATHBASE_API = "/api";
	public static final String PATHBASE_API_SERVER = "/api/server";

	/***************************************************************************************
	 * RPCService - About
	 **************************************************************************************/
	/**
	 * <pre>
	 * RPCService - About
	 * NO arguments but return type - accessible via get + post
	 * http://localhost:8099/api/about
	 * </pre>
	 */
	@WebRPCService(path = PATHBASE_API+"/about", methods = { "GET", "POST" }, contentType = JamnWebRPCProvider.HTTPVAL_CONTENT_TYPE_JSON)
	public AboutResponse sendAboutInfos() {
		AboutResponse lResponse = new AboutResponse();

		lResponse.name = "JamnServer Web Service API";
		lResponse.version = "0.0.1";
		lResponse.descr = "Just Another Micro Node Server Web API";

		return lResponse;
	}

	/**
	 */
	public static class AboutResponse {
		protected String name = "";
		protected String version = "";
		protected String descr = "";
	}

	/***************************************************************************************
	 * RPCService - Echo
	 **************************************************************************************/
	/**
	 * <pre>
	 * RPCService - Echo
	 * with native String argument and return type - accessible only via post
	 * http://localhost:8099/api/echo
	 * </pre>
	 */
	@WebRPCService(path = PATHBASE_API + "/echo", methods = { "POST" }, contentType = JamnWebRPCProvider.HTTPVAL_CONTENT_TYPE_TEXT)
	public String sendEcho(String pRequest) {
		return "Server ECHO: " + pRequest;
	}

	/***************************************************************************************
	 * RPCService - Get detailed Server Info
	 **************************************************************************************/
	/**
	 * <pre>
	 * RPCService - Get detailed Server Info
	 * with argument and return type - accessible only via post  
	 * Request example: {"subjects":["name","version","provider"]}
	 * http://localhost:8099/api/server/get-details
	 * </pre>
	 */
	@WebRPCService(path = PATHBASE_API_SERVER + "/get-details", methods = {"POST" }, contentType = JamnWebRPCProvider.HTTPVAL_CONTENT_TYPE_JSON)
	public DetailsResponse sendDetailsFor(DetailsRequest pRequest) {
		DetailsResponse lResponse = new DetailsResponse();

		for (String detail : pRequest.getSubjects()) {
			if ("name".equalsIgnoreCase(detail)) {
				lResponse.putSubject(detail, "JamnServer");
			} else if ("version".equalsIgnoreCase(detail)) {
				lResponse.putSubject(detail, "0.0.1");
			} else if ("provider".equalsIgnoreCase(detail)) {
				lResponse.putSubject(detail, "JamnWebRPCProvider");
			}
		}

		return lResponse;
	}

	/**
	 */
	public static class DetailsRequest {
		protected List<String> subjects = new ArrayList<>();

		List<String> getSubjects() {
			return subjects;
		}
	}

	/**
	 */
	public static class DetailsResponse {
		protected Map<String, String> details = new LinkedHashMap<>();

		public void putSubject(String pSubject, String pValue) {
			details.put(pSubject, pValue);
		}
	}

	/***************************************************************************************
	 **************************************************************************************/
}
