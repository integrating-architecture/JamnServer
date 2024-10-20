/*Authored by www.integrating-architecture.de*/

package org.isa.ipc.http;

import java.util.LinkedHashMap;
import java.util.Map;

import org.isa.ipc.JamnServer;


/**
 * Experimental class to deal with header attributes in a shorter form than with
 * constants. Just code format sugar.
 */
public class HttpHeader implements JamnServer.HttpConstants {

	protected Map<String, String> attributes = new LinkedHashMap<>();

	public HttpHeader(Map<String, String> pAttributes) {
		attributes = pAttributes;
	}

	public boolean isGET() {
		return attributes.getOrDefault(HTTP_METHOD, "").equalsIgnoreCase("GET");
	}

	public boolean isPOST() {
		return attributes.getOrDefault(HTTP_METHOD, "").equalsIgnoreCase("POST");
	}

	public boolean isOPTION() {
		return attributes.getOrDefault(HTTP_METHOD, "").equalsIgnoreCase("OPTION");
	}

	public boolean hasCONTENT_TYPE_JSON() {
		return attributes.getOrDefault(HTTP_CONTENT_TYPE, "").equalsIgnoreCase(HTTPVAL_CONTENT_TYPE_JSON);
	}

	public void setCONTENT_TYPE_JSON() {
		attributes.put(HTTP_CONTENT_TYPE, HTTPVAL_CONTENT_TYPE_JSON);
	}

	public boolean hasCONTENT_TYPE_HTML() {
		return attributes.getOrDefault(HTTP_CONTENT_TYPE, "").equalsIgnoreCase(HTTPVAL_CONTENT_TYPE_HTML);
	}

	public void setCONTENT_TYPE_HTML() {
		attributes.put(HTTP_CONTENT_TYPE, HTTPVAL_CONTENT_TYPE_HTML);
	}

	public void setCONTENT_TYPE_IMG_PNG() {
		attributes.put(HTTP_CONTENT_TYPE, HTTPVAL_CONTENT_TYPE_IMG_PNG);
	}

	public void setCONTENT_TYPE_IMG(String pPfx) {
		attributes.put(HTTP_CONTENT_TYPE, "image/" + pPfx);
	}

	public String getCONTENT_TYPE() {
		return attributes.getOrDefault(HTTP_CONTENT_TYPE, "");
	}

}
