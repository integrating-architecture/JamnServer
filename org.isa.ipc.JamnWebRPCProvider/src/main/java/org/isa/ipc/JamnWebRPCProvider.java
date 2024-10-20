/*Authored by www.integrating-architecture.de*/

package org.isa.ipc;

import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.isa.ipc.http.HttpHeader;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * This class realizes a sample web based RPC-Service Provider.
 * 
 * Via getJamnContentProvider it provides the plugin for the JamnServer.
 * 
 * A Service is implemented as a pojo class with annotations
 * - defining the web access properties
 * - a processing method
 * - and data objects for request and response
 * </pre>
 */
public class JamnWebRPCProvider implements JamnServer.HttpConstants {

	private static Logger LOG = null;

	private static final String LS = System.getProperty("line.separator");

	protected static final ObjectMapper JSON = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

	protected Map<String, RPCServiceObject> apiServices = new HashMap<>();

	/**
	 * The actual JamnServer.ContentProvider
	 */
	protected ContentProviderImpl jamnServerContentProviderImpl = new ContentProviderImpl();

	/**
	 */
	public JamnWebRPCProvider() {
		this(Logger.getLogger(JamnWebRPCProvider.class.getName()));
	}

	/**
	 */
	public JamnWebRPCProvider(Logger pLogger) {
		if (LOG == null) {
			LOG = pLogger;
		}
	}

	/**
	 * IMPORTANT
	 * This method is NOT part of a JamnServer provider interface.
	 * It is used to get the separated ContentProvider.  
	 */
	public JamnServer.ContentProvider getJamnContentProvider() {
		return jamnServerContentProviderImpl;
	}

	/**
	 * The providers interface to register and install Services.
	 */
	public JamnWebRPCProvider registerApiService(Class<?> pServiceClass) throws Exception {
		RPCServiceObject lServiceObj = null;
		WebRPCService lServiceAnno = null;
		Object lInstance = null;
		Class<?> lReponseClass = null;
		Method lProcessingMethod = null;

		if (pServiceClass.isAnnotationPresent(WebRPCService.class)) {
			lServiceAnno = pServiceClass.getDeclaredAnnotation(WebRPCService.class);
			ServiceHelper.checkServiceAnnotation(lServiceAnno, pServiceClass);

			lReponseClass = ServiceHelper.getServiceResponseClassFrom(pServiceClass);
			lProcessingMethod = ServiceHelper.getServiceProcessingMethodFrom(pServiceClass, lReponseClass);
			lInstance = pServiceClass.getDeclaredConstructor().newInstance();

			lServiceObj = new RPCServiceObject(lServiceAnno, lInstance, lReponseClass, lProcessingMethod);
			apiServices.put(lServiceObj.path, lServiceObj);
		} else {
			throw new ApiDefinitionException("No WebRPCService annotation found in [" + pServiceClass + "]");
		}
		return this;
	}

	/*********************************************************
	 * <pre>
	 * Annotation Interfaces to annotate classes as RPC Services.
	 * </pre>
	 *********************************************************/
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface WebRPCService {
		public String path() default "/";

		public String[] methods() default { "GET, POST" };

		public String contentType() default HTTPVAL_CONTENT_TYPE_JSON;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface Response {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Processor {
	}

	/*********************************************************
	 * <pre>
	 * The internal classes for loading and providing RPC Service objects.
	 * </pre>
	 *********************************************************/
	/**
	 * The central class that holds an Service Instance.
	 */
	protected static class RPCServiceObject {
		protected Object instance = null;
		protected String path = "";
		protected String contentType = "";
		protected Map<String, String> methods = new HashMap<>(4);
		protected Class<?> responseClass = null;
		protected Method processingMethod = null;

		protected RPCServiceObject(WebRPCService pServiceAnno, Object pInstance, Class<?> pResponseClass,
				Method pProcessingMethod) {
			instance = pInstance;
			path = pServiceAnno.path().trim();
			contentType = pServiceAnno.contentType().trim();
			responseClass = pResponseClass;
			processingMethod = pProcessingMethod;

			for (String meth : pServiceAnno.methods()) {
				methods.put(meth.toUpperCase(), meth.toUpperCase());
			}
		}

		/**
		 */
		public Class<?> getServiceClass() {
			return instance.getClass();
		}

		/**
		 */
		public boolean isMethodSupported(String pMethod) {
			return methods.containsKey(pMethod.toUpperCase());
		}

		/**
		 */
		public boolean isContentTypeSupported(String pContentType) {
			return (contentType.equalsIgnoreCase(pContentType) || pContentType.isEmpty());
		}

		/**
		 */
		public String getContentType() {
			return contentType;
		}

		/**
		 */
		public Object call() throws Exception {
			Object lRet = null;

			processingMethod.setAccessible(true);
			lRet = processingMethod.invoke(instance);

			// TODO
			// this is part of the RPCProvider contract
			// must be jason - does string make sense ?
			if (getContentType().equalsIgnoreCase(HTTPVAL_CONTENT_TYPE_JSON)) {
				lRet = JSON.writeValueAsString(lRet);
			}
			return lRet;
		}
	}

	/**
	 * An exception class for errors during api service creation.
	 */
	protected static class ApiDefinitionException extends Exception {
		private static final long serialVersionUID = 1L;

		ApiDefinitionException(String pMsg) {
			super(pMsg);
		}
	}

	/**
	 */
	protected static class ServiceHelper {
		/**
		 */
		protected static void checkServiceAnnotation(WebRPCService pServiceAnno, Class<?> pServiceClass) throws Exception {
			if (pServiceAnno.path().isEmpty()) {
				throw new ApiDefinitionException("No WebRPCService path attribute found in [" + pServiceClass + "]");
			}
			if (pServiceAnno.methods().length == 0) {
				throw new ApiDefinitionException("No WebRPCService methods attribute found in [" + pServiceClass + "]");
			}
		}

		/**
		 */
		protected static Method getServiceProcessingMethodFrom(Class<?> pServiceClass, Class<?> pResponseClass)
				throws Exception {
			Method[] lMethods = pServiceClass.getDeclaredMethods();

			for (Method meth : lMethods) {
				if (meth.isAnnotationPresent(Processor.class)) {
					if (meth.getReturnType() == pResponseClass) {
						return meth;
					}
				}
			}
			throw new ApiDefinitionException("No Processor method found in [" + pServiceClass + "]" + LS
					+ "The method MUST have return type [" + pResponseClass.getName() + "]");
		}

		/**
		 */
		protected static Class<?> getServiceResponseClassFrom(Class<?> pServiceClass) throws Exception {
			Class<?>[] lClasses = pServiceClass.getDeclaredClasses();

			for (Class<?> cls : lClasses) {
				if (cls.isAnnotationPresent(Response.class)) {
					return cls;
				}
			}
			throw new ApiDefinitionException("No Response class found in [" + pServiceClass + "]");
		}
	}

	/*********************************************************
	 * <pre>
	 * The actual JamnServer Content Provider for this RPC Service Provider.
	 * </pre>
	 *********************************************************/
	protected class ContentProviderImpl implements JamnServer.ContentProvider {

		@Override
		public String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
				String pMethod, String pPath, String pRequestBody, Map<String, String> pRequestAttributes) {

			String lStatus = HTTP_200_OK;
			HttpHeader lRequest = new HttpHeader(pRequestAttributes);

			RPCServiceObject lService = null;
			Object lResult = null;
			byte[] lData = null;
			try {
				if (lRequest.isGET() || lRequest.isPOST()) {
					lService = getServiceInstanceFor(pPath, pMethod, lRequest.getCONTENT_TYPE());
					lResult = lService.call();

					if (lResult instanceof String) {
						lData = ((String) lResult).getBytes();
						pResponseAttributes.put(HTTP_CONTENT_TYPE, lService.getContentType());
						pResponseContent.write(lData);
					} else {
						throw new ApiException(HTTP_500_INTERNAL_ERROR, "Unsupported API Return Type ["
								+ lResult.getClass() + "] [" + lService.getServiceClass() + "]");
					}
				}
			} catch (Exception e) {
				if (e instanceof ApiException) {
					LOG.fine("RPC API Error: [" + e.getMessage() + "]");
					lStatus = ((ApiException) e).getHttpStatus();
				} else {
					LOG.severe("Unexpected internal Error calling RPC API: [" + e.getMessage() + "]");
					lStatus = HTTP_500_INTERNAL_ERROR;
				}
			}

			return lStatus;
		}

		/**
		 */
		protected RPCServiceObject getServiceInstanceFor(String pPath, String pMethod, String pContentType)
				throws ApiException {
			RPCServiceObject lService = null;
			if (apiServices.containsKey(pPath)) {
				lService = apiServices.get(pPath);

				if (!lService.isMethodSupported(pMethod)) {
					throw new ApiException(HTTP_405_METHOD_NOT_ALLOWED,
							"Unsupported Service Method [" + pMethod + "] [" + lService.getServiceClass() + "]");
				}
				if (!lService.isContentTypeSupported(pContentType)) {
					throw new ApiException(HTTP_400_BAD_REQUEST,
							"Unsupported Service ContentType [" + pContentType + "] [" + lService.getServiceClass() + "]");
				}
			} else {
				throw new ApiException(HTTP_404_NOT_FOUND, "Unsupported Service Path [" + pPath + "]");
			}
			return lService;
		}

		/**
		 */
		protected static class ApiException extends Exception {
			private static final long serialVersionUID = 1L;
			private String httpStatus = HTTP_500_INTERNAL_ERROR;

			ApiException(String pHttpStatus, String pMsg) {
				super(pMsg);
				httpStatus = pHttpStatus;
			}

			public String getHttpStatus() {
				return httpStatus;
			}
		}
	}
}
