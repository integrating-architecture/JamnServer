/* Authored by www.integrating-architecture.de */

package org.isa.ipc;

import static org.isa.ipc.JamnServer.HttpHeader.HTTP_DEFAULT_RESPONSE_ATTRIBUTES;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_METHODS;
import static org.isa.ipc.JamnServer.HttpHeader.Field.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.isa.ipc.JamnServer.HttpHeader.Field.CONNECTION;
import static org.isa.ipc.JamnServer.HttpHeader.Field.CONTENT_LENGTH;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_1_0;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_METHOD;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_PATH;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_VERSION;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_VERSION_MARK;
import static org.isa.ipc.JamnServer.HttpHeader.Field.SEC_WEBSOCKET_KEY;
import static org.isa.ipc.JamnServer.HttpHeader.Field.SET_COOKIE;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_HEADERS_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_METHODS_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.ACCESS_CONTROL_ALLOW_ORIGIN_ALL;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.CLOSE;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.KEEP_ALIVE;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_HTML;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_204_NO_CONTENT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_403_FORBIDDEN;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
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
 * 
 * Jamn does NOT offer complete support for the HTTP protocol.
 * It just supports a subset - that is required and suitable for its use cases.
 * That are:
 *  - text data based network/interprocess communication
 *  - the ability to serve Browser based Applications
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
            InputStream lIn = JamnServer.class.getResourceAsStream("/blank-server.html");
            if (lIn != null) {
                BufferedReader lReader = new BufferedReader(new InputStreamReader(lIn));
                BlankServerPage = lReader.lines().collect(Collectors.joining("\n"));
            }
        } catch (RuntimeException e) {
            throw new UncheckedJamnServerException(e, "\nERROR - Jamn Server static initialization failed\n");
        }
    }

    protected static final Function<Socket, String> GetSocketIDText = socket -> String.format("ClientSocket [%s]:",
            socket.hashCode());

    public static final String LS = System.lineSeparator();
    public static final String LF = "\n";
    public static final String CRLF = "\r\n";
    public static final String WEBSOCKET_PROVIDER = "WebSocketProvider";
    public static final String SOCKET_IDTEXT = "socket.idtext";
    public static final String SOCKET_USAGE = "socket.usage";
    public static final String SOCKET_EXCEPTION = "socket.exception";

    // extendable, customizable helper functions
    protected static HttpHelper HttpHelper = new HttpHelper();

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
    public void setAccessManager(AccessManager pManager) {
        kernel.getRequestProcessor().setAccessManager(pManager);
    }

    /**
     */
    public static void setHttpHelper(HttpHelper pHelper) {
        HttpHelper = pHelper;
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
        protected RequestProcessor requestProcessor;

        protected ServerKernel() {
        }

        public ServerKernel(Config pConfig) {
            config = pConfig;
            requestProcessor = new DefaultRequestProcessor(pConfig);
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

                        // start request execution in its own thread
                        requestExecutor.execute(() -> {
                            Map<String, String> lComData = new HashMap<>(5);
                            long start = System.currentTimeMillis();
                            try {
                                try {
                                    lClientSocket.setSoTimeout(clientSocketTimeout);
                                    lClientSocket.setTcpNoDelay(true);
                                    // delegate the concrete request handling
                                    requestProcessor.handleRequest(lClientSocket, lComData);

                                } finally {
                                    try {
                                        if (!(lClientSocket instanceof SSLSocket)) {
                                            lClientSocket.shutdownOutput(); // first step only output
                                        }
                                    } finally {
                                        lClientSocket.close();
                                        LOG.fine(() -> String.format("%s %s %s %s %s",
                                                lComData.getOrDefault(SOCKET_IDTEXT, "unknown"),
                                                "closed [" + (System.currentTimeMillis() - start) + "]",
                                                "usage [" + lComData.getOrDefault(SOCKET_USAGE, "") + "]",
                                                "exp [" + lComData.getOrDefault(SOCKET_EXCEPTION, "") + "]",
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
         */
        protected ServerSocket createServerSocket() throws IOException {
            int lPort = config.getPort();
            ServerSocket lSocket = null;

            try {
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
            } catch (Exception e) {
                if (lSocket != null) {
                    lSocket.close();
                }
                throw e;
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
         * Interface for ContentProvider
         * 
         * @param pRequest
         * @param pResponse
         * @return
         */
        void createResponseContent(RequestMessage pRequest, ResponseMessage pResponse);

        /**
         * Interface for Upgrade Handler
         *
         * @param pRequest
         * @param pSocket
         * @param pComData
         * @return
         */
        public static interface UpgradeHandler extends ContentProvider {

            String handleRequest(RequestMessage pRequest, Socket pSocket, Map<String, String> pComData)
                    throws IOException;

            /**
             * Not relevant for websocket - set to default to shade
             */
            @Override
            default void createResponseContent(RequestMessage pRequest, ResponseMessage pResponse) {
                throw new UnsupportedOperationException(
                        "WebSocket UpgradeHandler does NOT support createResponseContent");
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
         * @param pSocket
         * @param pComData - internal communication data
         * @throws IOException
         */
        void handleRequest(Socket pSocket, Map<String, String> pComData) throws IOException;

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
        default void setAccessManager(AccessManager pManager) {
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
        String getContentProviderIDFor(RequestMessage pRequest);
    }

    /**
     * <pre>
     * IMPORTANT Preamble: 
     * 
     * The Jamn Server initially offers NO security components or implementations
     * !!! and is therefore "COMPLETELY INSECURE" !!!
     * Security mechanisms must be added individually.
     * 
     * At the first level by using the SSL socket connection in the server kernel.
     * 
     * At the next level, individual components are required 
     * that correspond to the security standards and needs of the user.
     *
     * However - this AccessManager interface 
     * serves as a place(holder) for this comment and thus 
     * as a reminder that the security issue must always be taken into account.
     *
     * Above the socket transport layer security, 
     * a possible entry point is directly after a client request message has been received
     * in this case: {@link DefaultRequestProcessor} handleRequest method
     * where this interface is located by default.
     *
     * </pre>
     */
    public static interface AccessManager {
        /**
         */
        void processRequestAccess(RequestMessage pRequest, ResponseMessage pResponse)
                throws SecurityException;
    }

    /**
     * <pre>
     * The RequestProcessor is the interface that is called by the socket layer 
     * when a message is received - and thus the entry point for processing.
     * 
     * The Default-RequestProcessor implements the basic JamnServer http layer
     * but it uses an EMPTY ContentProvider.
     * The processor just reads data from the underlying socket
     * and tries to interpret/transform it into a textual http message consisting of of a http-header and body.
     * 
     * The processor also branches to an upgrade handler in case of a Web-Socket connection request.
     * IMPORTANT
     * If a Web-Socket connection can be established
     * the further client/server communication will NOT be http anymore.
     * 
     * </pre>
     * 
     * Please see: {@link AccessManager}
     */
    public static class DefaultRequestProcessor implements RequestProcessor {
        protected Config config;
        protected String encoding = StandardCharsets.UTF_8.name();
        protected boolean keepAliveEnabled = false;

        /**
         */
        public DefaultRequestProcessor(Config pConfig) {
            this.config = pConfig;
            this.encoding = config.getEncoding();
            this.keepAliveEnabled = config.isConnectionKeepAlive();
        }

        // the available ContentProvider
        protected Map<String, ContentProvider> contentProviderMap = new HashMap<>();

        //
        protected AccessManager accessManager = (RequestMessage pRequest, ResponseMessage pResponse) -> LOG
                .warning(() -> "WARNING - EMPTY DEFAULT AccessManager active");

        // an empty default provider dispatcher
        protected ContentProviderDispatcher contentDispatcher = (RequestMessage pRequest) -> {
            LOG.warning(() -> String.format(
                    "WARNING - The empty DEFAULT ContentProvider-Dispatcher was invoked although there are [%s] provider registered.%s",
                    contentProviderMap.size(), LS));
            return null;
        };

        // empty default content provider
        // just returning the blank server page at root
        // or status SC_204_NO_CONTENT
        protected ContentProvider defaultContentProvider = (RequestMessage pRequest, ResponseMessage pResponse) -> {
            LOG.warning(() -> "Request to EMPTY Default Content Provider");

            pResponse.setStatus(SC_204_NO_CONTENT);
            try {
                if ("/".equals(pRequest.getPath())) {
                    pResponse.setContentType(TEXT_HTML);
                    pResponse.writeToContent(BlankServerPage.getBytes());
                    pResponse.setStatus(SC_200_OK);
                }
            } catch (IOException e) {
                // ignore in empty default implementation
            }
        };

        protected UpgradeHandler defaultUpgradeHandler = (RequestMessage pRequest, Socket pSocket,
                Map<String, String> pComData) -> {
            LOG.warning(() -> "USE of EMPTY default UpgradeHandler Content Provider");
            return SC_204_NO_CONTENT;
        };

        /**
         */
        protected ContentProvider getContentProviderFor(RequestMessage pRequest) {
            if (contentProviderMap.isEmpty()) {
                return defaultContentProvider;
            }
            // to avoid the need for a dispatcher for only one provider
            if (contentProviderMap.size() == 1) {
                return contentProviderMap.values().iterator().next();
            }
            // else use dispatcher
            String providerId = contentDispatcher.getContentProviderIDFor(pRequest);
            return contentProviderMap.getOrDefault(providerId, defaultContentProvider);
        }

        /**
         */
        protected UpgradeHandler getContentProviderUpgradeHandlerFor(String pType) {
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
        public void setAccessManager(AccessManager pManager) {
            accessManager = pManager;
        }

        /**
         */
        @Override
        public void addContentProvider(String pId, ContentProvider pProvider) {
            contentProviderMap.put(pId, pProvider);
        }

        /**
         */
        protected boolean isCORSEnabled() {
            return config.isCORSEnabled();
        }

        /**
         */
        protected HttpHeader readHeader(InputStream pInStream, String pSocketIDText) throws IOException {
            byte[] lBytes = HttpHelper.readHttpRequestHeader(pInStream);
            String lHeader = new String(lBytes, this.encoding);

            LOG.fine(() -> String.format("%s%s %s - Request header: %s%s", LS, pSocketIDText,
                    Thread.currentThread().getName(),
                    lHeader.trim(), LS));

            return new HttpHeader(Collections.unmodifiableMap(HttpHelper.parseHttpHeader(lHeader)));
        }

        /**
         * <pre>
         * Tries to blocking read the request body from the socket InputStream.
         * </pre>
         */
        protected String readBody(InputStream pInStream, int pContentLength, String pEncoding) throws IOException {
            byte[] lBytes = HttpHelper.readHttpRequestBody(pInStream, pContentLength);
            return new String(lBytes, pEncoding);
        }

        /**
         */
        protected ResponseMessage newResponseMessageFor(OutputStream pOutStream) {
            ResponseMessage lResponse = new ResponseMessage(pOutStream, isCORSEnabled(),
                    HTTP_DEFAULT_RESPONSE_ATTRIBUTES);
            // set default close - overwrite later if request want keep-alive
            lResponse.header().setConnectionClose();
            return lResponse;
        }

        /**
         */
        protected int getInitialBufferSizeFor(String pType) {
            return "in".equalsIgnoreCase(pType) ? 4 * 1024 : 8 * 1024;
        }

        /**
         */
        protected boolean checkForKeepAliveConnection(RequestMessage pRequest, ResponseMessage pResponse) {     

            if (pRequest.header().hasConnectionKeepAlive() && keepAliveEnabled) {
                pResponse.header().setConnectionKeepAlive();
                return true;
            }
            return false;
        }

        /**
         * The default handling. Read an incoming http request and reply a basic http
         * response.
         */
        @Override
        public void handleRequest(Socket pSocket, Map<String, String> pComData) throws IOException {

            String socketIDText = GetSocketIDText.apply(pSocket);
            pComData.put(SOCKET_IDTEXT, socketIDText);

            ContentProvider lContentProvider = null;
            InputStream lInStream = new BufferedInputStream(pSocket.getInputStream(), getInitialBufferSizeFor("in"));
            OutputStream lOutStream = new BufferedOutputStream(pSocket.getOutputStream(),
                    getInitialBufferSizeFor("out"));

            RequestMessage lRequest = null;
            ResponseMessage lResponse = newResponseMessageFor(lOutStream);

            boolean keepAlive = false;
            // a usage counter for debugging purpose
            int usage = 0;
            try {
                LOG.fine(() -> String.format("%s %s %s %s", socketIDText, "opened", pSocket.toString(),
                        Thread.currentThread().getName()));

                do {
                    keepAlive = false;
                    lRequest = new RequestMessage(readHeader(lInStream, socketIDText));
                    lRequest.setBody(readBody(lInStream, lRequest.getContentLength(), lRequest.getEncoding()));

                    lResponse = newResponseMessageFor(lOutStream);

                    // call a possible installed access manager
                    // the manager may trigger an immediately response
                    accessManager.processRequestAccess(lRequest, lResponse);

                    // if accessManager has NOT already sent a response
                    if (lResponse.isNotProcessed()) {
                        // route request to the required content provider
                        // check for WebSocket upgrade request
                        if (lRequest.header().isWebSocket()) {
                            // if so switch to WebSocket processing
                            UpgradeHandler lWebSocketHandler = getContentProviderUpgradeHandlerFor(WEBSOCKET_PROVIDER);
                            lWebSocketHandler.handleRequest(lRequest, pSocket, pComData);
                        } else {
                            keepAlive = checkForKeepAliveConnection(lRequest, lResponse);

                            // create and send the response content
                            lContentProvider = getContentProviderFor(lRequest);
                            lContentProvider.createResponseContent(lRequest, lResponse);
                            lResponse.send();
                            usage++;
                        }
                    }
                    // if keep-alive loop until socket timeout
                } while (keepAlive && keepAliveEnabled);
            } catch (InterruptedIOException e) {
                pComData.put(SOCKET_EXCEPTION, e.getMessage());
                interruptCleanUp(socketIDText, lInStream, lOutStream);
            } catch (SecurityException se) {
                // send 403 for any security exception
                lResponse.sendStatus(SC_403_FORBIDDEN);
            } catch (Exception e) {
                LOG.severe(() -> String.format("%s Request handling internal ERROR: %s %s %s", socketIDText, e, LS,
                        getStackTraceFrom(e)));
                // send 500 for any other exception
                lResponse.sendStatus(SC_500_INTERNAL_ERROR);
            } finally {
                lResponse.close();
                pComData.put(SOCKET_USAGE, String.valueOf(usage));
            }
        }

        /**
        */
        protected void interruptCleanUp(String pIDText, InputStream pIn, OutputStream pOut) {
            try {
                pIn.close();
            } catch (Exception e) {
                LOG.warning(String.format("%s ERROR closing input after timeout [%s]", pIDText, e.toString()));
            }
            try {
                pOut.close();
            } catch (Exception e) {
                LOG.warning(String.format("%s ERROR closing output after timeout [%s]", pIDText, e.toString()));
            }
        }

    }

    /**
     * <pre>
     * The class encapsulates HTTP header information.
     * In particular, it provides a selection of constants for status codes and header fields.
     * In addition, it serves as a wrapper around a map with key/value pairs for header fields.
     * </pre>
     */
    public static class HttpHeader {
        protected String encoding = StandardCharsets.UTF_8.name();

        protected String[] statusline = new String[] { HTTP_1_0, "" };

        protected Map<String, String> fieldMap = new LinkedHashMap<>();
        protected List<String> setCookies = null;

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
            public static final String SC_403_FORBIDDEN = "403";
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
                lMap.put("403", "Forbidden");
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
            public static final String HOST = "Host";
            public static final String UPGRADE = "Upgrade";
            public static final String SET_COOKIE = "Set-Cookie";
            public static final String COOKIE = "Cookie";

            public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
            public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
            public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

            public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
            public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
            public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
            public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
            public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

            public static final String AUTHORIZATION = "Authorization";
            public static final String BEARER = "Bearer";

        }

        /**
         * HTTP header field values.
         */
        public static class FieldValue {
            protected FieldValue() {
            }

            public static final String CLOSE = "close";
            public static final String KEEP_ALIVE = "keep-alive";
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
            String lStatus = HttpHelper.getHttpStatus(pVal);
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
        @Override
        public String toString() {
            return HttpHelper.createHttpHeader(statusline, fieldMap, setCookies).toString();
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
        public boolean hasConnectionKeepAlive() {
            return has(CONNECTION, KEEP_ALIVE);
        }

        /**
         */
        public HttpHeader setConnectionKeepAlive() {
            return set(CONNECTION, KEEP_ALIVE);
        }

        /**
         */
        public HttpHeader setConnectionClose() {
            return set(CONNECTION, CLOSE);
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
        public HttpHeader addSetCookie(String pVal) {
            getSetCookies().add(pVal);
            return this;
        }

        /**
         */
        public List<String> getSetCookies() {
            if (setCookies == null) {
                setCookies = new ArrayList<>();
            }
            return setCookies;
        }

        /**
         */
        public String getCookie() {
            return fieldMap.getOrDefault(Field.COOKIE, "");
        }

        /**
         */
        public List<String> getCookieAsList() {
            return Arrays.asList(fieldMap.getOrDefault(Field.COOKIE, "").split(";"));
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
        public String getHost() {
            return fieldMap.getOrDefault(Field.HOST, "");
        }

        /**
         */
        public String getAuthorization() {
            return fieldMap.getOrDefault(Field.AUTHORIZATION, "");
        }

        /**
         */
        public String getAuthorizationBearer() {
            String lVal = getAuthorization();
            if (lVal.startsWith(Field.BEARER)) {
                String[] lToken = lVal.split(" ");
                return lToken.length == 2 ? lToken[1].trim() : "";
            }
            return "";
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
     * </pre>
     */
    public static class RequestMessage {
        protected HttpHeader httpHeader = null;
        protected String bodyContent = "";

        public RequestMessage(HttpHeader pHeader) {
            httpHeader = pHeader;
        }

        /**
         */
        public HttpHeader header() {
            return httpHeader;
        }

        /**
         */
        public String body() {
            return bodyContent;
        }

        /**
         */
        public void setBody(String pBody) {
            bodyContent = pBody;
        }

        /**
         */
        public int getContentLength() {
            return httpHeader.getContentLength();
        }

        /**
         */
        public String getContentType() {
            return httpHeader.getContentType();
        }

        /**
         */
        public String getEncoding() {
            return httpHeader.getEncoding();
        }

        /**
         */
        public String getPath() {
            return httpHeader.getPath();
        }

        /**
         */
        public String getMethod() {
            return httpHeader.getMethod();
        }

        /**
         */
        public boolean isMethod(String pVal) {
            return httpHeader.getMethod().equalsIgnoreCase(pVal);
        }

        /**
         */
        public boolean hasContentType(String pType) {
            return httpHeader.hasContentType(pType);
        }
    }

    /**
     * <pre>
     * </pre>
     */
    public static class ResponseMessage {
        protected HttpHeader httpHeader = new HttpHeader();
        protected OutputStream outStream;
        protected ByteArrayOutputStream contentBuffer;
        protected String statusNr = "";
        protected boolean isProcessed = false;

        protected String encoding = "UTF-8";

        public ResponseMessage(OutputStream pOutStream, HttpHeader pHttpHeader) {
            outStream = pOutStream;
            httpHeader = pHttpHeader;
        }

        public ResponseMessage(OutputStream pOutStream, boolean pIsCORSEnabled, Map<String, String> pInitAttributes) {
            outStream = pOutStream;
            httpHeader.initWith(pInitAttributes, pIsCORSEnabled);
        }

        protected ByteArrayOutputStream getContentBuffer() {
            if (contentBuffer == null) {
                contentBuffer = new ByteArrayOutputStream();
            }
            return contentBuffer;
        }

        /**
         */
        public HttpHeader header() {
            return httpHeader;
        }

        /**
         */
        public ResponseMessage setStatus(String pVal) {
            statusNr = pVal;
            httpHeader.setHttpStatus(statusNr);
            return this;
        }

        /**
         */
        public String getStatus() {
            return statusNr;
        }

        /**
         */
        public void setProcessed() {
            isProcessed = true;
        }

        /**
         */
        public boolean isNotProcessed() {
            return !isProcessed;
        }

        /**
         */
        public ResponseMessage setContentType(String pVal) {
            httpHeader.setContentType(pVal);
            return this;
        }

        /**
         */
        public String getContentType() {
            return httpHeader.getContentType();
        }

        /**
         */
        public void writeToContent(byte[] pContent) throws IOException {
            getContentBuffer().write(pContent);
        }

        /**
         */
        public void send() throws IOException {
            writeTo(outStream, getContentBuffer().toByteArray());
        }

        /**
         */
        public void sendStatus(String pStatus) throws IOException {
            setStatus(pStatus);
            writeTo(outStream, null);
        }

        /**
         */
        public void close() throws IOException {
            outStream.flush();
        }

        /**
         * @throws IOException
         */
        protected void writeTo(OutputStream pOut, byte[] pBody) throws IOException {
            pOut.write(createMessageBytesWithBody(pBody));
            pOut.flush();
        }

        /**
         * @throws IOException
         */
        protected byte[] createMessageBytesWithBody(byte[] pBody) throws IOException {
            return HttpHelper.createHttpMessageBytes(httpHeader.statusline, httpHeader.fieldMap, httpHeader.setCookies,
                    pBody, encoding);
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
        public StringBuilder createHttpHeader(String[] pStatusLine, Map<String, String> pAttributes,
                List<String> pSetCookies) {
            StringBuilder lHeader = new StringBuilder(String.join(" ", pStatusLine));
            for (Map.Entry<String, String> entry : pAttributes.entrySet()) {
                lHeader.append(CRLF).append(entry.getKey()).append(": ").append(entry.getValue());
            }

            if (pSetCookies != null && !pSetCookies.isEmpty()) {
                for (String entry : pSetCookies) {
                    lHeader.append(CRLF).append(SET_COOKIE).append(": ").append(entry);
                }
            }

            lHeader.append(CRLF).append(CRLF);
            return lHeader;
        }

        /**
         */
        public byte[] createHttpMessageBytes(String[] pStatusLine, Map<String, String> pHeaderAttributes,
                List<String> pSetCookies, byte[] pBody,
                String pEncoding) throws IOException {
            ByteArrayOutputStream lMessage = new ByteArrayOutputStream();

            int lContentLen = (pBody != null) ? pBody.length : 0;
            if (lContentLen > 0) {
                pHeaderAttributes.put(CONTENT_LENGTH, String.valueOf(lContentLen));
            }

            byte[] lHeader = createHttpHeader(pStatusLine, pHeaderAttributes, pSetCookies).toString()
                    .getBytes(pEncoding);
            lMessage.write(lHeader);
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

        public static final String HTTP_CORS_ENABLED = "http.cors.enabled";
        public static final String CLIENT_SOCKET_TIMEOUT = "client.socket.timeout";
        public static final String CONNECTION_KEEP_ALIVE = "connection.keep.alive";

        public static final String DEFAULT_CONFIG = String.join(LF,
                "##",
                "## " + JamnServerWebID + " Config Properties",
                "##", "",
                "#Server port", "port=8099", "",
                "#Max worker threads", "worker=5", "",
                "#Socket timeout in millis", "client.socket.timeout=500", "",
                "#Use Connection:keep-alive header", "connection.keep.alive=true", "",
                "#Encoding", "encoding=" + StandardCharsets.UTF_8.name(), "",
                "#Cross origin flag", "http.cors.enabled=false", "");

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
            return props.getProperty("encoding", StandardCharsets.UTF_8.name());
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
        public boolean isConnectionKeepAlive() {
            return Boolean.parseBoolean(props.getProperty(CONNECTION_KEEP_ALIVE, "false"));
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
        public <T> T toObject(String pSrc, Class<T> pType) throws UncheckedJsonException;

        /**
         */
        public String toString(Object pObj) throws UncheckedJsonException;

        /**
         */
        public default Object getNativeTool() {
            return null;
        }

        /**
         */
        public default String prettify(String pJsonInput) {
            return pJsonInput;
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

    /**
     */
    public static class UncheckedJsonException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public static final String TOOBJ_ERROR = "JSON: string parse to object error";
        public static final String TOJSON_ERROR = "JSON: object write as json string error";
        public static final String PRETTIFY_ERROR = "JSON: prettiying json string error";

        public UncheckedJsonException(String pMsg, Throwable pCause) {
            super(pMsg, pCause);
        }

        public UncheckedJsonException(String pMsg) {
            super(pMsg);
        }

    }

}
