/*Authored by www.integrating-architecture.de*/

package org.isa.ipc;

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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
 * and responds with an appropriate HTTP message.
 * 
 * IMPORTANT: 
 * How ever - Jamn IS NOT a HTTP/Web Server Implementation - this is NOT intended.
 * Jamn does NOT offer complete support for the HTTP protocol.
 * 
 * </pre>
 */
public class JamnServer {

	// initialize logging from properties file
	static {
		try {
			LogManager.getLogManager().readConfiguration(JamnServer.class.getResourceAsStream("/logging.properties"));
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
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
	protected static CommonHelper Helper = new CommonHelper();
	protected static HttpHelper HttpHelper = new HttpHelper();

	protected Config config = new Config();

	protected volatile ServerSocket serverSocket = null;
	protected volatile ExecutorService requestExecutor = null;
	protected volatile int clientSocketTimeout = 10000;
	protected ServerThread serverThread = null;
	Supplier<ServerThread> serverThreadSupplier = () -> {
		return new ServerThread();
	};
	protected RequestProcessor requestProcessor = new DefaultRequestProcessor();

	public JamnServer() {
	}

	public JamnServer(Properties pProps) {
		config = new Config(pProps);
	}

	/**
	 */
	public static void main(String[] pArgs) {
		JamnServer lServer = new JamnServer();
		lServer.getConfig().setCORSEnabled(true); // required for localhost communication via js fetch
		lServer.start();
	}

	/**
	 */
	public synchronized void start() {
		if (serverSocket != null && !serverSocket.isClosed()) {
			return;
		}

		requestProcessor.setProperty(HttpConstants.HTTP_CORS_ENABLED, String.valueOf(getConfig().isCORSEnabled()));

		serverThread = serverThreadSupplier.get();
		serverThread.setName(getClass().getSimpleName() + " - on Port [" + config.getPort() + "]");

		try {
			serverSocket = ServerSocketFactory.getDefault().createServerSocket(config.getPort());
			serverSocket.setReuseAddress(true);

			clientSocketTimeout = config.getClientSocketTimeout();

			if (requestExecutor == null) {
				requestExecutor = Executors.newFixedThreadPool(config.getWorkerNumber());
			}

			serverThread.start();

			LOG.info("Server STARTED:  [" + config + "]" + LS);
		} catch (IOException e) {
			LOG.severe("ERROR starting Server: [" + e.getMessage() + "]");
			stop();
		}
	}

	/**
	 */
	public synchronized void stop() {
		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (SocketException e) {
				// OK this is specified
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

		LOG.info(LS + "Server STOPPED");
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

	/**
	 */
	public Logger getLoggerFor(String pName) {
		return Logger.getLogger(pName);
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
	public JamnServer addContentProvider(String pId, ContentProvider pProvider) {
		requestProcessor.addContentProvider(pId, pProvider);
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
	public JamnServer setHttpHelper(HttpHelper pHelper) {
		HttpHelper = pHelper;
		return this;
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

					requestExecutor.execute(new Runnable() {
						@Override
						public void run() {
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
										LOG.fine("ClientSocket closed: " + (System.currentTimeMillis() - start) + " - "
												+ Thread.currentThread().getName());
									}
								}
							} catch (IOException e) {
								// nothing to do
							}
						}
					});
				}
			} catch (IOException e) {
				// nothing to do
			} finally {
				LOG.fine("ServerThread finished: " + Thread.currentThread().getName());
			}
		}
	}

	/*********************************************************
	 * <pre>
	 * The central processing interfaces and default implementations.
	 * The interfaces only use themselves or standard Java data types.
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
		 */
		void handleRequest(InputStream pIn, OutputStream pOut, Socket pSocket, Map<String, String> pComData)
				throws IOException;

		/**
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
	public static class DefaultRequestProcessor implements RequestProcessor, HttpConstants {

		protected Properties props = new Properties();
		protected boolean isCORSEnabled = false;

		// empty default provider dispatcher
		protected ContentProviderDispatcher contentDispatcher = new ContentProviderDispatcher() {
			@Override
			public String getContentProviderIDFor(Map<String, String> pRequestAttributes) {
				LOG.warning("WARNING - The empty DEFAULT ContentProvide-Dispatcher was invoked although there are ["+contentProviderMap.size()+"] provider registered."+LS+"Please add a dispatcher to avoid this message.");
				return null;
			}

		};
		// the available ContentProvider
		protected Map<String, ContentProvider> contentProviderMap = new HashMap<>();
		// an empty default provider
		protected ContentProvider defaultContentProvider = new ContentProvider() {
			@Override
			public String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
					String pMethod, String pPath, String pRequestBody, final Map<String, String> pRequestAttributes) {
				// just for startup
				// so http://localhost:<port> should show blank to indicate server is alive
				if ("/".equals(pPath)) {
					return HTTP_200_OK;
				}
				// anything else should show error
				return HTTP_404_NOT_FOUND;
			}
		};

		public DefaultRequestProcessor() {
		}

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
			String pProviderId = contentDispatcher.getContentProviderIDFor(pRequestAttributes);
			return contentProviderMap.getOrDefault(pProviderId, defaultContentProvider);
		}

		/**
		 */
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

			isCORSEnabled = Boolean.valueOf(props.getProperty(HTTP_CORS_ENABLED, "false"));
			return this;
		}

		/**
		 * A default handling. Read an incoming http request and reply a basic http
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

			} catch (Throwable t) {
				if (t instanceof InterruptedIOException) {
					lResponse.sendStatus(HTTP_408_TIMEOUT);
				} else if (t instanceof RuntimeException) {
					lResponse.sendStatus(HTTP_500_INTERNAL_ERROR);
					LOG.severe("Request Handling internal/runtime ERROR: " + t.toString() + LS + Helper.getStackTraceFrom(t));
				}
			} finally {
				lResponse.close();
			}
		}

		/**
		 */
		public static class Response {
			protected String[] statusline = new String[] { HTTPVAL_MIN_VERSION, "" };
			protected Map<String, String> headerAttributes = new LinkedHashMap<>(HTTP_DEFAULT_RESPONSE_ATTRIBUTES);

			protected OutputStream outStream = null;
			protected ByteArrayOutputStream contentBuffer = null;

			public Response(OutputStream pOutStream, boolean pIsCORSEnabled) {
				outStream = pOutStream;
				setHttpStatus(HTTP_408_TIMEOUT);
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
					headerAttributes.put(HTTP_ACCESS_CONTROL_ALLOW_ORIGIN, HTTPVAL_ACCESS_CONTROL_ALLOW_ORIGIN_ALL);
					headerAttributes.put(HTTP_ACCESS_CONTROL_ALLOW_METHODS, HTTPVAL_ACCESS_CONTROL_ALLOW_METHODS_ALL);
					headerAttributes.put(HTTP_ACCESS_CONTROL_ALLOW_HEADERS, HTTPVAL_ACCESS_CONTROL_ALLOW_HEADERS_ALL);
				}
				return this;
			}

			public Response setContent(ByteArrayOutputStream pValue) {
				contentBuffer = pValue;
				return this;
			}

			private void send(boolean pWithContent) throws IOException {
				int lContentLen = (contentBuffer != null && pWithContent) ? contentBuffer.size() : 0;

				headerAttributes.put(HTTP_CONTENT_LENGTH, String.valueOf(lContentLen));
				StringBuilder lHeader = HttpHelper.createHttpHeader(statusline, headerAttributes, lContentLen);

				ByteArrayOutputStream lMsg = new ByteArrayOutputStream();
				lMsg.write(lHeader.toString().getBytes());
				if (pWithContent && lContentLen > 0) {
					lMsg.write(contentBuffer.toByteArray());
					contentBuffer.flush();
				}
				outStream.write(lMsg.toByteArray());
				lMsg.flush();
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
			protected boolean wantKeepAliveConnection = false;

			public Request() {
			}

			/**
			 */
			public void readHeader(InputStream pInStream) throws IOException {
				header = HttpHelper.readHttpRequestHeader(pInStream).toString();
				headerAttributes = Collections.unmodifiableMap(HttpHelper.parseHttpHeader(header));
				wantKeepAliveConnection = getHeaderAttribute(HTTP_CONNECTION, "").equalsIgnoreCase(HTTPVAL_KEEP_ALIVE);

				LOG.fine(Thread.currentThread().getName() + " - Request header: " + header);
			}

			/**
			 */
			public void readBody(InputStream pInStream) throws IOException {
				int lLength = getContentLength();
				if (lLength > 0) {
					body = HttpHelper.readHttpRequestBody(pInStream, lLength).toString();
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
			public boolean wantKeepAliveConnection() {
				return wantKeepAliveConnection;
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
				return Integer.valueOf(getHeaderAttribute(HTTP_CONTENT_LENGTH, "0"));
			}

			/**
			 */
			public String getHeaderAttribute(String pName, String pDefault) {
				return headerAttributes.getOrDefault(pName, pDefault);
			}
		}
	}

	/*********************************************************
	 * <pre>
	 * A HTTP Helper class to encapsulate http specific things.
	 * The main functionality is a very basic Header parser.
	 * </pre>
	 *********************************************************/
	/**
	 */
	public static interface HttpConstants {
		// HTTP header attribute names
		String HTTP_CONTENT_TYPE = "Content-Type";
		String HTTP_CONNECTION = "Connection";
		String HTTP_CONTENT_LENGTH = "Content-Length";
		String HTTP_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
		String HTTP_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
		String HTTP_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

		// HTTP header attribute values
		String HTTPVAL_MIN_VERSION = "HTTP/1.0";
		String HTTPVAL_KEEP_ALIVE = "keep-alive";
		String HTTPVAL_CONTENT_TYPE_TEXT = "text/plain";
		String HTTPVAL_CONTENT_TYPE_XML = "text/xml";
		String HTTPVAL_CONTENT_TYPE_HTML = "text/html";
		String HTTPVAL_CONTENT_TYPE_JSON = "application/json";
		String HTTPVAL_CONTENT_TYPE_IMG_PNG = "image/png";
		String HTTPVAL_CLOSE = "close";
		String HTTPVAL_ACCESS_CONTROL_ALLOW_ORIGIN_ALL = "*";
		String HTTPVAL_ACCESS_CONTROL_ALLOW_METHODS_ALL = "GET, POST, PATCH, PUT, DELETE, OPTIONS";
		String HTTPVAL_ACCESS_CONTROL_ALLOW_HEADERS_ALL = "Origin, Content-Type, X-Auth-Token";

		// HTTP status values
		String HTTP_200_OK = "200";
		String HTTP_400_BAD_REQUEST = "400";
		String HTTP_404_NOT_FOUND = "404";
		String HTTP_405_METHOD_NOT_ALLOWED = "405";
		String HTTP_408_TIMEOUT = "408";
		String HTTP_204_NO_CONTENT = "204";
		String HTTP_500_INTERNAL_ERROR = "500";

		// HTTP status value text parts
		@SuppressWarnings("serial")
		public static final Map<String, String> HTTP_STATUS = new HashMap<String, String>() {
			{
				put("200", "OK");
				put("201", "Created");
				put("204", "No Content");
				put("400", "Bad Request");
				put("404", "Not found");
				put("405", "Method Not Allowed");
				put("406", "Not Acceptable");
				put("408", "Request Timeout");
				put("411", "Length Required");
				put("500", "Internal Server Error");
				put("503", "Service Unavailable");
			}
		};

		// self defined attribute constants related to http
		// NOT for use in outgoing http headers
		String HTTP_CORS_ENABLED = "http-cors-enabled";
		String HTTP_METHOD = "http-method";
		String HTTP_PATH = "http-path";
		String HTTP_VERSION = "http-version";
		String HTTP_RESPONSE_STATUS = "http-response-status";
		String HTTP_VERSION_MARK = "HTTP/";

		@SuppressWarnings("serial")
		public static final Map<String, String> HTTP_DEFAULT_RESPONSE_ATTRIBUTES = new LinkedHashMap<String, String>() {
			{
				put(HTTP_CONTENT_TYPE, HTTPVAL_CONTENT_TYPE_TEXT);
				put(HTTP_CONTENT_LENGTH, "0");
			}
		};
	}

	/**
	 * <pre>
	 * This class implements a few central functions for the HTTP specific processing of a client request. 
	 * In particular, reading the input stream and parsing the header informations.
	 * </pre>
	 */
	public static class HttpHelper implements HttpConstants {

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
					break; // end of stream, because pIn.available() may ? not be reliable enough
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

				// empty line reached
				if (headerEndFlag == 2) {
					break;
				}

			} while (pInStream.available() > 0);
			return lBuffer;
		}

		/**
		 * Starting at the CURRENT position of the input stream reading until
		 * pContentLen bytes are read. TODO i != pContentLen. This is called after
		 * readHttpRequestHeader.
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
			Map<String, String> lAttributes = new LinkedHashMap<>();
			String[] lLines = pHeader.split("\\n");

			for (int i = 0; i < lLines.length; i++) {
				String lVal = "";
				String[] lParts = null;
				String[] lSubParts = null;
				String lLine = lLines[i];
				if (i > 0 && lLine.contains(":")) {
					lParts = lLine.split(":");
					if (lParts.length == 2) {
						lAttributes.put(lParts[0].trim(), lParts[1].trim());
					} else if (lParts.length > 2) {
						lVal = lParts[1].trim();
						for (int k = 1; k + 1 < lParts.length; k++) {
							lVal = lVal + ":" + lParts[k + 1].trim();
						}
						lAttributes.put(lParts[0].trim(), lVal);
					}
				} else if (i == 0) {
					// parse status line as "self defined attributes"
					lParts = lLine.split(" ");
					if (lParts.length > 0) {
						for (int j = 0; j < lParts.length; j++) {
							if (j == 0) {
								// always bring method names to Upper Case
								lAttributes.put(HTTP_METHOD, lParts[j].trim().toUpperCase());
							} else if (lParts[j].trim().toUpperCase().contains(HTTP_VERSION_MARK)) {
								lSubParts = lParts[j].trim().split("/");
								if (lSubParts.length == 2) {
									lAttributes.put(HTTP_VERSION, lSubParts[1].trim());
								}
							} else if (lParts[j].trim().contains("/")) {
								lAttributes.put(HTTP_PATH, lParts[j].trim());
							}
						}
					}
				}
			}
			return lAttributes;
		}

		/**
		 */
		public StringBuilder createHttpHeader(String[] pStatusLine, Map<String, String> pAttributes, int pContentLen) {
			StringBuilder lHeader = new StringBuilder(String.join(" ", pStatusLine));
			for (String key : pAttributes.keySet()) {
				lHeader.append(CRLF).append(key).append(": ").append(pAttributes.get(key));
			}
			lHeader.append(CRLF);
			if (pContentLen > 0) {
				lHeader.append(CRLF);
			}
			return lHeader;
		}

		/**
		 */
		public String getHttpStatus(Object pNr) {
			String lNr = String.valueOf(pNr).trim();
			if (HTTP_STATUS.containsKey(lNr)) {
				StringBuilder lStatus = new StringBuilder(lNr);
				lStatus.append(" ").append(HTTP_STATUS.get(lNr));
				return lStatus.toString();
			}
			return lNr;
		};
	}

	/*********************************************************
	 * <pre>
	 * A configuration properties class.
	 * </pre>
	 *********************************************************/
	/**
	 */
	public static class Config {
		public static final String DEFAULT_CONFIG = String.join("\n", new String[] { "port=8099", "worker=5",
				"protocol=http", "encoding=UTF-8", "client-socket-timeout=10000", "http-cors-enabled=false" });

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
		public int getWorkerNumber() {
			return Integer.valueOf(props.getProperty("worker", "10"));
		}

		/**
		 */
		public String getProtocol() {
			return props.getProperty("protocol", "http");
		}

		/**
		 */
		public int getClientSocketTimeout() {
			return Integer.valueOf(props.getProperty("clientSocketTimeout", "10000"));
		}

		/**
		 */
		public boolean useHttp() {
			return getProtocol().equalsIgnoreCase("http");
		}

		/**
		 */
		public boolean isCORSEnabled() {
			return Boolean.valueOf(props.getProperty("http-cors-enabled", "false")).booleanValue();
		}

		/**
		 */
		public void setCORSEnabled(boolean pVal) {
			props.setProperty("http-cors-enabled", String.valueOf(pVal));
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
				LOG.severe("ERROR parsing config to properties string [" + pDef + "]");
				throw new RuntimeException(e);
			}
			return lProps;
		}
	}

	/*********************************************************
	 * <pre>
	 * A common helper class - just to encapsulate helper methods from server code.
	 * </pre>
	 *********************************************************/
	/**
	 */
	public static class CommonHelper {

		/**
		 */
		public String getStackTraceFrom(Throwable t) {
			PrintWriter lWriter = new PrintWriter(new StringWriter());
			t.printStackTrace(lWriter);
			return lWriter.toString();
		}
	}
}
