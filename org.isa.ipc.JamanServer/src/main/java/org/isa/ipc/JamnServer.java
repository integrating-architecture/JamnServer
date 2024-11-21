/* Authored by www.integrating-architecture.de */

package org.isa.ipc;

import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_METHODS;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.isa.ipc.JamnServer.HttpHeader.Field.CONTENT_LENGTH;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_1_0;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_METHOD;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_PATH;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_VERSION;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_VERSION_MARK;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_HEADERS_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_METHODS_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_ORIGIN_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_204_NO_CONTENT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_408_TIMEOUT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * <pre>
 * Just another micro node Server
 *
 * Jamn is an experimental, lightweight socket-based text data server
 * designed for easy startup, independence and easy customization.
 *
 * The purpose is text data based interprocess communication.
 *
 * The structure consists of the following plugable components:
 * - listener thread with a socket and multi-threaded connection setup
 * - request processor
 * - content provider
 * - http helper
 *
 * All of this components can be easily changed or replaced.
 *
 * HTTP is supported in such a way,
 * that the Jamn-DefaultRequestProcessor can read incoming HTTP Header/Body messages
 * and responds with an so fare appropriate HTTP message.
 *
 * IMPORTANT:
 * How ever - Jamn IS NOT a HTTP/Web Server Implementation - this is NOT intended.
 * Jamn does NOT offer complete support for the HTTP protocol.
 * 
 * HINT to CORS:
 * Cross-Origin-Resource-Sharing:
 * For using e.g. Html/JavaScript pages/codes in a Browser 
 * that come from files e.g. like the ones included in this source code
 * the used Server must be set to CORS Enabled = true
 *  - lServer.getConfig().setCORSEnabled(true)
 * that is because the browser addresses the server e.g. at http//:localhost:8099
 * and the used files come from e.g. something like c:/../../js-fetch-with-json-arg.html
 * not from http//:localhost:8099 - which would be the origin.
 *
 * </pre>
 */
public class JamnServer {

    public static final String JamnServerWebID = "JamnServer/0.0.1";

    // initialize logging from properties file
    static {
        try {
            LogManager.getLogManager().readConfiguration(JamnServer.class.getResourceAsStream("/logging.properties"));
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
            throw new JamnRuntimeException(e);
        }
    }

    private static final Logger LOG = Logger.getLogger(JamnServer.class.getName());

    public static final String LS = System.getProperty("line.separator");
    public static final String CRLF = "\r\n";

    /*********************************************************
     * <pre>
     * The actual socket based Server.
     * </pre>
     *********************************************************/
    protected static HttpHelper HttpHelper = new HttpHelper();

    protected Config config = new Config();

    protected ServerSocket serverSocket = null;
    protected ExecutorService requestExecutor = null;

    protected int clientSocketTimeout = 10000;
    protected ServerThread serverThread = null;
    Supplier<ServerThread> serverThreadSupplier = ServerThread::new;
    protected RequestProcessor requestProcessor = new DefaultRequestProcessor();

    public JamnServer() {
        // default port in config is: 8099
    }

    public JamnServer(int pPort) {
        this();
        getConfig().setPort(pPort);
    }

    public JamnServer(Properties pProps) {
        this();
        config = new Config(pProps);
    }

    /**
     */
    public static void main(String[] pArgs) {
        JamnServer lServer = new JamnServer();
        // required for using Testfiles in a browser
        lServer.getConfig().setCORSEnabled(true);
        lServer.start();
    }

    /**
     */
    public synchronized void start() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return;
        }

        clientSocketTimeout = config.getClientSocketTimeout();
        requestProcessor.setProperty(Config.HTTP_CORS_ENABLED, String.valueOf(getConfig().isCORSEnabled()));

        serverThread = serverThreadSupplier.get();
        serverThread.setName(getClass().getSimpleName() + " - on Port [" + config.getPort() + "]");

        try {
            serverSocket = createServerSocket();

            if (requestExecutor == null) {
                requestExecutor = Executors.newFixedThreadPool(config.getWorkerNumber());
            }

            serverThread.start();

            LOG.info(() -> String.format("JamnServer Instance STARTED: [%s]%s", config, LS));
        } catch (Exception e) {
            LOG.severe(() -> String.format("ERROR starting JamnServer: [%s]%s%s", e.getMessage(), LS,
                    getStackTraceFrom(e)));
            stop();
        }
    }

    /**
     */
    public synchronized void stop() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // OK this is specified
            }
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.shutdown();
        }
        if (requestExecutor != null) {
            requestExecutor.shutdownNow();
        }

        LOG.info(LS + "JamnServer STOPPED");
    }

    /**
     */
    public boolean isRunning() {
        return (serverSocket != null && !serverSocket.isClosed());
    }

    /**
     */
    public Config getConfig() {
        return config;
    }

    /*********************************************************
     * <pre>
     * The customization methods for the server components.
     * </pre>
     *********************************************************/

    /**
     */
    public JamnServer setRequestProcessor(RequestProcessor pProcessor) {
        requestProcessor = pProcessor;
        return this;
    }

    /**
     */
    public JamnServer addContentProvider(ContentProvider pProvider) {
        requestProcessor.addContentProvider(pProvider.getClass().getSimpleName(), pProvider);
        return this;
    }

    /**
     */
    public JamnServer setContentProviderDispatcher(ContentProviderDispatcher pProviderDispatcher) {
        requestProcessor.setContentProviderDispatcher(pProviderDispatcher);
        return this;
    }

    /**
     * Supplier for a customized server thread implementation.
     */
    public JamnServer setServerThreadSupplier(Supplier<ServerThread> pServerThreadSupplier) {
        this.serverThreadSupplier = pServerThreadSupplier;
        return this;
    }

    /**
     */
    public JamnServer setExecutorService(ExecutorService pService) {
        this.requestExecutor = pService;
        return this;
    }

    /**
     */
    public static void setHttpHelper(HttpHelper pHelper) {
        HttpHelper = pHelper;
    }

    /**
     */
    protected ServerSocket createServerSocket() throws Exception {
        ServerSocket lSocket = null;

        if (!System.getProperty("javax.net.ssl.keyStore", "").isEmpty()
                && !System.getProperty("javax.net.ssl.keyStorePassword", "").isEmpty()) {
            lSocket = SSLServerSocketFactory.getDefault().createServerSocket(config.getPort());
        } else {
            lSocket = ServerSocketFactory.getDefault().createServerSocket(config.getPort());
        }

        lSocket.setReuseAddress(true);
        return lSocket;
    }

    /*********************************************************
     * <pre>
     * The top level Server socket listener Thread.
     * This thread listens for client connection requests
     * and then starts a worker thread via the requestExecutor.
     * The worker then delegates the processing to the current requestProcessor.
     * </pre>
     *********************************************************/
    /**
     */
    public class ServerThread extends Thread {
        private volatile boolean run = true;

        public synchronized void shutdown() {
            run = false;
        }

        @Override
        public void run() {
            Map<String, String> lComData = new HashMap<>(2);

            try {
                ServerSocket lServerSocket = serverSocket; // keep local

                while (lServerSocket != null && !lServerSocket.isClosed()) {
                    if (!run) {
                        break;
                    }

                    final Socket lClientSocket = lServerSocket.accept();

                    requestExecutor.execute(() -> {
                        try {
                            long start = System.currentTimeMillis();
                            try {
                                lComData.clear();
                                lClientSocket.setSoTimeout(clientSocketTimeout);
                                lClientSocket.setTcpNoDelay(true);
                                requestProcessor.handleRequest(lClientSocket.getInputStream(),
                                        lClientSocket.getOutputStream(), lClientSocket, lComData);

                            } finally {
                                try {
                                    if (!(lClientSocket instanceof SSLSocket)) {
                                        lClientSocket.shutdownOutput(); // first step only output
                                    }
                                } finally {
                                    lClientSocket.close();
                                    lComData.clear();
                                    LOG.fine(() -> String.format("ClientSocket closed: %s - %s",
                                            (System.currentTimeMillis() - start), Thread.currentThread().getName()));
                                }
                            }
                        } catch (IOException e) {
                            // nothing to do
                        }
                    });
                }
            } catch (IOException e) {
                // nothing to do
            } finally {
                LOG.fine(() -> String.format("ServerThread finished: %s", Thread.currentThread().getName()));
            }
        }
    }

    /*********************************************************
     * <pre>
     * The central processing interfaces and default implementations.
     * The interfaces use only themselves or standard Java data types.
     * - RequestProcessor
     * - ContentProvider
     * Chain: ServerThread -> RequestProcessor -> ContentProvider
     * </pre>
     *********************************************************/
    /**
     * The Interface that is used by the RequestProcessor to create the data content
     * for a request.
     */
    public static interface ContentProvider {
        /**
         * @param pResponseAttributes - the response header attributes/fields
         * @param pResponseContent    - the response content (body)
         * @param pMethod             - the given request method
         * @param pPath               - the given request path
         * @param pRequestBody        - the given request body content
         * @param pRequestAttributes  - the given request header attributes/fields
         * @return status - http
         */
        String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
                String pMethod, String pPath, String pRequestBody, final Map<String, String> pRequestAttributes);
    }

    /**
     * The Interface that is used by the server socket thread to delegate the
     * processing of an incoming client connection/request.
     */
    public static interface RequestProcessor {
        /**
         * The central interface method called from the socket based main thread.
         * 
         * @param pIn      - the socket input stream
         * @param pOut     - the socket output stream
         * @param pSocket
         * @param pComData - internal communication data (not used yet)
         * @throws IOException
         */
        void handleRequest(InputStream pIn, OutputStream pOut, Socket pSocket, Map<String, String> pComData)
                throws IOException;

        /**
         * The interface to set the content provider that creates the use case specific
         * response content.
         */
        void addContentProvider(String pId, ContentProvider pProvider);

        /**
         */
        default void setContentProviderDispatcher(ContentProviderDispatcher pProviderDispatcher) {
        }

        /**
         */
        default RequestProcessor setProperty(String pKey, String pValue) {
            return this;
        }
    }

    /**
     * <pre>
     * You can add different content providers.
     * If there is more than one provider,
     * a dispatcher must decide where requests are delegated to.
     * </pre>
     */
    public static interface ContentProviderDispatcher {
        /**
         */
        String getContentProviderIDFor(final Map<String, String> pRequestAttributes);
    }

    /**
     * <pre>
     * The Default RequestProcessor implementation using an empty ContentProvider.
     * This level is HTTP oriented and implements the basic HTTP capability.
     * </pre>
     */
    public static class DefaultRequestProcessor implements RequestProcessor {
        protected Properties props = new Properties();
        protected boolean isCORSEnabled = false;

        // the available ContentProvider
        protected Map<String, ContentProvider> contentProviderMap = new HashMap<>();

        // an empty default provider dispatcher
        protected ContentProviderDispatcher contentDispatcher = (Map<String, String> pRequestAttributes) -> {
            LOG.warning(() -> String.format(
                    "WARNING - The empty DEFAULT ContentProvider-Dispatcher was invoked although there are [%s] provider registered.%s",
                    contentProviderMap.size(), LS));
            return null;
        };

        // an empty default provider
        // just returning status SC_204_NO_CONTENT
        protected ContentProvider defaultContentProvider = (Map<String, String> pResponseAttributes,
                OutputStream pResponseContent, String pMethod, String pPath, String pRequestBody,
                final Map<String, String> pRequestAttributes) -> SC_204_NO_CONTENT;

        /**
         */
        protected ContentProvider getContentProviderFor(final Map<String, String> pRequestAttributes) {
            if (contentProviderMap.isEmpty()) {
                return defaultContentProvider;
            }
            // to avoid the need for a dispatcher for only one provider
            if (contentProviderMap.size() == 1) {
                return contentProviderMap.values().iterator().next();
            }
            // else use dispatcher
            String providerId = contentDispatcher.getContentProviderIDFor(pRequestAttributes);
            return contentProviderMap.getOrDefault(providerId, defaultContentProvider);
        }

        /**
         */
        @Override
        public void setContentProviderDispatcher(ContentProviderDispatcher pProviderDispatcher) {
            contentDispatcher = pProviderDispatcher;
        }

        /**
         */
        @Override
        public void addContentProvider(String pId, ContentProvider pProvider) {
            contentProviderMap.put(pId, pProvider);
        }

        /**
         */
        @Override
        public RequestProcessor setProperty(String pKey, String pValue) {
            props.setProperty(pKey, pValue);

            isCORSEnabled = Boolean.valueOf(props.getProperty(Config.HTTP_CORS_ENABLED, "false"));
            return this;
        }

        /**
         * The default handling. Read an incoming http request and reply a basic http
         * response.
         */
        @Override
        public void handleRequest(InputStream pSocketInStream, OutputStream pSocketOutStream, Socket pSocket,
                Map<String, String> pComData) throws IOException {

            ContentProvider lContentProvider = null;
            InputStream lInStream = new BufferedInputStream(pSocketInStream, 4096);
            OutputStream lOutStream = new BufferedOutputStream(pSocketOutStream, 4096);
            ByteArrayOutputStream lResponseContent = new ByteArrayOutputStream();

            String lStatus = "";
            Map<String, String> lResponseAttributes = new HashMap<>(5);

            Request lRequest = new Request();
            Response lResponse = new Response(lOutStream, isCORSEnabled);

            try {
                lRequest.readHeader(lInStream);
                lRequest.readBody(lInStream);

                lContentProvider = getContentProviderFor(lRequest.getHeaderAttributes());
                lStatus = lContentProvider.createResponseContent(lResponseAttributes, lResponseContent,
                        lRequest.getMethod(), lRequest.getPath(), lRequest.getBody(), lRequest.getHeaderAttributes());

                lResponse.setHttpStatus(lStatus).applyAttributes(lResponseAttributes).setContent(lResponseContent);

                lResponse.send();

            } catch (InterruptedIOException e) {
                lResponse.sendStatus(SC_408_TIMEOUT);
            } catch (RuntimeException e) {
                lResponse.sendStatus(SC_500_INTERNAL_ERROR);
                LOG.severe(() -> String.format("Request Handling internal/runtime ERROR: %s %s %s", e.toString(), LS,
                        getStackTraceFrom(e)));
            } finally {
                lResponse.close();
            }
        }

        /**
         * <pre>
         * Example header
         * HTTP/1.0 200 OK
         * Content-Type: text/plain
         * Content-Length: 39
         *
         * ...
         * </pre>
         */
        public static class Response {
            protected String[] statusline = new String[] { HTTP_1_0, "" };
            protected Map<String, String> headerAttributes = new LinkedHashMap<>(
                    HttpHeader.HTTP_DEFAULT_RESPONSE_ATTRIBUTES);

            protected OutputStream outStream = null;
            protected ByteArrayOutputStream contentBuffer = null;

            public Response(OutputStream pOutStream, boolean pIsCORSEnabled) {
                outStream = pOutStream;
                setHttpStatus(SC_408_TIMEOUT);
                setCORSEnabled(pIsCORSEnabled);
            }

            public Response setHttpVersion(String pVal) {
                statusline[0] = pVal;
                return this;
            }

            public Response setHttpStatus(Object pVal) {
                String lStatus = HttpHelper.getHttpStatus(pVal);
                if (!lStatus.isEmpty()) {
                    statusline[1] = lStatus;
                }
                return this;
            }

            public Response applyAttributes(Map<String, String> pAttributes) {
                headerAttributes.putAll(pAttributes);
                return this;
            }

            public Response setCORSEnabled(boolean pFlag) {
                if (pFlag) {
                    headerAttributes.put(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_ALL);
                    headerAttributes.put(ACCESS_CONTROL_ALLOW_METHODS, ACCESS_CONTROL_ALLOW_METHODS_ALL);
                    headerAttributes.put(ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_ALLOW_HEADERS_ALL);
                }
                return this;
            }

            public Response setContent(ByteArrayOutputStream pValue) {
                contentBuffer = pValue;
                return this;
            }

            private void send(boolean pWithContent) throws IOException {
                ByteArrayOutputStream lResponseMsg = new ByteArrayOutputStream();
                StringBuilder lHeader = null;
                int lContentLen = (contentBuffer != null && pWithContent) ? contentBuffer.size() : 0;

                if (pWithContent && lContentLen > 0) {
                    headerAttributes.put(CONTENT_LENGTH, String.valueOf(lContentLen));
                }

                lHeader = HttpHelper.createHttpHeader(statusline, headerAttributes);
                lResponseMsg.write(lHeader.toString().getBytes());

                if (pWithContent && lContentLen > 0) {
                    lResponseMsg.write(contentBuffer.toByteArray());
                    contentBuffer.flush();
                }
                outStream.write(lResponseMsg.toByteArray());
                lResponseMsg.flush();
            }

            public void send() throws IOException {
                send(true);
            }

            public void sendStatus(String pStatus) throws IOException {
                setHttpStatus(pStatus);
                send(false);
            }

            public void close() throws IOException {
                outStream.flush();
            }
        }

        /**
         */
        public static class Request {
            protected Map<String, String> headerAttributes = new LinkedHashMap<>();
            protected String header = "";
            protected String body = "";

            /**
             */
            public void readHeader(InputStream pInStream) throws IOException {
                header = HttpHelper.readHttpRequestHeader(pInStream).toString();
                headerAttributes = Collections.unmodifiableMap(HttpHelper.parseHttpHeader(header));

                LOG.fine(() -> String.format("%s - Request header: %s", Thread.currentThread().getName(), header));
            }

            /**
             * <pre>
             * This implementation is not http compliant and may also be incorrect.
             * However - it works with the standard java se client.
             * While None blocking at this point does NOT work reliable because the
             * JamnServer does NOT support "splitted" requests.
             * The JamnServer treats a request as a closed "transaction".
             * </pre>
             */
            public void readBody(InputStream pInStream) throws IOException {
                // try to read the body until socket timeout
                readBody(pInStream, -1);
            }

            /**
             * <pre>
             * Tries to blocking read the request body from the socket InputStream
             * until global socket timeout
             * or until a given local timeout value is reached.
             * </pre>
             */
            protected void readBody(InputStream pInStream, long pTimeout) throws IOException {
                int lContentLength = getContentLength();
                long lStart = System.currentTimeMillis();

                if (lContentLength > 0) {
                    StringBuilder lBuffer;
                    while (true) {
                        lBuffer = HttpHelper.readHttpRequestBody(pInStream, lContentLength);
                        // accept only complete bodies
                        if (lBuffer.length() == lContentLength) {
                            body = lBuffer.toString();
                            LOG.fine(() -> String.format("Body read time: %s", System.currentTimeMillis() - lStart));
                            break;
                        }
                        // if local timeout elapsed
                        if (pTimeout > 0 && (System.currentTimeMillis() - lStart) > pTimeout) {
                            int length = lBuffer.length();
                            LOG.severe(() -> String.format(
                                    "Could NOT read Request Body in time: [%s] - expected length [%s] read length [%s]",
                                    System.currentTimeMillis() - lStart, lContentLength, length));
                            throw new IOException("Could NOT read Request Body from socket stream.");
                        }
                    }
                }
            }

            /**
             */
            public Map<String, String> getHeaderAttributes() {
                return headerAttributes;
            }

            /**
             */
            public String getBody() {
                return body;
            }

            /**
             */
            public String getPath() {
                return getHeaderAttribute(HTTP_PATH, "");
            }

            /**
             */
            public String getMethod() {
                return getHeaderAttribute(HTTP_METHOD, "");
            }

            /**
             */
            public int getContentLength() {
                return Integer.valueOf(getHeaderAttribute(CONTENT_LENGTH, "0"));
            }

            /**
             */
            public String getHeaderAttribute(String pName, String pDefault) {
                return headerAttributes.getOrDefault(pName, pDefault);
            }
        }
    }

    /**
     * <pre>
     * The class encapsulates HTTP header handling.
     * In particular, it provides a selection of constants for status codes and header fields.
     * In addition, it serves as a wrapper around a map with key/value pairs for header fields.
     * </pre>
     */
    public static class HttpHeader {

        protected Map<String, String> fieldMap = new LinkedHashMap<>();

        public HttpHeader() {
        }

        public HttpHeader(Map<String, String> pAttributes) {
            fieldMap = pAttributes;
        }

        /**
         */
        public static final Map<String, String> HTTP_DEFAULT_RESPONSE_ATTRIBUTES;
        static {
            Map<String, String> lMap = new HashMap<>();
            lMap.put(Field.SERVER, JamnServerWebID);
            lMap.put(Field.CONTENT_TYPE, FieldValue.TEXT_PLAIN);
            lMap.put(Field.CONTENT_LENGTH, "0");
            HTTP_DEFAULT_RESPONSE_ATTRIBUTES = Collections.unmodifiableMap(lMap);
        }

        /**
         * HTTP status codes.
         */
        public static class Status {
            protected Status() {
            }

            public static final String SC_200_OK = "200";
            public static final String SC_204_NO_CONTENT = "204";
            public static final String SC_400_BAD_REQUEST = "400";
            public static final String SC_404_NOT_FOUND = "404";
            public static final String SC_405_METHOD_NOT_ALLOWED = "405";
            public static final String SC_408_TIMEOUT = "408";
            public static final String SC_500_INTERNAL_ERROR = "500";

            public static final Map<String, String> TEXT;
            static {
                Map<String, String> lMap = new HashMap<>();
                lMap.put("200", "OK");
                lMap.put("201", "Created");
                lMap.put("204", "No Content");
                lMap.put("400", "Bad Request");
                lMap.put("404", "Not found");
                lMap.put("405", "Method Not Allowed");
                lMap.put("406", "Not Acceptable");
                lMap.put("408", "Request Timeout");
                lMap.put("411", "Length Required");
                lMap.put("500", "Internal Server Error");
                lMap.put("503", "Service Unavailable");
                TEXT = Collections.unmodifiableMap(lMap);
            }
        }

        /**
         * HTTP header field identifier.
         */
        public static class Field {
            protected Field() {
            }

            public static final String HTTP_1_0 = "HTTP/1.0";
            public static final String HTTP_1_1 = "HTTP/1.1";

            // statusline attributes
            public static final String HTTP_METHOD = "http-method";
            public static final String HTTP_PATH = "http-path";
            public static final String HTTP_STATUS = "http-status";
            public static final String HTTP_VERSION = "http-version";
            public static final String HTTP_VERSION_MARK = "HTTP/";

            // header field attributes
            public static final String SERVER = "Server";
            public static final String CONTENT_LENGTH = "Content-Length";
            public static final String CONTENT_TYPE = "Content-Type";
            public static final String CONNECTION = "Connection";
            public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
            public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
            public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
        }

        /**
         * HTTP header field values.
         */
        public static class FieldValue {
            protected FieldValue() {
            }

            public static final String CLOSE = "close";
            public static final String TEXT = "text/";
            public static final String TEXT_PLAIN = "text/plain";
            public static final String TEXT_XML = "text/xml";
            public static final String TEXT_HTML = "text/html";
            public static final String TEXT_CSS = "text/css";
            public static final String TEXT_JS = "text/javascript";
            public static final String APPLICATION_JSON = "application/json";
            public static final String IMAGE = "image/";
            public static final String IMAGE_PNG = "image/png";
            public static final String IMAGE_X_ICON = "image/x-icon";
            public static final String ACCESS_CONTROL_ALLOW_ORIGIN_ALL = "*";
            public static final String ACCESS_CONTROL_ALLOW_METHODS_ALL = "GET, POST, PATCH, PUT, DELETE, OPTIONS";
            public static final String ACCESS_CONTROL_ALLOW_HEADERS_ALL = "Origin, Content-Type, X-Auth-Token";
        }

        /**
         */
        protected static boolean equalsOrContains(String pAttributeVal, String pVal) {
            return (pAttributeVal.equalsIgnoreCase(pVal) || pAttributeVal.toLowerCase().contains(pVal.toLowerCase()));
        }

        /**
         */
        public boolean has(String pKey, String pVal) {
            return equalsOrContains(fieldMap.getOrDefault(pKey, ""), pVal);
        }

        /**
         */
        public HttpHeader set(String pKey, String pVal) {
            fieldMap.put(pKey, pVal);
            return this;
        }

        /**
         */
        public String get(String pKey) {
            return fieldMap.getOrDefault(pKey, "");
        }

        /**
         */
        public String getContentType() {
            return get(Field.CONTENT_TYPE);
        }

        /**
         */
        public boolean hasContentType(String pVal) {
            return has(Field.CONTENT_TYPE, pVal);
        }

        /**
         */
        public HttpHeader setContentType(String pVal) {
            return set(Field.CONTENT_TYPE, pVal);
        }

        /**
         */
        public String getMethod() {
            return fieldMap.getOrDefault(Field.HTTP_METHOD, "");
        }

        /**
         */
        public String getPath() {
            return fieldMap.getOrDefault(Field.HTTP_PATH, "");
        }

        /**
         */
        public boolean isGET() {
            return getMethod().equalsIgnoreCase("GET");
        }

        /**
         */
        public boolean isPOST() {
            return getMethod().equalsIgnoreCase("POST");
        }

        /**
         */
        public boolean isOPTION() {
            return getMethod().equalsIgnoreCase("OPTION");
        }

    }

    /**
     * <pre>
     * This class implements a few central functions for the HTTP specific processing of a client request.
     * In particular, reading the input stream and parsing the header informations.
     * These methods are easily customizable with your own helper.
     *  - setHttpHelper( MyHelper extends HttpHelper )
     * </pre>
     */
    public static class HttpHelper {

        /**
         * Starting at the beginning of the stream reading until a blank/empty line is
         * detected.
         */
        public StringBuilder readHttpRequestHeader(InputStream pInStream) throws IOException {
            int lByte = 0;
            int headerEndFlag = 0;

            StringBuilder lBuffer = new StringBuilder(1000);

            do {
                if ((lByte = pInStream.read()) == -1) {
                    break; // end of stream, because pIn.available() may ? not
                           // be reliable enough
                }

                if (lByte == 13) { // CR
                    if (pInStream.read() == 10) { // LF
                        lBuffer.append("\n");
                        headerEndFlag++;
                    }
                } else {
                    lBuffer.append((char) lByte);
                    if (headerEndFlag == 1) {
                        headerEndFlag--;
                    }
                }
            } while (headerEndFlag < 2 && pInStream.available() > 0);
            return lBuffer;
        }

        /**
         * Starting at the CURRENT position of the input stream reading until
         * pContentLen bytes are read.
         */
        public StringBuilder readHttpRequestBody(InputStream pInStream, int pContentLen) throws IOException {
            int i = 0;
            int lByte = 0;
            StringBuilder lBuffer = new StringBuilder(1000);

            for (; i < pContentLen && pInStream.available() > 0; i++) {
                lByte = pInStream.read();
                if (lByte == -1) {
                    break;
                }
                lBuffer.append((char) lByte);
            }

            return lBuffer;
        }

        /**
         * Parse HTTP header lines to key/value pairs. This method includes the http
         * status line as "self defined attributes" (path, method, version etc.) in the
         * map.
         */
        public Map<String, String> parseHttpHeader(String pHeader) {
            Map<String, String> lFields = new LinkedHashMap<>();
            String[] lLines = pHeader.split("\\n");
            StringBuilder lVal;

            for (int i = 0; i < lLines.length; i++) {
                String[] lParts = null;
                String lLine = lLines[i];

                if (i == 0) {
                    lFields.putAll(parseHttpHeaderStatusLine(lLine));
                } else if (lLine.contains(":")) {
                    lParts = lLine.split(":");
                    if (lParts.length == 2) {
                        lFields.put(lParts[0].trim(), lParts[1].trim());
                    } else if (lParts.length > 2) {
                        lVal = new StringBuilder(lParts[1].trim());
                        for (int k = 1; k + 1 < lParts.length; k++) {
                            lVal.append(":").append(lParts[k + 1].trim());
                        }
                        lFields.put(lParts[0].trim(), lVal.toString());
                    }
                }
            }
            return lFields;
        }

        /**
         * Parse header first line = status line.
         */
        public Map<String, String> parseHttpHeaderStatusLine(String pStatusLine) {
            Map<String, String> lFields = new LinkedHashMap<>();

            // parse status line to "self defined attributes"
            String[] lParts = pStatusLine.split(" ");
            String[] lSubParts = null;
            if (lParts.length > 0) {
                for (int i = 0; i < lParts.length; i++) {
                    if (i == 0) {
                        // always bring method names to Upper Case
                        lFields.put(HTTP_METHOD, lParts[i].trim().toUpperCase());
                    } else if (lParts[i].trim().toUpperCase().contains(HTTP_VERSION_MARK)) {
                        lSubParts = lParts[i].trim().split("/");
                        if (lSubParts.length == 2) {
                            lFields.put(HTTP_VERSION, lSubParts[1].trim());
                        }
                    } else if (lParts[i].trim().contains("/")) {
                        lFields.put(HTTP_PATH, lParts[i].trim());
                    }
                }
            }
            return lFields;
        }

        /**
         * <pre>
         * Header Format:
         *
         * statusline \r\n
         * attributeline 0...n \r\n
         * \r\n
         *
         * HTTP/1.0 204 No Content
         * Content-Type: text/plain
         * Content-Length: 0
         * \r\n
         *
         * </pre>
         */
        public StringBuilder createHttpHeader(String[] pStatusLine, Map<String, String> pAttributes) {
            StringBuilder lHeader = new StringBuilder(String.join(" ", pStatusLine));
            for (Map.Entry<String, String> entry : pAttributes.entrySet()) {
                lHeader.append(CRLF).append(entry.getKey()).append(": ").append(entry.getValue());
            }
            lHeader.append(CRLF).append(CRLF);
            return lHeader;
        }

        /**
         */
        public String getHttpStatus(Object pNr) {
            String lNr = String.valueOf(pNr).trim();
            if (HttpHeader.Status.TEXT.containsKey(lNr)) {
                StringBuilder lStatus = new StringBuilder(lNr);
                lStatus.append(" ").append(HttpHeader.Status.TEXT.get(lNr));
                return lStatus.toString();
            }
            return lNr;
        }
    }

    /*********************************************************
     * <pre>
     * A configuration properties class.
     * </pre>
     *********************************************************/
    /**
     */
    public static class Config {

        public static final String HTTP_CORS_ENABLED = "http-cors-enabled";
        public static final String CLIENT_SOCKET_TIMEOUT = "client-socket-timeout";

        public static final String DEFAULT_CONFIG = String.join("\n", "port=8099", "worker=5", "encoding=UTF-8",
                "client-socket-timeout=10000", "http-cors-enabled=false");

        protected Properties props = new Properties();

        private Config() {
            this(DEFAULT_CONFIG);
        }

        private Config(String pDef) {
            this(buildPropertiesFrom(pDef));
        }

        private Config(Properties pProps) {
            props.putAll(pProps);
        }

        /**
         */
        public int getPort() {
            return Integer.valueOf(props.getProperty("port", "8099"));
        }

        /**
         */
        public Config setPort(int pPort) {
            props.setProperty("port", String.valueOf(pPort));
            return this;
        }

        /**
         */
        public int getWorkerNumber() {
            return Integer.valueOf(props.getProperty("worker", "5"));
        }

        /**
         */
        public int getClientSocketTimeout() {
            return Integer.valueOf(props.getProperty(CLIENT_SOCKET_TIMEOUT, "10000"));
        }

        /**
         */
        public boolean isCORSEnabled() {
            return Boolean.parseBoolean(props.getProperty(HTTP_CORS_ENABLED, "false"));
        }

        /**
         */
        public void setCORSEnabled(boolean pVal) {
            props.setProperty(HTTP_CORS_ENABLED, String.valueOf(pVal));
        }

        /**
         */
        @Override
        public String toString() {
            return props.toString();
        }

        /**
         */
        public Config set(String pKey, String pVal) {
            props.setProperty(pKey, pVal);
            return this;
        }

        /**
         */
        public static Properties buildPropertiesFrom(String pDef) {
            Properties lProps = new Properties();
            try {
                lProps.load(new StringReader(pDef));
            } catch (IOException e) {
                LOG.severe(String.format("ERROR parsing config to properties string [%s]", pDef));
                throw new JamnRuntimeException(e);
            }
            return lProps;
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

    /*********************************************************
     * <pre>
     * Common public static helper methods.
     * </pre>
     *********************************************************/
    /**
     */
    public static String getStackTraceFrom(Throwable t) {
        StringWriter lSwriter = new StringWriter();
        PrintWriter lPwriter = new PrintWriter(lSwriter);

        if (t instanceof InvocationTargetException te) {
            t = te.getTargetException();
        }

        t.printStackTrace(lPwriter);
        return lSwriter.toString();
    }

    /*********************************************************
     * <pre>
     * Jamn Exceptions.
     * </pre>
     *********************************************************/
    /**
     */
    public static class JamnRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JamnRuntimeException(Throwable pCause) {
            super(pCause.getMessage(), pCause);
        }
    }
}
