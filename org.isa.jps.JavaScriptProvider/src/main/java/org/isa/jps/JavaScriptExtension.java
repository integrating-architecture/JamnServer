/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * 
 */
public class JavaScriptExtension {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(JavaScriptExtension.class.getName());

    protected static final String JSID = "js";

    protected Config config = new Config();
    protected Path sourcePathBase;
    protected Context instanceContext;

    /**
     */
    public JavaScriptExtension(Path pSourcePathBase, Properties pConfigProps) {
        sourcePathBase = pSourcePathBase;
        config.getProperties().putAll(pConfigProps);
    }

    /**
     */
    public void initialize() {
        if (instanceContext != null) {
            instanceContext.close(true);
        }
        instanceContext = Context.newBuilder(JSID)
                .allowIO(true)
                .currentWorkingDirectory(sourcePathBase.toAbsolutePath())
                .option("engine.WarnInterpreterOnly", config.getWarnInterpreterOnly())
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build();
    }

    /**
     */
    public Object eval(String pFileName) {
        Source lSrc = getSourceFile(pFileName);
        Value lVal = instanceContext.eval(lSrc);
        return lVal.as(Object.class);
    }

    /**
     */
    protected Source getSourceFile(String pName) {
        Path lSrcPath = Paths.get(sourcePathBase.toString(), pName);
        try {
            return Source.newBuilder(JSID, lSrcPath.toFile()).build();
        } catch (IOException e) {
            throw new JavaScriptExtensionRTException(
                    String.format("Could not load source file [%s]", lSrcPath), e);
        }
    }

    /*********************************************************
     * Extension classes
     *********************************************************/
    /**
     */
    public static class Config {
        protected static final String DEFAULT_CONFIG = String.join(LS,
                "#" + JavaScriptExtension.class.getSimpleName() + " Config Properties", "",
                "#JS Engine option", "engine.WarnInterpreterOnly=false", "");

        protected Properties props = new Properties();

        private Config() {
        }

        private Config(InputStream pPropsIn) throws IOException {
            props.load(pPropsIn);
        }

        public String getWarnInterpreterOnly() {
            return props.getProperty("engine.WarnInterpreterOnly", "false");
        }

        public Properties getProperties() {
            return props;
        }
    }

    /**
     */
    public static class JavaScriptExtensionRTException extends RuntimeException {
        public JavaScriptExtensionRTException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }

}
