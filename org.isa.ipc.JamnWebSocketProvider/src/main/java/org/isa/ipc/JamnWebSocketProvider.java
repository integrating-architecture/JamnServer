/* Authored by www.integrating-architecture.de */
package org.isa.ipc;

import static org.isa.ipc.JamnServer.HttpHeader.HTTP_DEFAULT_RESPONSE_ATTRIBUTES;
import static org.isa.ipc.JamnServer.HttpHeader.HTTP_DEFAULT_WEBSOCKET_RESPONSE_ATTRIBUTES;
import static org.isa.ipc.JamnServer.HttpHeader.Field.HTTP_1_1;
import static org.isa.ipc.JamnServer.HttpHeader.Field.SEC_WEBSOCKET_ACCEPT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_101_SWITCH_PROTOCOLS;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_204_NO_CONTENT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.Config;
import org.isa.ipc.JamnServer.HttpHeader;

/**
 * A rudimentary WebSocket Provider implementation for the JamnServer.
 */
public class JamnWebSocketProvider implements JamnServer.ContentProvider.UpgradeHandler {

    protected static final String LS = System.getProperty("line.separator");
    protected static Logger LOG = Logger.getLogger(JamnWebSocketProvider.class.getName());

    protected static WebSocketMessageBroker WSOMessageBroker = new WebSocketMessageBroker();

    protected WsoAccessController accessCtrl = new WsoAccessController() {

        @Override
        public boolean isSupportedTopic(String pTopic, StringBuilder pMsg) {
            if (pTopic.equalsIgnoreCase("/wsapi")) {
                return true;
            }
            pMsg.append("Unsupported path [").append(pTopic).append("]");
            return false;
        }

        @Override
        public boolean isAccessGranted(Map<String, String> pRequestAttributes, StringBuilder pMsg) {
            return true;
        }
    };

    protected Config config = new Config();

    /**
     */
    private JamnWebSocketProvider() {
    }

    protected static WebSocketMessageBroker getWebSocketBroker() {
        return WSOMessageBroker;
    }

    /*********************************************************
     * Public Interface
     *********************************************************/
    /**
     */
    public static JamnWebSocketProvider Builder() {
        JamnWebSocketProvider lProvider = new JamnWebSocketProvider();
        return lProvider;
    }

    /**
     */
    public JamnWebSocketProvider setConfig(Config pConfig) {
        config = pConfig;
        return this;
    }

    /**
     */
    public JamnWebSocketProvider setAccessController(WsoAccessController pCtrl) {
        accessCtrl = pCtrl;
        return this;
    }

    /**
     */
    public JamnWebSocketProvider build() {
        return this;
    }

    /**
     */
    public JamnWebSocketProvider addMessageConsumer(WsoMessageConsumer pConsumer) {
        getWebSocketBroker().addMessageConsumer(pConsumer);
        return this;
    }

    /**
     */
    public WebSocketMessageBroker.WebSocketSender createMessageSenderFor(String pConnectionId, String pTopic) {
        return new WebSocketMessageBroker.WebSocketSender(pConnectionId, pTopic);
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
    public String handleRequest(String pMethod, String pPath, Map<String, String> pRequestAttributes,
            InputStream pSocketInStream, OutputStream pSocketOutStream, Socket pSocket, Map<String, String> pComData) {

        WebSocketConnectionHandler lHandler = new WebSocketConnectionHandler(pPath, false, accessCtrl);
        lHandler.handleRequest(new HttpHeader(pRequestAttributes), pSocketInStream, pSocketOutStream, pSocket,
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
     * The WebSocketMessageBroker mediates between the established WebSocket connections
     * and the application specific message consumer and sender.
     * 
     * The current implementation is very rudimentary, not secure and does not scale.
     * However it's task is to connect incoming messages with consumers.
     * A place for your ideas.
     * </pre>
     */
    private static class WebSocketMessageBroker {
        // connectionId -> topic -> sender/handler
        Map<String, Map<String, WsoMessageSender>> openConnections = Collections.synchronizedMap(new HashMap<>());

        // topic -> consumer list
        Map<String, List<WsoMessageConsumer>> consumerMap = Collections.synchronizedMap(new HashMap<>());

        /**
         */
        protected synchronized void connectionEstablished(String pConnectionId, WsoMessageSender pSender) {
            Map<String, WsoMessageSender> lTopics = Collections.synchronizedMap(new HashMap<>());
            openConnections.put(pConnectionId, lTopics);
            lTopics.put(pSender.getTopic(), pSender);
        }

        /**
         */
        protected synchronized void connectionClosed(String pConnectionId, WsoMessageSender pSender) {
            openConnections.remove(pConnectionId);
        }

        /**
         * <pre>
         * This method is called for every incoming client "message" read from a WebSocketConnection.
         * The current implementation does not scale because all known consumers for the topic are simply called synchronously.
         * </pre>
         */
        protected void publishMessageFor(String pConnectionId, String pTopic, byte[] pMessage) {
            List<WsoMessageConsumer> lList = consumerMap.get(pTopic);
            if (lList != null) {
                lList.forEach(consumer -> consumer.onMessage(pConnectionId, pMessage));
            }
        }

        /**
         */
        protected void addMessageConsumer(WsoMessageConsumer pConsumer) {
            List<WsoMessageConsumer> lList = consumerMap.get(pConsumer.getTopic());
            if (lList == null) {
                lList = new ArrayList<WsoMessageConsumer>();
                consumerMap.put(pConsumer.getTopic(), lList);
            }
            lList.add(pConsumer);
        }

        /**
         */
        protected void sendMessageFor(String pConnectionId, String pTopic, byte[] pMessage) {
            Map<String, WsoMessageSender> lTopics = openConnections.getOrDefault(pConnectionId, null);
            if (lTopics != null && lTopics.containsKey(pTopic)) {
                lTopics.get(pTopic).sendMessage(pMessage);
            }
        }

        /**
         */
        public static class WebSocketSender {
            private String connectionId = "";
            private String topic = "";

            private WebSocketSender() {
            }

            private WebSocketSender(String pConnectionId, String pTopic) {
                this();
                connectionId = pConnectionId;
                topic = pTopic;
            }

            @SuppressWarnings("unused")
            public void send(byte[] pMessage) {
                getWebSocketBroker().sendMessageFor(connectionId, topic, pMessage);
            }
        }
    }

    /**
     */
    protected static class WebSocketConnectionRejectedException extends Exception {
        private static final long serialVersionUID = 1L;

        WebSocketConnectionRejectedException(String pMsg) {
            super(pMsg);
        }
    }

    /**
     */
    protected static class WebSocketConnectionHandler implements WsoMessageSender {

        protected String connectionId = "";
        protected String topic = "/";
        protected boolean isCORSEnabled = false;
        protected OutputStream outStream;
        protected WsoAccessController accessCtrl;

        protected WebSocketConnectionHandler() {
        }

        public WebSocketConnectionHandler(String pTopic, boolean pCORSEnabled, WsoAccessController pCtrl) {
            this();
            topic = pTopic;
            isCORSEnabled = pCORSEnabled;
            accessCtrl = pCtrl;
        }

        /**
         */
        @Override
        public String geConnectiontId() {
            return connectionId;
        }

        /**
         */
        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public void sendMessage(byte[] pMessage) {
            if (outStream != null) {
                try {
                    byte[] encodedBytes = encodeMessage(pMessage);

                    outStream.write(encodedBytes);
                    outStream.flush();
                } catch (IOException e) {
                    LOG.severe(() -> String.format("WebSocket send message error: %s %s %s", e.toString(), LS,
                            getStackTraceFrom(e)));
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

            try {
                // check accessibility
                if (!accessCtrl.isSupportedTopic(topic, lErrorMsg)
                        || !accessCtrl.isAccessGranted(pRequestHeader.getAttributes(), lErrorMsg)) {
                    lHandShakeHeader.writeTo(outStream);
                    throw new WebSocketConnectionRejectedException(
                            String.format("WebSocket connection rejected [%s]", lErrorMsg));
                }
                try {
                    // accept handshake
                    lHandShakeHeader.initWith(HTTP_DEFAULT_WEBSOCKET_RESPONSE_ATTRIBUTES).setHttpVersion(HTTP_1_1)
                            .setHttpStatus(SC_101_SWITCH_PROTOCOLS)
                            .set(SEC_WEBSOCKET_ACCEPT, createWebSocketAcceptKey(pRequestHeader.getWebSocketKey()));

                    lHandShakeHeader.writeTo(outStream);

                    // create a unique connectionId
                    connectionId = topic + " - " + Integer.toHexString(pSocket.hashCode()) + "-" + pSocket.toString();
                    // register this connection at the WebSocketBroker
                    getWebSocketBroker().connectionEstablished(connectionId, this);

                    LOG.info(() -> String.format("WebSocket connection established [%s]%s%s", connectionId, LS,
                            lHandShakeHeader));

                } catch (Exception e) {
                    lHandShakeHeader.setHttpStatus(SC_500_INTERNAL_ERROR).writeTo(outStream);
                    throw e;
                }

                // from here the io is websocket specific
                // and no longer like http

                // the processing blocks reading the lInStream until connection is closed
                // every read is considered as a "message"
                // and is forwarded to the WebSocketMessageBroker
                // that tries to find a consumer for it
                pSocket.setSoTimeout(0);
                readIncomingMessages(lInStream, outStream, getWebSocketBroker(), connectionId, topic);

                // returning from reading lInStream
                // means the stream returned -1, end of stream and closed
                outStream.flush();
                outStream.close();

            } catch (Exception e) {
                LOG.severe(() -> String.format("WebSocket request handling error: %s %s %s", e.toString(), LS,
                        getStackTraceFrom(e)));
            } finally {
                // remove connection from the broker
                getWebSocketBroker().connectionClosed(connectionId, this);
                try {
                    pSocket.close();
                } catch (IOException e) {
                    // hmm
                }
            }
        }

        /**
         * @throws UnsupportedEncodingException
         * @throws NoSuchAlgorithmException
         */
        protected String createWebSocketAcceptKey(String pRequestKey)
                throws NoSuchAlgorithmException, UnsupportedEncodingException {
            String lKey = pRequestKey + HttpHeader.MAGIC_WEBSOCKET_GUID;
            byte[] lSha1 = MessageDigest.getInstance("SHA-1").digest(lKey.getBytes("UTF-8"));
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
                WebSocketMessageBroker pBroker, String pConnectionId, String pTopic) throws IOException {

            boolean connected = true;
            int readPacketLength = 0;
            byte[] packet = new byte[1024];
            ByteArrayOutputStream packetStream = new ByteArrayOutputStream();
            byte[] message = new byte[0];

            while (connected) {
                readPacketLength = pInStream.read(packet);

                if (readPacketLength == -1) {
                    connected = false;
                    break;
                } else {
                    if ((packet[0] & (byte) 15) == (byte) 8) { // Disconnect packet
                        // returning the same but encoded packet for client to terminate connection
                        packet = encodeMessage(packet);
                        pOutStream.write(packet, 0, readPacketLength);
                        pOutStream.flush();
                        connected = false;
                        break;
                    }
                    byte messageLengthByte = 0;
                    int messageLength = 0;
                    int maskIndex = 2;
                    int messageStart = 0;
                    // b[0] is always text in my case so no need to check;
                    byte data = packet[1];
                    byte op = (byte) 127; // 0111 111
                    messageLengthByte = (byte) (data & op);

                    int totalPacketLength = 0;
                    if (messageLengthByte == (byte) 126 || messageLengthByte == (byte) 127) {
                        if (messageLengthByte == (byte) 126) {
                            maskIndex = 4;
                            // if (messageLengthInt==(byte)126), then 16-bit length is stored in packet[2]
                            // and [3]
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
                            // if (messageLengthInt==(byte)127), then 64-bit length is stored in bytes [2]
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
                    // forward message to the message broker for delivery
                    pBroker.publishMessageFor(pConnectionId, pTopic, message);
                }
            }
        }

        /**
         * taken from
         * https://stackoverflow.com/questions/43163592/standalone-websocket-server-without-jee-application-server
         */
        protected byte[] encodeMessage(byte[] rawData) throws IOException {

            int frameCount = 0;
            byte[] frame = new byte[10];

            frame[0] = (byte) 129;

            if (rawData.length <= 125) {
                frame[1] = (byte) rawData.length;
                frameCount = 2;
            } else if (rawData.length >= 126 && rawData.length <= 65535) {
                frame[1] = (byte) 126;
                int len = rawData.length;
                frame[2] = (byte) ((len >> 8) & (byte) 255);
                frame[3] = (byte) (len & (byte) 255);
                frameCount = 4;
            } else {
                frame[1] = (byte) 127;
                // org - int len = rawData.length;
                long len = rawData.length; // note an int is not big enough in java
                frame[2] = (byte) ((len >> 56) & (byte) 255);
                frame[3] = (byte) ((len >> 48) & (byte) 255);
                frame[4] = (byte) ((len >> 40) & (byte) 255);
                frame[5] = (byte) ((len >> 32) & (byte) 255);
                frame[6] = (byte) ((len >> 24) & (byte) 255);
                frame[7] = (byte) ((len >> 16) & (byte) 255);
                frame[8] = (byte) ((len >> 8) & (byte) 255);
                frame[9] = (byte) (len & (byte) 255);
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
     * The WebSocketMessageConsumer defines a message listener for a topic.
     * 
     * Once registered at the MessageBroker this consumer receives all messages
     * with the declared topic - from any client - identified by the pConnectionId.
     * </pre>
     */
    public static interface WsoMessageConsumer {

        /**
         */
        public default String getTopic() {
            return "/";
        }

        /**
         */
        public void onMessage(String pConnectionId, byte[] pMessage);

    }

    /**
     * <pre>
     * </pre>
     */
    public static interface WsoMessageSender {

        /**
         */
        public String geConnectiontId();

        /**
         */
        public default String getTopic() {
            return "/";
        }

        /**
         */
        public void sendMessage(byte[] pMessage);

    }

    /**
     * <pre>
     * </pre>
     */
    public static interface WsoAccessController {
        /**
         */
        public boolean isSupportedTopic(String pTopic, StringBuilder pMsg);

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
