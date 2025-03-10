/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.JsonToolWrapper;
import org.isa.ipc.JamnWebSocketProvider;
import org.isa.ipc.JamnWebSocketProvider.WsoMessageProcessor;
import org.isa.jps.JamnPersonalServerApp;
import org.isa.jps.JamnPersonalServerApp.CommonHelper;
import org.isa.jps.JamnPersonalServerApp.Config;
import org.isa.jps.JavaScriptProvider;
import org.isa.jps.JavaScriptProvider.JSCallContext;

/**
 * <pre>
 * Example of a server side websocket message listener to execute commands.
 * 
 * A user counterpart on the web client side is 
 *  - command.mjs.
 * </pre>
 */
public class DefaultWebSocketMessageProcessor implements WsoMessageProcessor {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(DefaultWebSocketMessageProcessor.class.getName());
    protected static final CommonHelper Tool = new CommonHelper();

    protected static final String CMD_RUNJS = "runjs";
    protected static final String STATUS_SUCCESS = "success";
    protected static final String STATUS_ERROR = "error";

    protected Config config;
    protected JsonToolWrapper json;
    protected AtomicBoolean isAvailable;

    protected JavaScriptProvider jsProvider;
    protected JamnWebSocketProvider wsoProvider;

    protected Charset encoding = StandardCharsets.UTF_8;

    public DefaultWebSocketMessageProcessor(Config pConfig, JsonToolWrapper pJson, JamnWebSocketProvider pWsoProvider,
            Charset pEncoding) {
        config = pConfig;
        json = pJson;
        wsoProvider = pWsoProvider;
        isAvailable = new AtomicBoolean(true);

        if (pEncoding != null) {
            encoding = pEncoding;
        } else {
            encoding = Charset.forName(config.getStandardEncoding());
        }
        LOG.info(() -> String.format("WebSocket Processor encoding: [%s]", encoding));
    }

    /**
     * The WsoMessageProcessor Interface
     */
    @Override
    public byte[] onMessage(String pConnectionId, byte[] pMessage) {
        String lJsonResponse = "";
        String lJsonMsg = new String(pMessage);
        LOG.info(() -> String.format("WebSocket Message received: [%s] - [%s]", lJsonMsg, pConnectionId));

        WsoCommonMessage lResponseMsg = onMessage(pConnectionId, json.toObject(lJsonMsg, WsoCommonMessage.class));
        if (lResponseMsg != null) {
            lJsonResponse = json.toString(lResponseMsg);
        }

        return lJsonResponse.getBytes();
    }

    /**
     * The internal processing implementation
     */
    protected WsoCommonMessage onMessage(String pConnectionId, WsoCommonMessage pRequestMsg) {
        WsoCommonMessage lResponseMsg = new WsoCommonMessage(pRequestMsg.getReference());

        // limits message processing to - ONE message at ONE time
        if (isAvailable.compareAndSet(true, false)) {
            try {
                if (CMD_RUNJS.equalsIgnoreCase(pRequestMsg.getCommand())) {
                    runJSCommand(pConnectionId, pRequestMsg, lResponseMsg);
                    lResponseMsg.setStatus(STATUS_SUCCESS);
                }
            } catch (Exception e) {
                lResponseMsg.setStatus(STATUS_ERROR);
                lResponseMsg.setError(String.format("ERROR processing wso command request [%s] [%s]%s%s", pRequestMsg,
                        pConnectionId,
                        LS, Tool.getStackTraceFrom(e)));
                LOG.severe(lResponseMsg.getError());
            } finally {
                isAvailable.set(true);
            }
        }

        return lResponseMsg;
    }

    /**
     */
    protected void runJSCommand(String pConnectionId, WsoCommonMessage pRequestMsg, WsoCommonMessage lResponseMsg) {

        WsoCommonMessage lOutputMsg = new WsoCommonMessage(pRequestMsg.getReference());

        JSCallContext lCallCtx = new JSCallContext((String output) -> {
            lOutputMsg.setTextdata(output);
            String lJsonMsg = json.toString(lOutputMsg);
            wsoProvider.sendMessageTo(pConnectionId, lJsonMsg.getBytes(encoding));
        });
        js().eval(lCallCtx, pRequestMsg.getScript());
    }

    /**
     */
    protected JavaScriptProvider js() {
        if (jsProvider == null) {
            jsProvider = JamnPersonalServerApp.getInstance().getJavaScript();
            if (jsProvider == null) {
                throw new UncheckedWsoProcessorException("ERROR JavaScript NOT available");
            }
        }
        return jsProvider;
    }

    /**
     * A simple general message data structure for web socket communication.
     */
    public static class WsoCommonMessage {
        protected String reference = "";
        protected String textdata = "";
        protected String command = "";
        protected String script = "";
        protected String status = "";
        protected String error = "";
        protected Map<String, String> data = new LinkedHashMap<>();

        public WsoCommonMessage() {
        }

        public WsoCommonMessage(String reference) {
            this.reference = reference;
        }

        @Override
        public String toString() {
            return String.join(", ", reference, command, script);
        }

        public String getReference() {
            return reference;
        }

        public String getTextdata() {
            return textdata;
        }

        public String getCommand() {
            return command;
        }

        public String getScript() {
            return script;
        }

        public String getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public Map<String, String> getData() {
            return data;
        }

        public WsoCommonMessage setReference(String reference) {
            this.reference = reference;
            return this;
        }

        public WsoCommonMessage setTextdata(String textdata) {
            this.textdata = textdata;
            return this;
        }

        public WsoCommonMessage setCommand(String command) {
            this.command = command;
            return this;
        }

        public WsoCommonMessage setStatus(String status) {
            this.status = status;
            return this;
        }

        public WsoCommonMessage setError(String error) {
            this.error = error;
            return this;
        }

        public WsoCommonMessage addData(String pKey, String pVal) {
            this.data.put(pKey, pVal);
            return this;
        }
    }

    /**
     */
    public static class UncheckedWsoProcessorException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UncheckedWsoProcessorException(String pMsg) {
            super(pMsg);
        }

        UncheckedWsoProcessorException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }

}
