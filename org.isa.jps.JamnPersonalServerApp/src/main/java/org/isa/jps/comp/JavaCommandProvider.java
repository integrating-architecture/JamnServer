package org.isa.jps.comp;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer;
import org.isa.jps.JamnPersonalServerApp;
import org.isa.jps.JamnPersonalServerApp.CommonHelper;
import org.isa.jps.JamnPersonalServerApp.Config;

/**
 * <pre>
 * </pre>
 */
public class JavaCommandProvider {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(JavaCommandProvider.class.getName());
    protected static final CommonHelper Tool = JamnPersonalServerApp.Tool;

    protected Charset encoding;
    protected JamnServer.JsonToolWrapper json;
    protected Config config;
    protected Path pathBase;
    protected Map<String, JCmdCartridge> commands;

    protected JavaCommandProvider() {
    }

    public JavaCommandProvider(Path pPathBase, Config pConfig, JamnServer.JsonToolWrapper pJsonTool) {
        this();
        json = pJsonTool;
        pathBase = pPathBase;
        config = pConfig;
        encoding = Charset.forName(config.getStandardEncoding());
    }

    /**
     */
    public void initialize() {
        commands = new HashMap<>();
    }

    /**
     */
    public String run(JCCallContext pCtx, String pName, String[] pArgs) {
        String lResult = "";
        JCmdCartridge lCmd = getCmd(pName);

        try {
            lResult = executeCommand(lCmd, pArgs, pCtx);
        } catch (UncheckedJavaCommandException e) {
            throw e;
        } catch (Exception e) {
            throw new UncheckedJavaCommandException(
                    String.format("Java Command execution failed [%s]", pName), e);
        }
        return lResult != null ? lResult : "";
    }

    /**
     */
    protected JCmdCartridge getCmd(String pName) {
        if (!commands.containsKey(pName)) {
            JCmdDef lDef = getCmdDef(pName);
            JCmdCartridge lCat = new JCmdCartridge(pName, lDef);
            initCmd(lCat);
            commands.put(pName, lCat);
        }
        return commands.get(pName);
    }

    /**
     */
    protected void initCmd(JCmdCartridge pCmd) {
        Path lFile = null;
        Path lLibsDir = null;
        URLClassLoader lCmdLoader = null;
        List<URL> lUrls = new ArrayList<>();
        List<String> lErrors = new ArrayList<>();

        try {
            // the command itself
            lFile = Paths.get(pCmd.def.getBinPath());
            Tool.createFileURL(lFile, lUrls, pCmd.def, lErrors);

            // the command libraries
            if (!pCmd.def.libsPath.trim().isEmpty()) {
                lLibsDir = Paths.get(pCmd.def.libsPath.trim());
            }

            for (String lib : pCmd.def.getLibs()) {
                lFile = lLibsDir != null ? Paths.get(lLibsDir.toString(), lib) : Paths.get(lib.trim());
                Tool.createFileURL(lFile, lUrls, pCmd.def, lErrors);
            }

            if (!lErrors.isEmpty()) {
                throw new UncheckedJavaCommandException(
                        String.format("Java Command binary init failed: %s%s", LS, String.join(LS, lErrors)));
            } else {
                lCmdLoader = new URLClassLoader(lUrls.toArray(new URL[lUrls.size()]),
                        ClassLoader.getPlatformClassLoader());

                pCmd.clazz = lCmdLoader.loadClass(pCmd.name);
                pCmd.constructor = pCmd.clazz.getConstructor(Map.class);
                pCmd.runMethod = pCmd.clazz.getMethod(pCmd.def.getRunMethodName(), String[].class);
            }

        } catch (UncheckedJavaCommandException e) {
            throw e;
        } catch (Exception e) {
            throw new UncheckedJavaCommandException(
                    String.format("Java Command Init failed [%s]", pCmd.def), e);
        }
    }

    /**
      */
    protected String executeCommand(JCmdCartridge pCmd, String[] pArgs, JCCallContext pCtx)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        Map<String, Object> callContextData = new HashMap<>();
        callContextData.put("output", pCtx.getOutputConsumer());
        Object lInstance = pCmd.constructor.newInstance(callContextData);
        return (String) pCmd.runMethod.invoke(lInstance, (Object) pArgs);
    }

    /**
     */
    protected JCmdDef getCmdDef(String pName) {
        String lJson;
        Path lDefFile = null;
        Path lBinPath = null;
        JCmdDef lDef = null;

        try {
            lDefFile = Paths.get(pathBase.toString(), pName + ".json");
            lBinPath = Paths.get(pathBase.toString(), pName + ".jar");
            if (Files.exists(lDefFile)) {
                lJson = new String(Files.readAllBytes(lDefFile), encoding);
                lDef = json.toObject(lJson, JCmdDef.class);
            } else {
                lDef = new JCmdDef();
                lDef.setTitle(pName);
                lDef.setBinPath(lBinPath.toString());
            }

            if (!Files.exists(Paths.get(lDef.binPath))) {
                throw new UncheckedJavaCommandException(
                        String.format("Could not find java command file [%s]", lDef.binPath));
            }

        } catch (IOException e) {
            throw new UncheckedJavaCommandException(
                    String.format("Could not load java command definition-file [%s]", lDefFile), e);
        }
        return lDef;
    }

    /*******************************************************************************
    *******************************************************************************/
    /**
     */
    protected static class JCmdCartridge {
        protected String name;
        protected JCmdDef def;
        protected Class<?> clazz;
        protected Constructor<?> constructor = null;
        protected Method runMethod = null;

        protected JCmdCartridge(String pName, JCmdDef pDef) {
            name = pName;
            def = pDef;
        }
    }

    /**
     */
    public static class JCmdDef {
        protected String title = "";
        protected String binPath = "";

        protected String libsPath = "";
        protected List<String> libs = new ArrayList<>();
        protected String version = "";
        protected String runMethodName = "execute";

        public String getRunMethodName() {
            return runMethodName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBinPath() {
            return binPath;
        }

        public void setBinPath(String binPath) {
            this.binPath = binPath;
        }

        public List<String> getLibs() {
            return libs;
        }

        public String getVersion() {
            return version;
        }
    }

    /**
     */
    public static class JCCallContext {

        // the output consumer is used to forward java shell process output
        private Consumer<String> outputConsumer = null;
        private String result = null;

        public JCCallContext() {
        }

        public JCCallContext(Consumer<String> outputConsumer) {
            this.outputConsumer = outputConsumer;
        }

        public Consumer<String> getOutputConsumer() {
            return outputConsumer;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    /**
     */
    public static class UncheckedJavaCommandException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedJavaCommandException(String pMsg) {
            super(pMsg);
        }

        public UncheckedJavaCommandException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }
}
