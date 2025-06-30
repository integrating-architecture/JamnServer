/* Authored by www.integrating-architecture.de */
package org.isa.ipc;

import static org.isa.ipc.JamnServer.HttpHeader.HTTP_DEFAULT_RESPONSE_ATTRIBUTES;
import static org.isa.ipc.JamnServer.HttpHeader.HTTP_DEFAULT_WEBSOCKET_RESPONSE_ATTRIBUTES;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_1_1;
import static org.isa.ipc.JamnServer.HttpHeader.Field.SEC_WEBSOCKET_ACCEPT;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.UPGRADE;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_101_SWITCH_PROTOCOLS;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_204_NO_CONTENT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.HttpHeader;
import org.isa.ipc.JamnServer.RequestMessage;
import org.isa.ipc.JamnServer.ResponseMessage;

/**
 * <pre>
 * A rudimentary WebSocket Provider implementation for the JamnServer.
 * The construction consists of 
 *  - the top level - JamnWebSocketProvider - added to the Server
 *  - that creates a - WebSocketHandler - for every incoming connection request
 *  
 *  The WebSocketHandler implements the main WebSocket logic and behavior.
 *   - in particular - the WebSocket "message format magic" - see: readIncomingMessages + encodeMessage
 *   
 *  In the present implementation there is also a WsoConnectionManager object involved
 *  that holds a references to each established handler. 
 *  How ever - since every handler represents a client connection
 *  the manager is intended to make active server-side communication possible.
 *  E.g. broadcasting messages or things like that.
 *  
 *  The "business logic" of a WebSocket is implemented in a WsoMessageProcessor
 *  associated with one WebSocket-connection-path (resp. handler) supporting n client connections.
 *  
 *  You can easily play with this things by using e.g.:
 *  JamnWebSocketProvider\src\test\..\..\sample\browser-js-websocket-call.html
 * </pre>
 */
public class JamnWebSocketProvider implements JamnServer.ContentProvider.UpgradeHandler {

    // default websocket connection url: "ws://host:port/wsoapi"
    public static final String DefaultPath = "/wsoapi";

    protected static final String LS = System.lineSeparator();
    protected static Logger LOG = Logger.getLogger(JamnWebSocketProvider.class.getName());

    protected WsoConnectionManager connectionManager = new WsoConnectionManager();

    // a empty default access controller
    protected WsoAccessController accessCtrl = new WsoAccessController() {

        @Override
        public boolean isSupportedPath(String pPath, StringBuilder pMsg) {
            if (connectionPathNames.contains(pPath)) {
                return true;
            }
            pMsg.append("Unsupported path [").append(pPath).append("]");
            return false;
        }

        @Override
        public boolean isAccessGranted(Map<String, String> pRequestAttributes, StringBuilder pMsg) {
            return true;
        }
    };

    protected Set<String> connectionPathNames = new HashSet<>();

    /**
     */
    private JamnWebSocketProvider() {
        addConnectionPath(DefaultPath);
    }

    /*********************************************************
     * Public Interface
     *********************************************************/
    /**
     */
    public static JamnWebSocketProvider newBuilder() {
        return new JamnWebSocketProvider();
    }

    /**
     * <pre>
     * WebSocket connections base on an one time, initial url path.
     * After a connection was established - there are NO pathnames involved any more.
     * 
     * How ever - it can be useful to distinguish different task areas already when connecting.
     * </pre>
     */
    public JamnWebSocketProvider addConnectionPath(String pPath) {
        connectionPathNames.add(pPath);
        return this;
    }

    /**
     */
    public Set<String> getConnectionPathNames() {
        return connectionPathNames;
    }

    /**
     */
    public JamnWebSocketProvider setAccessController(WsoAccessController pCtrl) {
        accessCtrl = pCtrl;
        return this;
    }

    /**
     */
    public void addMessageProcessor(WsoMessageProcessor pProcessor, String... pPath) {
        String lPath = (pPath != null && pPath.length == 1) ? pPath[0] : DefaultPath;
        if (connectionPathNames.contains(lPath)) {
            connectionManager.addMessageProcessor(pProcessor, lPath);
        } else {
            throw new UncheckedWebSocketException(
                    String.format("WebSocket Message Processor for unknown path [%s]", lPath));
        }
    }

    /**
     */
    public void sendMessageTo(String pConnectionId, byte[] pMessage) {
        if (connectionManager.isConnectionAvailable(pConnectionId)) {
            connectionManager.sendMessageFor(pConnectionId, pMessage);
        }
    }

    /**
     */
    public JamnWebSocketProvider build() {
        return this;
    }

    /*********************************************************
     * End - Public Interface
     *********************************************************/

    /*********************************************************
     * <pre>
     * The Jamn WebSocket-Server implementations.
     * </pre>
     *********************************************************/

    /**
     * The JamnServer.ContentProvider.UpgradeHandler Interface method.
     */
    @Override
    public String handleRequest(RequestMessage pRequest, InputStream pSocketInStream, OutputStream pSocketOutStream,
            Socket pSocket, Map<String, String> pComData) {

        WebSocketHandler lHandler = new WebSocketHandler(pRequest.getPath(), false, accessCtrl, connectionManager);
        lHandler.handleRequest(pRequest.header(), pSocketInStream, pSocketOutStream,
                pSocket,
                pComData);
        return null;
    }

    /*********************************************************
     * <pre>
     * The Jamn WebSocket-Server implementations.
     * </pre>
     *********************************************************/
    /**
     * <pre>
     * The WsoConnectionManager holds the established connections to be identified by the ConnectionId.
     * 
     * A connection is represented by a WebSocketHandler=WsoConnection.
     * </pre>
     */
    private static class WsoConnectionManager {
        // connectionId -> connection
        protected Map<String, WsoConnection> openConnections = Collections.synchronizedMap(new HashMap<>());

        // path -> processor
        protected Map<String, WsoMessageProcessor> processorMap = Collections.synchronizedMap(new HashMap<>());

        /**
         */
        protected synchronized void connectionEstablished(String pConnectionId, WsoConnection pConnection) {
            openConnections.put(pConnectionId, pConnection);
        }

        /**
         */
        protected synchronized void connectionClosed(String pConnectionId) {
            openConnections.remove(pConnectionId);
            LOG.info(() -> String.format("Closed WebSocket connection [%s]", pConnectionId));
        }

        /**
         * <pre>
         * This method is called for every incoming client "message" read from a WebSocketConnection.
         * </pre>
         */
        protected void processMessageFor(String pConnectionId, byte[] pMessage) {
            WsoMessageProcessor lProcessor;
            WsoConnection lConnection = openConnections.getOrDefault(pConnectionId, null);

            if (lConnection != null) {
                lProcessor = processorMap.getOrDefault(lConnection.getPath(), null);
                if (lProcessor != null) {
                    byte[] lResponse = lProcessor.onMessage(pConnectionId, pMessage);
                    // if response available - send it back
                    if (lResponse != null && lResponse.length > 0) {
                        sendMessageFor(pConnectionId, lResponse);
                    }
                }
            }
        }

        /**
         * A WebSocket is a connection established at one single access-point-path but
         * shared by all clients. Insofar is a WebSocket also associated with one
         * Processor that implements it's behavior.
         */
        protected void addMessageProcessor(WsoMessageProcessor pProcessor, String pPath) {
            if (!processorMap.containsKey(pPath)) {
                processorMap.put(pPath, pProcessor);
            } else {
                throw new UncheckedWebSocketException(
                        String.format("WebSocket Message Processor already defined for path [%s]", pPath));
            }
        }

        /**
         * The method implements the way from the WebSocket server side - back to a
         * connected client.
         */
        protected void sendMessageFor(String pConnectionId, byte[] pMessage) {
            WsoConnection lCon = openConnections.getOrDefault(pConnectionId, null);
            if (lCon != null) {
                lCon.sendMessage(pMessage);
            }
        }

        /**
         */
        protected boolean isConnectionAvailable(String pConnectionId) {
            return openConnections.containsKey(pConnectionId);
        }

    }

    /**
     */
    public static class WebSocketConnectionRejectedException extends Exception {
        private static final long serialVersionUID = 1L;

        WebSocketConnectionRejectedException(String pMsg) {
            super(pMsg);
        }
    }

    /**
     */
    public static class UncheckedWebSocketException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UncheckedWebSocketException(String pMsg) {
            super(pMsg);
        }

        UncheckedWebSocketException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }

    /**
     */
    protected static class WebSocketHandler implements WsoConnection {

        protected String connectionId = "";
        protected String initUrlPath = "";
        protected boolean isCORSEnabled = false;
        protected OutputStream outStream;
        protected WsoAccessController accessCtrl;
        protected WsoConnectionManager connectionManager;

        protected WebSocketHandler() {
        }

        public WebSocketHandler(String pInitUrlPath, boolean pCORSEnabled, WsoAccessController pCtrl,
                WsoConnectionManager pConManager) {
            this();
            initUrlPath = pInitUrlPath;
            isCORSEnabled = pCORSEnabled;
            accessCtrl = pCtrl;
            connectionManager = pConManager;
        }

        /**
         */
        @Override
        public String geConnectiontId() {
            return connectionId;
        }

        @Override
        public String getPath() {
            return initUrlPath;
        }

        @Override
        public void sendMessage(byte[] pMessage) {
            if (outStream != null) {
                try {
                    byte[] encodedBytes = encodeMessage(pMessage);

                    outStream.write(encodedBytes);
                    outStream.flush();
                } catch (IOException e) {
                    throw new UncheckedWebSocketException(String.format("WebSocket send message error: [%s]",
                            geConnectiontId()), e);
                }
            }
        }

        /**
         * Interface method for (default)request processor.
         */
        protected void handleRequest(HttpHeader pRequestHeader, InputStream pSocketInStream,
                OutputStream pSocketOutStream, Socket pSocket, Map<String, String> pComData) {
            // setting a mark for the top level server socket thread
            pComData.put(JamnServer.WEBSOCKET_PROVIDER, "");

            InputStream lInStream = new BufferedInputStream(pSocketInStream, 4096);
            outStream = new BufferedOutputStream(pSocketOutStream, 4096);

            StringBuilder lErrorMsg = new StringBuilder();
            HttpHeader lHandShakeHeader = new HttpHeader().initWith(HTTP_DEFAULT_RESPONSE_ATTRIBUTES)
                    .setHttpVersion(HTTP_1_1).setHttpStatus(SC_204_NO_CONTENT);
            ResponseMessage lHandshakeResponse = new ResponseMessage(outStream, lHandShakeHeader);

            try {
                // check accessibility
                if (!accessCtrl.isSupportedPath(initUrlPath, lErrorMsg)
                        || !accessCtrl.isAccessGranted(pRequestHeader.getAttributes(), lErrorMsg)) {
                    lHandshakeResponse.send();
                    throw new WebSocketConnectionRejectedException(
                            String.format("WebSocket connection rejected [%s] [%s]", getPath(), lErrorMsg));
                }
                try {
                    // accept handshake
                    lHandShakeHeader.initWith(HTTP_DEFAULT_WEBSOCKET_RESPONSE_ATTRIBUTES).setHttpVersion(HTTP_1_1)
                            .setHttpStatus(SC_101_SWITCH_PROTOCOLS)
                            .setConnection(UPGRADE)
                            .set(SEC_WEBSOCKET_ACCEPT, createWebSocketAcceptKey(pRequestHeader.getWebSocketKey()));

                    lHandshakeResponse.send();

                    // create a unique connectionId
                    connectionId = initUrlPath + " - " + Integer.toHexString(pSocket.hashCode()) + "-"
                            + pSocket.toString();
                    // register this connection at the WsoConnectionManager
                    connectionManager.connectionEstablished(connectionId, this);

                    LOG.info(() -> String.format("%sWebSocket connection established [%s]%s%s", LS, connectionId, LS,
                            lHandShakeHeader.toString().trim()));

                } catch (Exception e) {
                    lHandShakeHeader.setHttpStatus(SC_500_INTERNAL_ERROR);
                    lHandshakeResponse.send();
                    throw e;
                }

                // from here the io is websocket specific
                // and NO longer bound to the http protocol

                // the processing blocks reading the lInStream until connection is closed
                // every read is considered as a "message"
                // and is forwarded/published to the ConnectionManager
                // that tries to find the processor for it
                pSocket.setSoTimeout(0);
                readIncomingMessages(lInStream, outStream, connectionManager, connectionId);

                // returning from reading lInStream
                // means the stream returned -1, end of stream and closed
                outStream.flush();
                outStream.close();

            } catch (Exception e) {
                throw new UncheckedWebSocketException(String.format("WebSocket request handling error: [%s]",
                        geConnectiontId()), e);
            } finally {
                // remove connection from the ConnectionManager
                connectionManager.connectionClosed(connectionId);
                try {
                    pSocket.close();
                } catch (IOException e) {
                    // hmm ?
                }
            }
        }

        /**
         * @throws NoSuchAlgorithmException
         */
        protected String createWebSocketAcceptKey(String pRequestKey)
                throws NoSuchAlgorithmException {
            String lKey = pRequestKey + HttpHeader.MAGIC_WEBSOCKET_GUID;
            byte[] lSha1 = MessageDigest.getInstance("SHA-1").digest(lKey.getBytes(StandardCharsets.UTF_8));
            lKey = Base64.getEncoder().encodeToString(lSha1);
            return lKey;
        }

        /**
         * <pre>
         * taken from
         * https://stackoverflow.com/questions/43163592/standalone-websocket-server-without-jee-application-server
         * and I admit, I have no idea what the method really does - but it actually reads messages ;)
         * </pre>
         */
        protected void readIncomingMessages(InputStream pInStream, OutputStream pOutStream,
                WsoConnectionManager pConnectionManager, String pConnectionId) throws IOException {

            boolean connected = true;
            int readPacketLength = 0;
            byte[] packet = new byte[1024];
            ByteArrayOutputStream packetStream = new ByteArrayOutputStream();
            byte[] message;

            while (connected) {
                readPacketLength = pInStream.read(packet);

                if (readPacketLength == -1) {
                    break;
                } else {
                    if ((packet[0] & (byte) 15) == (byte) 8) { // Disconnect packet
                        // returning the same but encoded packet for client to terminate connection
                        packet = encodeMessage(packet);
                        pOutStream.write(packet, 0, readPacketLength);
                        pOutStream.flush();
                        break;
                    }
                    byte messageLengthByte = 0;
                    int messageLength = 0;
                    int maskIndex = 2;
                    int messageStart = 0;
                    // b[0] is always text in my case so no need to check
                    byte data = packet[1];
                    byte op = (byte) 127; // 0111 111
                    messageLengthByte = (byte) (data & op);

                    int totalPacketLength = 0;
                    if (messageLengthByte == (byte) 126 || messageLengthByte == (byte) 127) {
                        if (messageLengthByte == (byte) 126) {
                            maskIndex = 4;
                            // if messageLengthInt==(byte)126, then 16-bit length is stored in packet[2] and
                            // [3]
                            ByteBuffer messageLength16Bit = ByteBuffer.allocateDirect(4);
                            messageLength16Bit.order(ByteOrder.BIG_ENDIAN);
                            messageLength16Bit.put((byte) 0x00);
                            messageLength16Bit.put((byte) 0x00);
                            messageLength16Bit.put(packet, 2, 2);
                            messageLength16Bit.flip();
                            messageLength = messageLength16Bit.getInt();
                            totalPacketLength = messageLength + 8;
                        } else {
                            maskIndex = 10;
                            // if messageLengthInt==(byte)127, then 64-bit length is stored in bytes [2]
                            // to [9]. Using only 32-bit
                            ByteBuffer messageLength64Bit = ByteBuffer.allocateDirect(4);
                            messageLength64Bit.order(ByteOrder.BIG_ENDIAN);
                            messageLength64Bit.put(packet, 6, 4);
                            messageLength64Bit.flip();
                            messageLength = messageLength64Bit.getInt();
                            totalPacketLength = messageLength + 14;
                        }

                        if (readPacketLength != totalPacketLength) {
                            packetStream.write(packet, 0, readPacketLength);

                            int lastPacketLength = 0;
                            while (readPacketLength < totalPacketLength) {
                                packet = new byte[1024];
                                readPacketLength += lastPacketLength = pInStream.read(packet);
                                packetStream.write(packet, 0, lastPacketLength);
                            }
                            packet = packetStream.toByteArray();
                            packetStream.reset();
                        }
                    } else { // using message length from packet[1]
                        messageLength = messageLengthByte;
                    }

                    byte[] masks = new byte[4];
                    int i = 0;
                    int j = 0;
                    for (i = maskIndex; i < (maskIndex + 4); i++) {
                        masks[j] = packet[i];
                        j++;
                    }

                    messageStart = maskIndex + 4;

                    message = new byte[messageLength];
                    for (i = messageStart, j = 0; i < readPacketLength; i++, j++) {
                        message[j] = (byte) (packet[i] ^ masks[j % 4]);
                    }
                    packet = new byte[1024];

                    // after reading and hopefully decoding
                    // forward message to the message-processor
                    pConnectionManager.processMessageFor(pConnectionId, message);
                }
            }
        }

        /**
         * taken from
         * https://stackoverflow.com/questions/43163592/standalone-websocket-server-without-jee-application-server
         */
        protected byte[] encodeMessage(byte[] rawData) {

            int frameCount = 0;
            byte[] frame = new byte[10];

            frame[0] = (byte) 129;

            if (rawData.length <= 125) {
                frame[1] = (byte) rawData.length;
                frameCount = 2;
            } else if (rawData.length >= 126 && rawData.length <= 65535) {
                frame[1] = (byte) 126;
                int len = rawData.length;
                frame[2] = (byte) ((len >> 8) & ((byte) 255 & 0xff));
                frame[3] = (byte) (len & (byte) 255);
                frameCount = 4;
            } else {
                frame[1] = (byte) 127;
                // org - int len = rawData.length
                long len = rawData.length; // note an int is not big enough in java
                frame[2] = (byte) ((len >> 56) & ((byte) 255 & 0xff));
                frame[3] = (byte) ((len >> 48) & ((byte) 255 & 0xff));
                frame[4] = (byte) ((len >> 40) & ((byte) 255 & 0xff));
                frame[5] = (byte) ((len >> 32) & ((byte) 255 & 0xff));
                frame[6] = (byte) ((len >> 24) & ((byte) 255 & 0xff));
                frame[7] = (byte) ((len >> 16) & ((byte) 255 & 0xff));
                frame[8] = (byte) ((len >> 8) & ((byte) 255 & 0xff));
                frame[9] = (byte) (len & ((byte) 255 & 0xff));
                frameCount = 10;
            }

            int bLength = frameCount + rawData.length;

            byte[] reply = new byte[bLength];

            int bLim = 0;
            for (int i = 0; i < frameCount; i++) {
                reply[bLim] = frame[i];
                bLim++;
            }
            for (int i = 0; i < rawData.length; i++) {
                reply[bLim] = rawData[i];
                bLim++;
            }

            return reply;
        }

    }

    /**
     * <pre>
     * The Processor defines the message listener on the Server Side.
     * The contract is byte data - in and out.
     * A concrete WsoMessageProcessor Implementation is responsible for anything else.
     * </pre>
     */
    public static interface WsoMessageProcessor {

        /**
         */
        public byte[] onMessage(String pConnectionId, byte[] pMessage);

    }

    /**
     * <pre>
     * </pre>
     */
    public static interface WsoConnection {

        /**
         */
        public String geConnectiontId();

        /**
         * The initial url connection path.
         */
        public String getPath();

        /**
         * Send data to the client that established the connection.
         */
        public void sendMessage(byte[] pMessage);

    }

    /**
     * <pre>
     * A rudimentary "security" interface.
     * </pre>
     */
    public static interface WsoAccessController {
        /**
         */
        public boolean isSupportedPath(String pPath, StringBuilder pMsg);

        /**
         */
        public boolean isAccessGranted(Map<String, String> pRequestAttributes, StringBuilder pMsg);

    }

    /**
     */
    protected static String getStackTraceFrom(Throwable t) {
        return JamnServer.getStackTraceFrom(t);
    }

}
