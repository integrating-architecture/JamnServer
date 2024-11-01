/* Authored by www.integrating-architecture.de */

package org.isa.ipc;

import static org.isa.ipc.JamnServer.HttpHeader.Field.CONTENT_TYPE;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_PLAIN;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_400_BAD_REQUEST;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_404_NOT_FOUND;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_405_METHOD_NOT_ALLOWED;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.HttpHeader;

/**
 * <pre>
 * This class realizes a sample Web Service Provider.
 *
 * Usage:
 *  ...
 *  
 *  // create a Jamn server
 *  JamnServer lServer = new JamnServer();
 * 
 *  // create a service provider
 *  JamnWebServiceProvider lWebServiceProvider = new JamnWebServiceProvider();
 *
 *  // register your web service class(es)
 *  lWebServiceProvider.registerServices(SampleWebApiServices.class);
 *
 *  // add the actual jamn-content-provider to the server
 *  lServer.addContentProvider("WebServiceProvider", lWebServiceProvider.getJamnContentProvider());
 *  
 *  ...
 *
 *
 * The data IO is by default based on JSON which is provided by the "com.fasterxml.jackson" library.
 * But you can simply fallback to pure String IO or plug in any other JSON or String based format converter.
 *
 * JamnWebServiceProvider.setJsonTool(new JamnWebServiceProvider.JsonToolWrapper() {
 * 		private final MyConverterClass MyConverter = new MyConverterClass();
 * 			Override
 * 			public <T> T toObject(String pSrc, Class<T> pType) throws Exception {
 * 				return MyConverter.getAsObject(pSrc, pType);
 * 			}
 * 			Override
 * 			public String toString(Object pObj) throws Exception {
 * 				return MyConverter.getAsString(pObj);
 * 			}
 * 		});
 * </pre>
 */
public class JamnWebServiceProvider {

    protected static final String LS = System.getProperty("line.separator");
    protected static Logger LOG = Logger.getLogger(JamnWebServiceProvider.class.getName());

    /****************************************************************************
     * IT IS MANDATORY TO SET A JSON-TOOL manually
     *
     * @see org.isa.ipc.sample.SampleWebServiceApp
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
     * A map holding all registered services.
     */
    protected Map<String, ServiceObject> registeredServices = new HashMap<>();

    /**
     * The actual JamnServer.ContentProvider implementation instance.
     */
    protected WebServiceContentProviderImpl jamnWebServiceContentProvider = new WebServiceContentProviderImpl();

    /**
     * IMPORTANT This method is NOT part of any interface contract. It is just used
     * to get the separated ContentProvider instance.
     */
    public JamnServer.ContentProvider getJamnContentProvider() {
        return jamnWebServiceContentProvider;
    }

    /**
     * The public interface method to register and install Services.
     */
    public JamnWebServiceProvider registerServices(Class<?> pServiceClass) throws WebServiceDefinitionException {
        ServiceObject lServiceObj = null;
        WebService lServiceAnno = null;
        Object lInstance = null;
        Class<?> lRequestClass = null;
        Class<?> lReponseClass = null;

        try {
            lInstance = pServiceClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new WebServiceDefinitionException(e);
        }

        Method[] lMethodes = pServiceClass.getDeclaredMethods();
        for (Method serviceMethod : lMethodes) {
            if (serviceMethod.isAnnotationPresent(WebService.class)) {
                lServiceAnno = serviceMethod.getDeclaredAnnotation(WebService.class);
                checkServiceAnnotation(lServiceAnno, serviceMethod);

                lRequestClass = getServiceRequestClassFrom(serviceMethod);
                lReponseClass = getServiceResponseClassFrom(serviceMethod);

                lServiceObj = new ServiceObject(lServiceAnno, lInstance, lRequestClass, lReponseClass, serviceMethod);
                if (!registeredServices.containsKey(lServiceObj.path)) {
                    registeredServices.put(lServiceObj.path, lServiceObj);
                    final String info = String.format("WebService installed [%s] at [%s]", lServiceObj.getName(),
                            lServiceObj.path);
                    LOG.fine(info);
                } else {
                    throw new WebServiceDefinitionException(String.format(
                            "WebService Path of [%s] already defined for [%s]", lServiceObj.getName(),
                            registeredServices.get(lServiceObj.path).getName()));
                }
            }
        }
        return this;
    }

    /*********************************************************
     * The public Annotation Interfaces to annotate methods as WebServices.
     *********************************************************/
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface WebService {
        public String path() default "/";

        public String[] methods() default { "GET, POST" };

        public String contentType() default APPLICATION_JSON;
    }

    /*********************************************************
     * Internal static helper methods.
     *********************************************************/
    /**
     */
    protected static String getStackTraceFrom(Throwable t) {
        return JamnServer.getStackTraceFrom(t);
    }

    /**
     */
    protected static String getServiceMethodName(Method pMeth) {
        return pMeth.getDeclaringClass().getSimpleName() + " - " + pMeth.getName();
    }

    /**
     */
    protected static void checkServiceAnnotation(WebService pServiceAnno, Method pMeth)
            throws WebServiceDefinitionException {
        if (pServiceAnno.path().isEmpty()) {
            throw new WebServiceDefinitionException(
                    String.format("No WebService path attribute found for [%s]",
                            getServiceMethodName(pMeth)));
        }
        if (pServiceAnno.methods().length == 0) {
            throw new WebServiceDefinitionException(String.format("No WebService methods attribute found for [%s]",
                    getServiceMethodName(pMeth)));
        }
    }

    /**
     */
    protected static Class<?> getServiceRequestClassFrom(Method pMeth)
            throws WebServiceDefinitionException {
        Class<?>[] lClasses = pMeth.getParameterTypes();
        if (lClasses.length == 1) {
            return lClasses[0];
        } else if (lClasses.length > 1) {
            throw new WebServiceDefinitionException(String.format(
                    "WebService method must declare 0 or 1 parameter [%s]",
                    getServiceMethodName(pMeth)));
        }
        return null;
    }

    /**
     */
    protected static Class<?> getServiceResponseClassFrom(Method pMeth) {
        return pMeth.getReturnType();
    }

    /*********************************************************
     * The internal classes for loading and providing WebService objects.
     *********************************************************/
    /**
     * The internal class that holds a Service Instance.
     */
    protected static class ServiceObject {
        protected Object instance = null;
        protected String path = "";
        protected String contentType = "";
        protected Map<String, String> httpMethods = new HashMap<>(4);
        protected Class<?> requestClass = null;
        protected Class<?> responseClass = null;
        protected Method serviceMethod = null;

        protected ServiceObject(WebService pServiceAnno, Object pInstance, Class<?> pRequestClass,
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
            return getName();
        }

        /**
         */
        public String getName() {
            return getServiceMethodName(serviceMethod);
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
        protected Object callWith(String pRequestData) throws Exception {
            Object lRet = null;
            Object lParam = null;

            if (getContentType().equalsIgnoreCase(APPLICATION_JSON)) {
                if (hasParameter()) {
                    lParam = JSON.toObject(pRequestData, requestClass);
                    lRet = serviceMethod.invoke(instance, lParam);
                } else {
                    lRet = serviceMethod.invoke(instance);
                }
                lRet = JSON.toString(lRet);
            } else if (getContentType().equalsIgnoreCase(TEXT_PLAIN)) {
                if (hasParameter() && requestClass == String.class) {
                    lRet = serviceMethod.invoke(instance, pRequestData);
                } else {
                    lRet = serviceMethod.invoke(instance);
                }
                if (responseClass == String.class) {
                    // if string defined return directly
                    return lRet;
                }
                // this surrounds a blank string with ""
                lRet = JSON.toString(lRet);
            }

            return lRet;
        }
    }

    /**
     * Exceptions thrown by this JamnWebServiceProvider implementation during
     * Service initialization/creation.
     */
    protected static class WebServiceDefinitionException extends Exception {
        private static final long serialVersionUID = 1L;

        WebServiceDefinitionException(String pMsg) {
            super(pMsg);
        }

        WebServiceDefinitionException(Throwable t) {
            super(t.getMessage(), t);
        }
    }

    /*********************************************************
     * The actual JamnServer.ContentProvider Interface Implementation.
     *********************************************************/
    protected class WebServiceContentProviderImpl implements JamnServer.ContentProvider {

        /**
         * This method realizes the service execution logic.
         */
        @Override
        public String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
                String pMethod, String pPath, String pRequestBody, Map<String, String> pRequestAttributes) {

            String lStatus = SC_200_OK;
            HttpHeader lRequest = new HttpHeader(pRequestAttributes);

            ServiceObject lService = null;
            Object lResult = null;
            byte[] lData = null;
            try {
                if (lRequest.isGET() || lRequest.isPOST()) {
                    lService = getServiceInstanceFor(pPath, pMethod, lRequest.getContentType());

                    lResult = lService.callWith(pRequestBody);

                    if (lResult instanceof String result) {
                        lData = result.getBytes();
                        pResponseAttributes.put(CONTENT_TYPE, lService.getContentType());
                        pResponseContent.write(lData);
                    } else {
                        throw new WebServiceException(SC_500_INTERNAL_ERROR,
                                String.format("Unsupported WebService API Return Type [%s] [%s]", lResult.getClass(),
                                        lService.getName()));
                    }
                }
            } catch (WebServiceException wse) {
                LOG.fine(() -> String.format("WebService API Error: [%s]", wse.getMessage()));
                lStatus = wse.getHttpStatus();
            } catch (Exception e) {
                String info = lService != null ? lService.getName() : "";
                info = info + LS + getStackTraceFrom(e);
                LOG.severe(String.format("WebService Request Handling internal/runtime ERROR: %s %s %s", e.toString(),
                        LS, info));
                lStatus = SC_500_INTERNAL_ERROR;
            }

            return lStatus;
        }

        /**
         */
        protected ServiceObject getServiceInstanceFor(String pPath, String pMethod, String pContentType)
                throws WebServiceException {
            ServiceObject lService = null;
            if (registeredServices.containsKey(pPath)) {
                lService = registeredServices.get(pPath);

                if (!lService.isMethodSupported(pMethod)) {
                    throw new WebServiceException(SC_405_METHOD_NOT_ALLOWED, String
                            .format("Unsupported WebService Method [%s] [%s]", pMethod, lService.getName()));
                }
                if (!lService.isContentTypeSupported(pContentType)) {
                    throw new WebServiceException(SC_400_BAD_REQUEST, String.format(
                            "Unsupported WebService ContentType [%s] [%s]", pContentType, lService.getName()));
                }
            } else {
                throw new WebServiceException(SC_404_NOT_FOUND,
                        String.format("Unsupported WebService Path [%s]", pPath));
            }
            return lService;
        }

        /**
         * Exceptions thrown by this WebServiceContentProviderImpl implementation during
         * Service execution.
         */
        protected static class WebServiceException extends Exception {
            private static final long serialVersionUID = 1L;
            private final String httpStatus;

            WebServiceException(String pHttpStatus, String pMsg) {
                super(pMsg);
                httpStatus = pHttpStatus;
            }

            public String getHttpStatus() {
                return httpStatus;
            }
        }
    }

    /*********************************************************
     * A wrapper interface for a JSON tool.
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
