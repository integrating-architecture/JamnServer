/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * <pre>
 * A simple Graal-JS based JavaScript Engine provider.
 * 
 * some graal js optional vm args - also see config
 * -Dpolyglot.engine.WarnInterpreterOnly=false
 * -Dpolyglot.inspect=localhost:9229 
 * -Dpolyglot.inspect.Secure=false
 * -Dpolyglot.inspect.Suspend=false
 * 
 * </pre>
 */
public class JavaScriptProvider {

    public static final String JSHostAppId = "HostApp";

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(JavaScriptProvider.class.getName());

    protected static final String JSID = "js";

    protected Config config = new Config();
    protected Path sourcePathBase;

    protected JavaScriptHostApp hostApp;

    /**
     */
    public JavaScriptProvider(Path pSourcePathBase, Properties pConfigProps) {
        sourcePathBase = pSourcePathBase;
        config.getProperties().putAll(pConfigProps);
    }

    /**
     */
    public JavaScriptProvider setHostApp(JavaScriptHostApp pHostApp) {
        hostApp = pHostApp;
        return this;
    }

    /**
     */
    public boolean sourceExists(String pSource) {
        return Files.exists(getSourcePath(pSource));
    }

    /**
     */
    public Path getSourcePath(String pSource) {
        return Paths.get(sourcePathBase.toString(), pSource);
    }

    /**
     */
    public void initialize() {
        if (!System.getProperties().containsKey(Config.GRAAL_WARN_INTERPRETER)) {
            System.setProperty(Config.GRAAL_WARN_INTERPRETER, config.getWarnInterpreterOnly());
        }
        if (config.isJavaScriptDebugEnabled()) {
            if (!System.getProperties().containsKey(Config.GRAAL_INSPECT)) {
                System.setProperty(Config.GRAAL_INSPECT, config.getInspectAddress());
            }
            if (!System.getProperties().containsKey(Config.GRAAL_INSPECT_SUSPEND)) {
                System.setProperty(Config.GRAAL_INSPECT_SUSPEND, config.getInspectSuspend());
            }
        }
        hostApp.initialize();
    }

    /**
     */
    protected Context newContext() {
        Context lCtx = Context.newBuilder(JSID)
                .allowIO(true)
                .currentWorkingDirectory(sourcePathBase.toAbsolutePath())
                .option("engine.WarnInterpreterOnly", config.getWarnInterpreterOnly())
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build();

        lCtx.getBindings(JSID).putMember(JSHostAppId, hostApp);
        return lCtx;
    }

    /**
     * <pre>
     * The basic js script execution interface.
     * The implementation creates a new js context for each call
     * which is automatically closed after retrieving a return value;
     * </pre>
     */
    public JsValue eval(String pFileName, String... pArgsMember) {
        Source lSrc;
        Value lBindings;
        Value lValue;
        JsValue lRet = JsValue.DefaultNullValue;

        try (Context lCtx = newContext()) {
            lSrc = getSourceFile(pFileName);
            lBindings = lCtx.getBindings(JSID);
            lBindings.putMember("args", pArgsMember);
            lValue = lCtx.eval(lSrc);
            lRet = new JsValue(lValue);
        }
        return lRet;
    }

    /**
     */
    protected Source getSourceFile(String pName) {
        Path lSrcPath = Paths.get(sourcePathBase.toString(), pName);
        try {
            return Source.newBuilder(JSID, lSrcPath.toFile()).build();
        } catch (IOException e) {
            throw new UncheckedJavaScriptException(
                    String.format("Could not load source file [%s]", lSrcPath), e);
        }
    }

    /*********************************************************
     * Public Provider classes
     *********************************************************/

    /**
     * <pre>
     * Experimental startup common javascript return value
     * to restrict the basic data io between the app and the js engine to string.
     * 
     * That keeps app code clean from js engine code.
     * Other use cases must be explicitly designed and implemented.
     * </pre>
     */
    public static class JsValue {
        protected static final JsValue DefaultNullValue = new JsValue();

        protected Value engineValue;
        protected String stringValue = null;

        protected JsValue() {
        }

        public JsValue(Value pValue) {
            engineValue = pValue;
            resolve();
        }

        /**
         * Expects that js execution return a string value or an executable that returns
         * a string value.
         */
        protected void resolve() {
            if (engineValue.canExecute()) {
                stringValue = engineValue.execute().asString();
            } else {
                stringValue = engineValue.asString();
            }
        }

        /**
         * Indicates that the js execution did not return a expected value.
         */
        public boolean isNull() {
            return stringValue == null;
        }

        public Value origin() {
            return engineValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public String asString() {
            return stringValue;
        }
    }

    /**
     * <pre>
     * A rudimentary "marker" interface for JavaScript Host App implementations.
     * Just to provide a Java Type instead of raw Object.
     * </pre>
     */
    public static interface JavaScriptHostApp {
        public default String name() {
            return "unknown";
        }

        public default void initialize() {
        }
    }

    /**
     */
    public static class Config {
        public static final String GRAAL_WARN_INTERPRETER = "polyglot.engine.WarnInterpreterOnly";
        public static final String GRAAL_INSPECT = "polyglot.inspect";
        public static final String GRAAL_INSPECT_SECURE = "polyglot.inspect.Secure";
        public static final String GRAAL_INSPECT_SUSPEND = "polyglot.inspect.Suspend";

        public static final String JS_DEBUG = "javascript.debug.enabled";

        protected static final String GRAAL_JSEOPTION = "#GraalJS Engine option";
        protected static final String DEFAULT_CONFIG = String.join(LS,
                "#" + JavaScriptProvider.class.getSimpleName() + " Config Properties", "",
                GRAAL_JSEOPTION, GRAAL_WARN_INTERPRETER + "=false", "",
                GRAAL_JSEOPTION, GRAAL_INSPECT + "=localhost:9229", "",
                GRAAL_JSEOPTION, GRAAL_INSPECT_SECURE + "=false", "",
                GRAAL_JSEOPTION, GRAAL_INSPECT_SUSPEND + "=false", "",
                "#JavaScript debug enabled", JS_DEBUG + "=false", "");

        protected Properties props = new Properties();

        private Config() {
        }

        private Config(InputStream pPropsIn) throws IOException {
            props.load(pPropsIn);
        }

        public boolean isJavaScriptDebugEnabled() {
            return Boolean.parseBoolean(props.getProperty(JS_DEBUG, "false"));
        }

        public String getWarnInterpreterOnly() {
            return props.getProperty("polyglot.engine.WarnInterpreterOnly", "false");
        }

        public String getInspectAddress() {
            return props.getProperty("polyglot.inspect", "localhost:9229");
        }

        public String getInspectSecure() {
            return props.getProperty("polyglot.inspect.Secure", "false");
        }

        public String getInspectSuspend() {
            return props.getProperty("polyglot.inspect.Suspend", "false");
        }

        public Properties getProperties() {
            return props;
        }
    }

    /**
     */
    public static class UncheckedJavaScriptException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedJavaScriptException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }

}
