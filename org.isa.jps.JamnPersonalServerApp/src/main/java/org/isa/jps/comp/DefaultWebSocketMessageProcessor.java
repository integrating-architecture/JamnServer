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
import org.isa.jps.comp.ExtensionHandler.ExtensionCallContext;

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
    protected static final String CMD_RUNEXT = "runext";
    protected static final String STATUS_SUCCESS = "success";
    protected static final String STATUS_ERROR = "error";

    protected Config config;
    protected JsonToolWrapper json;
    protected AtomicBoolean isAvailable;

    protected JavaScriptProvider jsProvider;
    protected ExtensionHandler extensionHandler;
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
        String lJsonMsg = new String(pMessage, encoding);
        LOG.info(() -> String.format("WebSocket Message received: [%s] - [%s]", lJsonMsg, pConnectionId));

        WsoCommonMessage lResponseMsg = onMessage(pConnectionId, json.toObject(lJsonMsg, WsoCommonMessage.class));
        if (lResponseMsg != null) {
            lJsonResponse = json.toString(lResponseMsg);
        }

        return lJsonResponse.getBytes();
    }

    /**
     */
    @Override
    public byte[] onError(String pConnectionId, Exception pExp) {
        WsoCommonMessage lResponseMsg = new WsoCommonMessage("server")
            .setStatus(STATUS_ERROR)
            .setError(pExp.getMessage());

        String  lJsonResponse = json.toString(lResponseMsg);
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
                } else if (CMD_RUNEXT.equalsIgnoreCase(pRequestMsg.getCommand())) {
                    runExtCommand(pConnectionId, pRequestMsg, lResponseMsg);
                    lResponseMsg.setStatus(STATUS_SUCCESS);
                } else {
                    throw new UncheckedWsoProcessorException(
                            String.format("Unsupported command [%s]", pRequestMsg.getCommand()));
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
    protected String[] parseArgsFrom(String pArgsSrc) {
        return Tool.parseCommandLine(pArgsSrc, null);
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
        js().run(lCallCtx, pRequestMsg.getScript(), parseArgsFrom(pRequestMsg.getArgsSrc()));
        if (lCallCtx.getResult() != null && !lCallCtx.getResult().isEmpty() && lCallCtx.getOutputConsumer() != null) {
            String lResultPrint = Tool.formatCommandReturn(lCallCtx.getResult());
            lCallCtx.getOutputConsumer().accept(lResultPrint);
        }
    }

    /**
     */
    protected void runExtCommand(String pConnectionId, WsoCommonMessage pRequestMsg, WsoCommonMessage lResponseMsg) {

        WsoCommonMessage lOutputMsg = new WsoCommonMessage(pRequestMsg.getReference());

        ExtensionCallContext lCallCtx = new ExtensionCallContext((String output) -> {
            lOutputMsg.setTextdata(output);
            String lJsonMsg = json.toString(lOutputMsg);
            wsoProvider.sendMessageTo(pConnectionId, lJsonMsg.getBytes(encoding));
        });
        ext().run(lCallCtx, pRequestMsg.getScript(), parseArgsFrom(pRequestMsg.getArgsSrc()));
        if (lCallCtx.getResult() != null && !lCallCtx.getResult().isEmpty() && lCallCtx.getOutputConsumer() != null) {
            String lResultPrint = Tool.formatCommandReturn(lCallCtx.getResult());
            lCallCtx.getOutputConsumer().accept(lResultPrint);
        }
    }

    /**
     */
    protected ExtensionHandler ext() {
        if (extensionHandler == null) {
            extensionHandler = JamnPersonalServerApp.getInstance().getExtensionHandler();
            if (extensionHandler == null) {
                throw new UncheckedWsoProcessorException("ERROR Extension Handler NOT available");
            }
        }
        return extensionHandler;
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
        protected String argsSrc = "";
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

        public String getArgsSrc() {
            return argsSrc;
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
