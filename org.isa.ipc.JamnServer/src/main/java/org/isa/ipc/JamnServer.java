/* Authored by www.integrating-architecture.de */

package org.isa.ipc;

//import static org.isa.ipc.JamnServer.HttpHeader.Field.*;
//import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.*;
//import static org.isa.ipc.JamnServer.HttpHeader.Status.*;

import static org.isa.ipc.JamnServer.HttpHeader.HTTP_DEFAULT_RESPONSE_ATTRIBUTES;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_METHODS;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.isa.ipc.JamnServer.HttpHeader.Field.CONTENT_LENGTH;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_1_0;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_METHOD;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_PATH;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_VERSION;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_VERSION_MARK;
import static org.isa.ipc.JamnServer.HttpHeader.Field.SEC_WEBSOCKET_KEY;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_HEADERS_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_METHODS_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_ORIGIN_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_HTML;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_204_NO_CONTENT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_408_TIMEOUT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.isa.ipc.JamnServer.ContentProvider.UpgradeHandler;

/**
 * <pre>
 * Just another micro node Server
 *
 * Jamn is an experimental, lightweight socket-based text data server
 * designed for smallness, independence and easy customization.
 *
 * The purpose is text data based interprocess communication.
 *
 * The structure consists of the following plugable components:
 * - server kernel with socket and multi-threaded connection setup
 * - request processor
 * - content provider
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
 * the Server must be set to CORS Enabled = true
 *  - lServer.getConfig().setCORSEnabled(true)
 *
 * </pre>
 */
public class JamnServer {

    private static final Logger LOG = Logger.getLogger(JamnServer.class.getName());

    // JamnServer web id - just used for http header info
    public static final String JamnServerWebID = "JamnServer/0.0.1";

    // note page - to show that NO content provider is installed by default
    private static String BlankServerPage;

    static {
        try {
            // if exists - initialize logging from internal properties file
            InputStream lIn = JamnServer.class.getResourceAsStream("/logging.properties");
            if (lIn != null) {
                LogManager.getLogManager().readConfiguration(lIn);
            }
            lIn = JamnServer.class.getResourceAsStream("/blank-server.html");
            if (lIn != null) {
                try (BufferedReader lReader = new BufferedReader(new InputStreamReader(lIn))) {
                    BlankServerPage = lReader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
            throw new UncheckedJamnServerException(e, "Jamn Server static initialization failed");
        }
    }

    public static final String LS = System.lineSeparator();
    public static final String CRLF = "\r\n";
    public static final String WEBSOCKET_PROVIDER = "WebSocketProvider";

    // extendable, customizable helper functions
    protected static WebHelper WebHelper = new WebHelper();

    protected Config config = new Config();
    // the actual socket based server implementation
    protected ServerKernel kernel;

    public JamnServer() {
        // default port in config is: 8099
        createServerKernel();
    }

    public JamnServer(int pPort) {
        getConfig().setPort(pPort);
        createServerKernel();
    }

    public JamnServer(Properties pProps) {
        config = new Config(pProps);
        createServerKernel();
    }

    /**
     * Just for running in development
     */
    public static void main(String[] pArgs) {
        JamnServer lServer = new JamnServer();
        // required for using local Testfiles in a browser
        lServer.getConfig().setCORSEnabled(true);
        lServer.start();
    }

    /**
     */
    protected void createServerKernel() {
        kernel = new ServerKernel(config);
    }

    /**
     */
    public synchronized void start() {
        String lErrorInfo = "";
        try {
            kernel.start();
            LOG.info(() -> String.format("JamnServer Instance STARTED: [%s]%s%s", config, LS,
                    LS + " http://localhost:" + config.getPort() + LS));
        } catch (Exception e) {
            if (e instanceof BindException) {
                lErrorInfo = String.format("Probably ALREADY RUNNING SERVER on port [%s]", getConfig().getPort());
                LOG.severe(String.format("%s %s%s", lErrorInfo, LS, getStackTraceFrom(e)));
            } else {
                LOG.severe(String.format("ERROR starting JamnServer: [%s]%s%s", e, LS,
                        getStackTraceFrom(e)));
                lErrorInfo = e.getMessage();
            }
            stop();
            throw new UncheckedJamnServerException(String.format("JamnServer start failed [%s]", lErrorInfo));
        }
    }

    /**
     */
    public synchronized void stop() {
        boolean wasRunning = isRunning();
        kernel.stop();
        if (wasRunning) {
            LOG.info(LS + "JamnServer STOPPED");
        }
    }

    /**
     */
    public boolean isRunning() {
        return kernel.isRunning();
    }

    /**
     */
    public Config getConfig() {
        return config;
    }

    /*********************************************************
     * <pre>
     * The server plugin interfaces for customer extensions and provider.
     * </pre>
     *********************************************************/

    /**
     */
    public JamnServer addContentProvider(String pId, ContentProvider pProvider) {
        kernel.getRequestProcessor().addContentProvider(pId, pProvider);
        return this;
    }

    /**
     */
    public JamnServer setContentProviderDispatcher(ContentProviderDispatcher pProviderDispatcher) {
        kernel.getRequestProcessor().setContentProviderDispatcher(pProviderDispatcher);
        return this;
    }

    /**
     */
    public static void setWebHelper(WebHelper pHelper) {
        WebHelper = pHelper;
    }

    /*********************************************************
     * <pre>
     * The actual socket based Server implementation.
     * </pre>
     *********************************************************/
    public static class ServerKernel {

        protected Config config = new Config();

        protected ServerThread serverThread = null;
        protected ServerSocket serverSocket = null;
        protected ExecutorService requestExecutor = null;
        protected int clientSocketTimeout = 10000;
        protected RequestProcessor requestProcessor = new DefaultRequestProcessor();

        protected ServerKernel() {
        }

        public ServerKernel(Config pConfig) {
            config = pConfig;
        }

        /*********************************************************
         * <pre>
         * The Server socket listener Thread.
         * It starts via requestExecutor a worker thread for every incoming connection
         * and delegates the client socket to a central requestProcessor.
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

                try {
                    ServerSocket lServerSocket = serverSocket; // keep local

                    while (lServerSocket != null && !lServerSocket.isClosed()) {
                        if (!run) {
                            break;
                        }

                        final Socket lClientSocket = lServerSocket.accept();

                        requestExecutor.execute(() -> {
                            Map<String, String> lComData = new HashMap<>(5);
                            long start = System.currentTimeMillis();
                            try {
                                try {
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
                                        LOG.fine(() -> String.format("ClientSocket closed: %s - %s",
                                                (System.currentTimeMillis() - start),
                                                Thread.currentThread().getName()));
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

            /**
             */
            protected boolean isWebSocketRequest(Map<String, String> pComData) {
                return pComData.containsKey(WEBSOCKET_PROVIDER);
            }
        }

        /**
         * @throws IOException
         */
        protected ServerSocket createServerSocket() throws IOException {
            int lPort = config.getPort();
            ServerSocket lSocket = null;

            if (!System.getProperty("javax.net.ssl.keyStore", "").isEmpty()
                    && !System.getProperty("javax.net.ssl.keyStorePassword", "").isEmpty()) {
                lSocket = SSLServerSocketFactory.getDefault().createServerSocket(lPort);
            } else {
                lSocket = ServerSocketFactory.getDefault().createServerSocket(lPort);
            }

            lSocket.setReuseAddress(true);
            if (lPort == 0) {
                config.setActualPort(lSocket.getLocalPort());
            }
            return lSocket;
        }

        /**
         * @throws IOException
         */
        public synchronized void start() throws IOException {
            if (serverSocket != null && !serverSocket.isClosed()) {
                return;
            }

            clientSocketTimeout = config.getClientSocketTimeout();
            requestProcessor.setProperty(Config.HTTP_CORS_ENABLED, String.valueOf(config.isCORSEnabled()));

            serverThread = new ServerThread();
            serverThread.setName(getClass().getSimpleName() + " - on Port [" + config.getPort() + "]");

            serverSocket = createServerSocket();

            if (requestExecutor == null) {
                requestExecutor = Executors.newFixedThreadPool(config.getWorkerNumber());
            }

            serverThread.start();
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
        }

        /**
         */
        public RequestProcessor getRequestProcessor() {
            return requestProcessor;
        }

        /**
         */
        public boolean isRunning() {
            return (serverSocket != null && !serverSocket.isClosed());
        }
    }

    /*********************************************************
     * <pre>
     * The central processing interfaces and default implementations.
     * The interfaces use only themselves or standard Java data types to keep them independent.
     * - RequestProcessor
     * - ContentProvider
     * Chain: ServerThread -> RequestProcessor -> ContentProvider
     * </pre>
     *********************************************************/
    /**
     * <pre>
     * The Interface that is used by the RequestProcessor to create the data content for a request.
     * Or to delegate a protocol upgrade to a specific handler e.g. WebSocket.
     * </pre>
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

        public static interface UpgradeHandler extends ContentProvider {

            /**
             * Interface for Upgrade Handler
             *
             * @param pMethod
             * @param pPath
             * @param pRequestAttributes
             * @param pSocketInStream
             * @param pSocketOutStream
             * @param pSocket
             * @param pComData
             * @return status - http
             */
            String handleRequest(String pMethod, String pPath, Map<String, String> pRequestAttributes,
                    InputStream pSocketInStream, OutputStream pSocketOutStream, Socket pSocket,
                    Map<String, String> pComData);

            /**
             * Not used here - set to default to shade
             */
            @Override
            default String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
                    String pMethod, String pPath, String pRequestBody, final Map<String, String> pRequestAttributes) {
                return SC_204_NO_CONTENT;
            }
        }
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

        // the available ContentProvider
        protected Map<String, ContentProvider> contentProviderMap = new HashMap<>();

        // an empty default provider dispatcher
        protected ContentProviderDispatcher contentDispatcher = (Map<String, String> pRequestAttributes) -> {
            LOG.warning(() -> String.format(
                    "WARNING - The empty DEFAULT ContentProvider-Dispatcher was invoked although there are [%s] provider registered.%s",
                    contentProviderMap.size(), LS));
            return null;
        };

        // empty default content provider
        // just returning the blank server page at root
        // or status SC_204_NO_CONTENT
        protected ContentProvider defaultContentProvider = (Map<String, String> pResponseAttributes,
                OutputStream pResponseContent, String pMethod, String pPath, String pRequestBody,
                final Map<String, String> pRequestAttributes) -> {
            LOG.warning(() -> "Request to EMPTY Default Content Provider");

            try {
                if ("/".equals(pPath)) {
                    new HttpHeader(pResponseAttributes).setContentType(TEXT_HTML);
                    pResponseContent.write(BlankServerPage.getBytes());
                    return SC_200_OK;
                }
            } catch (IOException e) {
                // ignore in empty default implementation
            }
            return SC_204_NO_CONTENT;
        };

        protected UpgradeHandler defaultUpgradeHandler = (pMethod, pPath, pRequestAttributes, pSocketInStream,
                pSocketOutStream, pSocket, pComData) -> {
            LOG.warning(() -> "USE of EMPTY default UpgradeHandler Content Provider");
            return SC_204_NO_CONTENT;
        };

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
        protected UpgradeHandler getContentProviderUpgradeHandlerFor(String pType,
                final Map<String, String> pRequestAttributes) {
            return (UpgradeHandler) contentProviderMap.getOrDefault(pType, defaultUpgradeHandler);
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
            return this;
        }

        /**
         */
        protected boolean isCORSEnabled() {
            return Boolean.valueOf(props.getProperty(Config.HTTP_CORS_ENABLED, "false"));
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
            Response lResponse = new Response(lOutStream, isCORSEnabled(), HTTP_DEFAULT_RESPONSE_ATTRIBUTES);

            try {
                lRequest.readHeader(lInStream);
                lRequest.readBody(lInStream);

                // check for WebSocket upgrade request
                if (lRequest.isWebSocket()) {
                    // if so switch to WebSocket processing
                    UpgradeHandler lWebSocketHandler = getContentProviderUpgradeHandlerFor(WEBSOCKET_PROVIDER,
                            lRequest.getAttributes());
                    lWebSocketHandler.handleRequest(lRequest.getMethod(), lRequest.getPath(), lRequest.getAttributes(),
                            pSocketInStream, pSocketOutStream, pSocket, pComData);
                } else {
                    // else do basic http processing
                    lContentProvider = getContentProviderFor(lRequest.getAttributes());
                    lStatus = lContentProvider.createResponseContent(lResponseAttributes, lResponseContent,
                            lRequest.getMethod(), lRequest.getPath(), lRequest.getBody(), lRequest.getAttributes());

                    lResponse.setHttpStatus(lStatus).applyAttributes(lResponseAttributes).setContent(lResponseContent);
                    lResponse.send();
                }
            } catch (InterruptedIOException e) {
                lResponse.sendStatus(SC_408_TIMEOUT);
            } catch (Exception e) {
                lResponse.sendStatus(SC_500_INTERNAL_ERROR);
                LOG.severe(() -> String.format("Request Handling internal/runtime ERROR: %s %s %s", e, LS,
                        getStackTraceFrom(e)));
            } finally {
                lResponse.close();
            }
        }

        /**
         * Simple DefaultRequestProcessor internal object for better readability.
         */
        protected static class Response {
            protected HttpHeader httpHeader = new HttpHeader();
            protected OutputStream outStream = null;
            protected ByteArrayOutputStream contentBuffer = null;

            public Response(OutputStream pOutStream, boolean pIsCORSEnabled, Map<String, String> pInitAttributes) {
                outStream = pOutStream;
                httpHeader.initWith(pInitAttributes, pIsCORSEnabled);
            }

            public Response setHttpStatus(Object pVal) {
                httpHeader.setHttpStatus(pVal);
                return this;
            }

            public Response applyAttributes(Map<String, String> pAttributes) {
                httpHeader.applyAttributes(pAttributes);
                return this;
            }

            public Response setContent(ByteArrayOutputStream pValue) {
                contentBuffer = pValue;
                return this;
            }

            public void send() throws IOException {
                httpHeader.writeWithBodyTo(outStream, contentBuffer.toByteArray());
            }

            public void sendStatus(String pStatus) throws IOException {
                setHttpStatus(pStatus);
                httpHeader.writeTo(outStream);
            }

            public void close() throws IOException {
                outStream.flush();
            }
        }

        /**
         * Simple DefaultRequestProcessor internal object for better readability.
         */
        protected static class Request extends HttpHeader {
            protected String body = "";

            /**
             */
            public void readHeader(InputStream pInStream) throws IOException {
                byte[] lBytes = WebHelper.readHttpRequestHeader(pInStream);
                String lHeader = new String(lBytes, getEncoding());

                fieldMap = Collections.unmodifiableMap(WebHelper.parseHttpHeader(lHeader));

                LOG.fine(() -> String.format("%s - Request header: %s", Thread.currentThread().getName(), lHeader));
            }

            /**
             * <pre>
             * Tries to blocking read the request body from the socket InputStream.
             * </pre>
             */
            public void readBody(InputStream pInStream) throws IOException {
                byte[] lBytes = WebHelper.readHttpRequestBody(pInStream, getContentLength());
                body = new String(lBytes, getEncoding());
            }

            /**
             */
            public String getBody() {
                return body;
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
        protected String encoding = StandardCharsets.UTF_8.name();

        protected String[] statusline = new String[] { HTTP_1_0, "" };

        protected Map<String, String> fieldMap = new LinkedHashMap<>();

        public HttpHeader() {
        }

        /**
         * IMPORTANT - this INHERITS immutability
         */
        public HttpHeader(Map<String, String> pAttributes) {
            fieldMap = pAttributes;
        }

        /**
         * IMPORTANT - this DOES NOT inherit immutability
         */
        public HttpHeader initWith(Map<String, String> pInitAttributes) {
            fieldMap = new LinkedHashMap<>(pInitAttributes);
            return this;
        }

        public HttpHeader initWith(Map<String, String> pInitAttributes, boolean pIsCORSEnabled) {
            initWith(pInitAttributes);
            setCORSEnabled(pIsCORSEnabled);
            return this;
        }

        // the magic websocket uid to accept a connection request
        public static final String MAGIC_WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

        /**
         */
        public static final Map<String, String> HTTP_DEFAULT_RESPONSE_ATTRIBUTES;
        public static final Map<String, String> HTTP_DEFAULT_WEBSOCKET_RESPONSE_ATTRIBUTES;
        static {
            Map<String, String> lMap = new HashMap<>();
            lMap.put(Field.SERVER, JamnServerWebID);
            lMap.put(Field.CONTENT_TYPE, FieldValue.TEXT_PLAIN);
            lMap.put(Field.CONTENT_LENGTH, "0");
            HTTP_DEFAULT_RESPONSE_ATTRIBUTES = Collections.unmodifiableMap(lMap);

            lMap = new HashMap<>();
            lMap.put(Field.SERVER, JamnServerWebID);
            lMap.put(Field.UPGRADE, FieldValue.WEBSOCKET);
            lMap.put(Field.CONNECTION, FieldValue.UPGRADE);
            lMap.put(Field.SEC_WEBSOCKET_ACCEPT, "");
            HTTP_DEFAULT_WEBSOCKET_RESPONSE_ATTRIBUTES = Collections.unmodifiableMap(lMap);
        }

        /**
         * HTTP status codes.
         */
        public static class Status {
            protected Status() {
            }

            public static final String SC_101_SWITCH_PROTOCOLS = "101";
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
                lMap.put("101", "Switching Protocols");
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
            public static final String UPGRADE = "Upgrade";
            public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
            public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
            public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

            public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
            public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
            public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
            public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
        }

        /**
         * HTTP header field values.
         */
        public static class FieldValue {
            protected FieldValue() {
            }

            public static final String CLOSE = "close";
            public static final String UPGRADE = "Upgrade";
            public static final String KEEP_ALIVE_UPGRADE = "keep-alive, Upgrade";
            public static final String WEBSOCKET = "websocket";
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
        public HttpHeader setCORSEnabled(boolean pFlag) {
            if (pFlag) {
                // at first just allow everything
                set(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_ALL);
                set(ACCESS_CONTROL_ALLOW_METHODS, ACCESS_CONTROL_ALLOW_METHODS_ALL);
                set(ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_ALLOW_HEADERS_ALL);
            }
            return this;
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
        public String get(String pKey, String pDefault) {
            return fieldMap.getOrDefault(pKey, pDefault);
        }

        /**
         */
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        /**
         */
        public String getEncoding() {
            return encoding;
        }

        /**
         */
        public HttpHeader setHttpVersion(String pVal) {
            statusline[0] = pVal;
            return this;
        }

        /**
         */
        public HttpHeader setHttpStatus(Object pVal) {
            String lStatus = WebHelper.getHttpStatus(pVal);
            if (!lStatus.isEmpty()) {
                statusline[1] = lStatus;
            }
            return this;
        }

        /**
         */
        public HttpHeader applyAttributes(Map<String, String> pAttributes) {
            fieldMap.putAll(pAttributes);
            return this;
        }

        /**
         */
        public Map<String, String> getAttributes() {
            return this.fieldMap;
        }

        /**
         */
        public StringBuilder getHeader() {
            return WebHelper.createHttpHeader(statusline, fieldMap);
        }

        /**
         */
        @Override
        public String toString() {
            return getHeader().toString();
        }

        /**
         * @throws IOException
         */
        public byte[] createMessageBytesWithBody(byte[] pBody) throws IOException {
            return WebHelper.createHttpMessageBytes(statusline, fieldMap, pBody, this.encoding);
        }

        /**
         * @throws IOException
         */
        public void writeTo(OutputStream pOut) throws IOException {
            pOut.write(createMessageBytesWithBody(null));
            pOut.flush();
        }

        /**
         * @throws IOException
         */
        public void writeWithBodyTo(OutputStream pOut, byte[] pBody) throws IOException {
            pOut.write(createMessageBytesWithBody(pBody));
            pOut.flush();
        }

        /**
         */
        public String getContentType() {
            return get(Field.CONTENT_TYPE);
        }

        /**
         */
        public int getContentLength() {
            return Integer.valueOf(get(Field.CONTENT_LENGTH, "0"));
        }

        /**
         */
        public String getWebSocketKey() {
            return get(SEC_WEBSOCKET_KEY);
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
        public HttpHeader setConnection(String pVal) {
            return set(Field.CONNECTION, pVal);
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
        public boolean isWebSocket() {
            return !getWebSocketKey().isEmpty();
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
    public static class WebHelper {

        /**
         * Starting at the beginning of the stream reading until a blank/empty line is
         * detected.
         * 
         * @throws IOException
         */
        public byte[] readHttpRequestHeader(InputStream pInStream) throws IOException {
            int lByte = 0;
            int headerEndFlag = 0;

            ByteArrayOutputStream lByteBuffer = new ByteArrayOutputStream();
            do {
                if ((lByte = pInStream.read()) == -1) {
                    break; // end of stream, because pIn.available() may ? not
                           // be reliable enough
                }

                // TODO encoding test
                if (lByte == 13) { // CR
                    lByteBuffer.write(lByte);
                    if ((lByte = pInStream.read()) == 10) { // LF
                        lByteBuffer.write(lByte);
                        headerEndFlag++;
                    } else if (lByte != -1) {
                        lByteBuffer.write(lByte);
                    }
                } else {
                    lByteBuffer.write(lByte);
                    if (headerEndFlag == 1) {
                        headerEndFlag--;
                    }
                }
            } while (headerEndFlag < 2 && pInStream.available() > 0);

            return lByteBuffer.toByteArray();
        }

        /**
         * <pre>
         * Tries to blocking read the request body from the socket InputStream.
         * </pre>
         */
        public byte[] readHttpRequestBody(InputStream pInStream, int pContentLength)
                throws IOException {
            ByteArrayOutputStream lByteBuffer = new ByteArrayOutputStream();
            int lByte = 0;
            int lActual = 0;
            int lAvailable = 0;

            if (pContentLength > 0) {
                while ((lAvailable = pInStream.available()) > 0 || lActual < pContentLength) {
                    lActual++;
                    lByte = pInStream.read();
                    if (lByte == -1) {
                        break;
                    }
                    lByteBuffer.write(lByte);
                    // lBuffer.append((char) lByte);
                }
                if (lActual != pContentLength || lAvailable > 0) {
                    String msg = String.format("Http body read: actual [%s] header [%s] available [%s]", lActual,
                            pContentLength,
                            lAvailable);
                    LOG.warning(() -> msg);
                }
            }
            return lByteBuffer.toByteArray();
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
        public byte[] createHttpMessageBytes(String[] pStatusLine, Map<String, String> pHeaderAttributes, byte[] pBody,
                String pEncoding) throws IOException {
            ByteArrayOutputStream lMessage = new ByteArrayOutputStream();

            int lContentLen = (pBody != null) ? pBody.length : 0;
            if (lContentLen > 0) {
                pHeaderAttributes.put(CONTENT_LENGTH, String.valueOf(lContentLen));
            }

            lMessage.write(createHttpHeader(pStatusLine, pHeaderAttributes).toString().getBytes());
            if (lContentLen > 0) {
                lMessage.write(pBody);
            }
            return lMessage.toByteArray();
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
     * A properties configuration  class.
     * </pre>
     *********************************************************/
    /**
     */
    public static class Config {

        public static final String HTTP_CORS_ENABLED = "http-cors-enabled";
        public static final String CLIENT_SOCKET_TIMEOUT = "client-socket-timeout";

        public static final String DEFAULT_CONFIG = String.join(LS,
                "##",
                "## " + JamnServerWebID + " Config Properties",
                "##", "",
                "#Server port", "port=8099", "", 
                "#Max worker threads", "worker=5", "",
                "#Encoding", "encoding=" + StandardCharsets.UTF_8.name(), "",
                "#Socket timeout in millis", "client-socket-timeout=10000", "",
                "#Cross origin flag", "http-cors-enabled=false", "");

        protected Properties props = new Properties();

        public Config() {
            this(DEFAULT_CONFIG);
        }

        public Config(String pDef) {
            this(buildPropertiesFrom(pDef));
        }

        public Config(Properties pProps) {
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
        public int getActualPort() {
            return Integer.valueOf(props.getProperty("actual.port", "-1"));
        }

        /**
         */
        public Config setActualPort(int pPort) {
            props.setProperty("actual.port", String.valueOf(pPort));
            return this;
        }

        /**
         */
        public String getEncoding() {
            return props.getProperty("encoding", "UTF-8");
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
                LOG.severe(String.format("ERROR parsing config to properties string [%s] [%s]", pDef, e));
                throw new UncheckedJamnServerException("Config properties creation/initialization error");
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
        public <T> T toObject(String pSrc, Class<T> pType) throws IOException;

        /**
         */
        public String toString(Object pObj) throws IOException;

        /**
         */
        public default Object getNativeTool() {
            return null;
        }

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
    public static class UncheckedJamnServerException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedJamnServerException(Throwable pCause, String pMsg) {
            super(pMsg, pCause);
        }

        public UncheckedJamnServerException(String pMsg) {
            super(pMsg);
        }

    }

}
