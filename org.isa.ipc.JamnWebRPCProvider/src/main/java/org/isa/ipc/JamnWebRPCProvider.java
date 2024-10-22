/*Authored by www.integrating-architecture.de*/

package org.isa.ipc;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.isa.ipc.http.HttpHeader;

/**
 * <pre>
 * This class realizes a sample web based RPC-Service Provider.
 *
 * Usage:
 *  ... 
 *   //create the provider
 *   JamnWebRPCProvider lWebRPCProvider = new JamnWebRPCProvider(lServer.getLoggerFor(JamnWebRPCProvider.class.getName()));
 *   //register rpc api services
 *   lWebRPCProvider.registerApiService(SampleServerApiServices.class);
 *   //get the actual provider and add it to a server
 *   lServer.addContentProvider("RPCProvider", lWebRPCProvider.getJamnContentProvider());
 *  ...
 *
 * Services are implemented as classes with annotated service methods.
 * The data IO is based on JSON which is provided by the "com.fasterxml.jackson" library.
 * </pre>
 */
public class JamnWebRPCProvider implements JamnServer.HttpConstants {
	protected static final String LS = System.getProperty("line.separator");

	protected static Logger LOG = Logger.getLogger(JamnWebRPCProvider.class.getName());
	protected static CommonHelper Helper = new CommonHelper();

	/****************************************************************************
	 * IT IS MANDATORY TO SET A JSON-TOOL manually
	 * 
	 * @see org.isa.ipc.sample.SampleWebRPCServerApp
	 ***************************************************************************/
	protected static JsonToolWrapper JSON = null;
	public static void setJsonTool(JsonToolWrapper pTool) {
		JSON = pTool;
	}

	/**
	 * Logger is optional.
	 */
	public static void setLogger(Logger pLogger) {
		LOG = pLogger;
	}

	/**
	 * A map holding all registered services - which define an API.
	 */
	protected Map<String, RPCServiceObject> apiServices = new HashMap<>();

	/**
	 * The actual JamnServer.ContentProvider implementation.
	 */
	protected ContentProviderImpl jamnServerContentProviderImpl = new ContentProviderImpl();

	/**
	 */
	public JamnWebRPCProvider() {
	}

	/**
	 * IMPORTANT This method is NOT part of a JamnServer provider interface. It is
	 * used to get the separated ContentProvider.
	 */
	public JamnServer.ContentProvider getJamnContentProvider() {
		return jamnServerContentProviderImpl;
	}

	/**
	 * The interface method to register and install Services.
	 */
	public JamnWebRPCProvider registerApiService(Class<?> pServiceClass) throws Exception {
		RPCServiceObject lServiceObj = null;
		WebRPCService lServiceAnno = null;
		Object lInstance = null;
		Class<?> lRequestClass = null;
		Class<?> lReponseClass = null;

		lInstance = pServiceClass.getDeclaredConstructor().newInstance();

		Method[] lMethodes = pServiceClass.getDeclaredMethods();
		for (Method serviceMethod : lMethodes) {
			if (serviceMethod.isAnnotationPresent(WebRPCService.class)) {
				lServiceAnno = serviceMethod.getDeclaredAnnotation(WebRPCService.class);
				ServiceHelper.checkServiceAnnotation(lServiceAnno, serviceMethod, pServiceClass);

				lRequestClass = ServiceHelper.getServiceRequestClassFrom(serviceMethod, pServiceClass);
				lReponseClass = ServiceHelper.getServiceResponseClassFrom(serviceMethod, pServiceClass);

				lServiceObj = new RPCServiceObject(lServiceAnno, lInstance, lRequestClass, lReponseClass,
						serviceMethod);
				if (!apiServices.containsKey(lServiceObj.path)) {
					apiServices.put(lServiceObj.path, lServiceObj);
					LOG.fine("WebRPCService installed [" + lServiceObj.getSimpleName() + "] at [" + lServiceObj.path
							+ "]");
				} else {
					throw new ApiDefinitionException("WebRPCService Path of [" + lServiceObj.getSimpleName()
							+ "] already defined for [" + apiServices.get(lServiceObj.path).getSimpleName() + "]");
				}
			}
		}

		return this;
	}

	/*********************************************************
	 * <pre>
	 * The Annotation Interfaces to annotate classes as RPC Services.
	 * </pre>
	 *********************************************************/
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface WebRPCService {
		public String path() default "/";

		public String[] methods() default { "GET, POST" };

		public String contentType() default HTTPVAL_CONTENT_TYPE_JSON;
	}

	/*********************************************************
	 * <pre>
	 * The internal classes for loading and providing RPC Service objects.
	 * </pre>
	 *********************************************************/
	/**
	 * The central internal class that holds an Service Instance.
	 */
	protected static class RPCServiceObject {
		protected Object instance = null;
		protected String path = "";
		protected String contentType = "";
		protected Map<String, String> httpMethods = new HashMap<>(4);
		protected Class<?> requestClass = null;
		protected Class<?> responseClass = null;
		protected Method serviceMethod = null;

		protected RPCServiceObject(WebRPCService pServiceAnno, Object pInstance, Class<?> pRequestClass,
				Class<?> pResponseClass, Method pServiceMethod) {
			instance = pInstance;
			path = pServiceAnno.path().trim();
			contentType = pServiceAnno.contentType().trim();
			requestClass = pRequestClass;
			responseClass = pResponseClass;
			serviceMethod = pServiceMethod;
			serviceMethod.setAccessible(true);

			for (String meth : pServiceAnno.methods()) {
				httpMethods.put(meth.toUpperCase(), meth.toUpperCase());
			}
		}

		/**
		 */
		@Override
		public String toString() {
			return getSimpleName();
		}

		/**
		 */
		public String getSimpleName() {
			return ServiceHelper.getSimpleName(getServiceClass(), serviceMethod);
		}

		/**
		 */
		public Class<?> getServiceClass() {
			return instance.getClass();
		}

		/**
		 */
		public boolean isMethodSupported(String pMethod) {
			return httpMethods.containsKey(pMethod.toUpperCase());
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
		public boolean hasParameter() {
			return (requestClass != null);
		}

		/**
		 */
		public Object callWith(String pRequestData) throws Exception {
			Object lRet = null;
			Object lParam = null;

			if (getContentType().equalsIgnoreCase(HTTPVAL_CONTENT_TYPE_JSON)) {
				if (hasParameter()) {
					lParam = JSON.toObject(pRequestData, requestClass);
					lRet = serviceMethod.invoke(instance, lParam);
				} else {
					lRet = serviceMethod.invoke(instance);
				}

				lRet = JSON.toString(lRet);
			} else if (getContentType().equalsIgnoreCase(HTTPVAL_CONTENT_TYPE_TEXT)) {
				if (hasParameter() && requestClass == String.class) {
					lRet = serviceMethod.invoke(instance, pRequestData);
				} else {
					lRet = serviceMethod.invoke(instance);
				}

				lRet = JSON.toString(lRet);
			}

			return lRet;
		}
	}

	/**
	 * Exceptions thrown by this JamnWebRPCProvider implementation during
	 * Service-API initialization/creation.
	 */
	protected static class ApiDefinitionException extends Exception {
		private static final long serialVersionUID = 1L;

		ApiDefinitionException(String pMsg) {
			super(pMsg);
		}
	}

	/**
	 * Internal Helper to create and manage the service objects.
	 */
	protected static class ServiceHelper {
		/**
		 */
		protected static String getSimpleName(Class<?> pServiceClass, Method pMeth) {
			return pServiceClass.getSimpleName() + " - " + pMeth.getName();
		}

		/**
		 */
		protected static void checkServiceAnnotation(WebRPCService pServiceAnno, Method pMeth, Class<?> pServiceClass)
				throws Exception {
			if (pServiceAnno.path().isEmpty()) {
				throw new ApiDefinitionException(
						"No WebRPCService path attribute found for [" + getSimpleName(pServiceClass, pMeth) + "]");
			}
			if (pServiceAnno.methods().length == 0) {
				throw new ApiDefinitionException(
						"No WebRPCService methods attribute found for [" + getSimpleName(pServiceClass, pMeth) + "]");
			}
		}

		/**
		 */
		protected static Class<?> getServiceRequestClassFrom(Method pMeth, Class<?> pServiceClass) throws Exception {
			Class<?>[] lClasses = pMeth.getParameterTypes();
			if (lClasses.length == 1) {
				return lClasses[0];
			} else if (lClasses.length > 1) {
				throw new ApiDefinitionException("WebRPCService method must declare 0 or 1 parameter ["
						+ getSimpleName(pServiceClass, pMeth) + "]");
			}
			return null;
		}

		/**
		 */
		protected static Class<?> getServiceResponseClassFrom(Method pMeth, Class<?> pServiceClass) throws Exception {
			Class<?> lCls = pMeth.getReturnType();
			return lCls;
		}
	}

	/*********************************************************
	 * <pre>
	 * The actual JamnServer.ContentProvider Interface Implementation for this RPC Service Provider.
	 * </pre>
	 *********************************************************/
	protected class ContentProviderImpl implements JamnServer.ContentProvider {

		/**
		 * This method realizes the service execution logic.
		 */
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

					lResult = lService.callWith(pRequestBody);

					if (lResult instanceof String) {
						lData = ((String) lResult).getBytes();
						pResponseAttributes.put(HTTP_CONTENT_TYPE, lService.getContentType());
						pResponseContent.write(lData);
					} else {
						throw new ApiException(HTTP_500_INTERNAL_ERROR, "Unsupported API Return Type ["
								+ lResult.getClass() + "] [" + lService.getSimpleName() + "]");
					}
				}
			} catch (Exception e) {
				if (e instanceof ApiException) {
					LOG.fine("RPC API Error: [" + e.getMessage() + "]");
					lStatus = ((ApiException) e).getHttpStatus();
				} else {
					String lInfo = lService != null ? lService.getSimpleName() : "";
					lInfo = lInfo + LS + Helper.getStackTraceFrom(e);
					LOG.severe("RPC Request Handling internal/runtime ERROR: " + e.toString() + LS + lInfo);
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
							"Unsupported Service Method [" + pMethod + "] [" + lService.getSimpleName() + "]");
				}
				if (!lService.isContentTypeSupported(pContentType)) {
					throw new ApiException(HTTP_400_BAD_REQUEST, "Unsupported Service ContentType [" + pContentType
							+ "] [" + lService.getSimpleName() + "]");
				}
			} else {
				throw new ApiException(HTTP_404_NOT_FOUND, "Unsupported Service Path [" + pPath + "]");
			}
			return lService;
		}

		/**
		 * Exceptions thrown by this ContentProviderImpl implementation during
		 * Service-API execution.
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

	/*********************************************************
	 * <pre>
	 * A common helper class - just to encapsulate helper methods.
	 * </pre>
	 *********************************************************/
	/**
	 */
	protected static class CommonHelper {

		/**
		 */
		public String getStackTraceFrom(Throwable t) {
			StringWriter lSwriter = new StringWriter();
			PrintWriter lPwriter = new PrintWriter(lSwriter);

			if (t instanceof InvocationTargetException) {
				t = ((InvocationTargetException) t).getTargetException();
			}

			t.printStackTrace(lPwriter);
			return lSwriter.toString();
		}

	}

	/*********************************************************
	 * <pre>
	 * A wrapper interface for a JSON tool.
	 * </pre>
	 *********************************************************/
	/**
	 */
	public static interface JsonToolWrapper {
		/**
		 */
		public <T> T toObject(String pSrc, Class<T> pType) throws Exception;

		/**
		 */
		public String toString(Object pObj) throws Exception;
	}

}
