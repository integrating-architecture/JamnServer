/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnServer.HttpHeader.Field;
import org.isa.ipc.JamnWebContentProvider;
import org.isa.ipc.JamnWebContentProvider.DefaultFileEnricher;
import org.isa.ipc.JamnWebServiceProvider;
import org.isa.ipc.JamnWebServiceProvider.WebServiceDefinitionException;
import org.isa.jps.comp.CommandLineInterface;
import org.isa.jps.comp.DefaultCLICommands;
import org.isa.jps.comp.DefaultWebServices;
import org.isa.jps.comp.JavaScriptCLICommands;
import org.isa.jps.comp.SystemInterface;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * The JamnPersonalServerApp is an example of how to
 * assemble a jamn server with providers 
 * and define an environment of folders and configurations
 * that can serve as a startup/example for individual use.
 * 
 * The goal is an individual, open tool with the ability
 * to create local, network and browser enabled functions and services
 * in java and javascript all in one place.
 * </pre>
 */
public class JamnPersonalServerApp {

    /**
     * internal constants
     */
    protected static final String AppName = "Jamn Personal Server";

    protected static final String LS = System.lineSeparator();

    // internal ids
    protected static final String CONTENT_PROVIDER_ID = "ContentProvider";
    protected static final String SERVICE_PROVIDER_ID = "ServiceProvider";
    protected static final String WEBSOCKET_PROVIDER_ID = "WebSocketProvider";

    // folders under a user defined home directory
    protected static final String APP_LIBS_DIR = "libs";
    protected static final String WEB_FILE_ROOT = "http";
    protected static final String SCRIPT_ROOT = "scripts";
    protected static final String EXTENSION_ROOT = "extensions";
    // files
    protected static final String PROPERTIES_NAME = "jps.properties";
    protected static final String LOGGING_PROPERTIES_NAME = "jps.logging.properties";
    protected static final String EXTENSION_DEF_FILE = "app-extension-defs.json";

    // just a logging text snippet
    protected static final String INIT_LOGPRFX = "JPS init -";

    /**
     * internal static variables
     */
    protected static Charset StandardEncoding = StandardCharsets.UTF_8;
    protected static Logger LOG = Logger.getLogger(JamnPersonalServerApp.class.getName());
    protected static Path AppHome = Paths.get(System.getProperty("user.dir"));

    // a helper tool for common functions
    public static final CommonHelper Tool = new CommonHelper();

    /**
     * a simple singleton construct for this app
     */
    protected static JamnPersonalServerApp Instance;

    public static synchronized JamnPersonalServerApp getInstance() {
        if (Instance == null) {
            Instance = new JamnPersonalServerApp();
        }
        return Instance;
    }

    private JamnPersonalServerApp() {
    }

    /**
     * member variables
     */
    // JamnServer
    protected Config config;
    protected JamnServer server;
    protected JamnServer.JsonToolWrapper jsonTool;
    protected JamnWebServiceProvider webServiceProvider;

    // JavaScript extension
    protected JavaScriptExtension javaScript;

    // internal components
    protected SystemInterface sysIFace;
    protected CommandLineInterface jpsCli;

    // a content dispatcher predicate
    // to distinguish content and services
    protected Predicate<String> isServiceRequest = path -> false;

    /*********************************************************
     * Public methods
     *********************************************************/
    /**
     */
    public static void main(String[] args) {
        JamnPersonalServerApp.getInstance().start(args);
    }

    /**
     * @throws IOException
     * @throws SecurityException
     * @throws WebServiceDefinitionException
     * @throws AppExtensionDefinitionException
     */
    public JamnPersonalServerApp initialize(String[] pArgs)
            throws SecurityException, IOException, WebServiceDefinitionException, AppExtensionDefinitionException {
        LOG.info(() -> String.format("%s%s home [%s] args [%s]", LS, INIT_LOGPRFX,
                AppHome,
                String.join(" ", pArgs)));

        initLogging();
        initConfig();
        initProgramArgs(pArgs);
        sysIFace = new SystemInterface(config);

        initJsonTool();

        if (config.isServerEnabled()) {
            initServer();
            initContentProvider();
            initWebServiceProvider();
            initWebSocketProvider();
            initContentDispatcher();
        }

        initCli();
        initJavaScript();

        initExtensions();

        return this;
    }

    /**
     */
    public JamnPersonalServerApp start(String[] pArgs) {
        try {
            initialize(pArgs);
            if (config.isServerAutostart()) {
                server.start();
            }
            if (config.isCliEnabled()) {
                jpsCli.start();
            }
        } catch (Exception e) {
            stop();
            LOG.severe(() -> String.format("%sERROR starting %s: %s%s%s", LS, AppName, e, LS,
                    Tool.getStackTraceFrom(e)));
        }
        return this;
    }

    /**
     */
    public void stop() {
        if (server != null) {
            server.stop();
        }
        if (jpsCli != null) {
            jpsCli.stop();
        }
    }

    /**
     */
    public boolean isRunning() {
        return server.isRunning();
    }

    /**
     */
    public Config getConfig() {
        return config;
    }

    /**
     */
    public JamnServer.JsonToolWrapper getJsonTool() {
        return jsonTool;
    }

    /**
     */
    public JamnServer getServer() {
        return server;
    }

    /**
     */
    public SystemInterface getSysIFace() {
        return sysIFace;
    }

    /**
     */
    public CommandLineInterface getCli() {
        return jpsCli;
    }

    /**
     */
    public JavaScriptExtension getJavaScript() {
        return javaScript;
    }

    /**
     * Public-Interface to register extension WebServices
     * 
     * @throws WebServiceDefinitionException
     */
    public void registerWebServices(Object pServices) throws WebServiceDefinitionException {
        webServiceProvider.registerServices(pServices);
    }

    /**
     */
    public Path getHomePath(String... pSubPathParts) {
        return Paths.get(AppHome.toString(), pSubPathParts);
    }

    /*********************************************************
     * Internal methods
     *********************************************************/

    /**
     */
    protected void initProgramArgs(String[] pArgs) {
        Map<String, String> lArgs = Tool.defaultArgParser.apply(pArgs);
        getConfig().getProperties().putAll(lArgs);
    }

    /**
     */
    protected void initLogging() throws SecurityException, IOException {
        Path lConfigPath = getHomePath(LOGGING_PROPERTIES_NAME);

        if (Files.exists(lConfigPath)) {
            LogManager.getLogManager().readConfiguration(Files.newInputStream(lConfigPath));
            LOG.info(() -> String.format("%s user logging config read from [%s]", INIT_LOGPRFX, lConfigPath));

        } else {
            LogManager.getLogManager().readConfiguration(getClass().getResourceAsStream("/" + LOGGING_PROPERTIES_NAME));
            LOG.info(() -> String.format("%s default logging config read [%s]", INIT_LOGPRFX, LOGGING_PROPERTIES_NAME));
        }
    }

    /**
     */
    protected void initConfig() throws IOException {
        Path lConfigPath = getHomePath(PROPERTIES_NAME);

        // load config
        if (Files.exists(lConfigPath)) {
            config = new Config(Files.newInputStream(lConfigPath));
            LOG.info(() -> String.format("%s user app config file read [%s]", INIT_LOGPRFX, lConfigPath));
        } else {
            String lDefaultConfig = String.join(LS, Config.DEFAULT_CONFIG, JamnServer.Config.DEFAULT_CONFIG);
            config = new Config(Tool.getAsInputStream(lDefaultConfig));
            Files.writeString(lConfigPath, lDefaultConfig, StandardEncoding, StandardOpenOption.CREATE);
            LOG.info(() -> String.format("%s default app config loaded and saved to [%s]", INIT_LOGPRFX, lConfigPath));
        }
    }

    /**
     */
    protected void initCli() {
        jpsCli = new CommandLineInterface();
        if (config.isCliEnabled()) {
            DefaultCLICommands.create();
        } else {
            LOG.info(() -> String.format("%s CLI is Disabled. To enable set config [%s] property [cli.enabled=true]",
                    INIT_LOGPRFX, PROPERTIES_NAME));
        }
    }

    /**
     */
    protected void initJavaScript() throws IOException {
        if (config.isJavaScriptEnabled()) {

            Path lScriptPath = Tool.ensureSubDir(config.getScriptRoot(), AppHome);

            javaScript = new JavaScriptExtension(lScriptPath, config.getProperties());
            javaScript.initialize();

            if (jpsCli != null) {
                new JavaScriptCLICommands(javaScript).createCommandsFor(jpsCli);
            }
            LOG.info(() -> String.format("%s JavaScript Extension initialized [%s]", INIT_LOGPRFX, lScriptPath));
        } else {
            LOG.info(() -> String.format(
                    "%s JavaScript is Disabled. To enable ensure libraries and set config [%s] property [javascript.enabled=true]",
                    INIT_LOGPRFX, PROPERTIES_NAME));
        }
    }

    /**
     * @throws IOException
     * @throws AppExtensionDefinitionException
     */
    protected void initExtensions() throws IOException, AppExtensionDefinitionException {
        if (config.isExtensionsEnabled()) {
            // ensure the extensions root folder
            Path lRootPath = Tool.ensureSubDir(config.getExtensionRoot(), AppHome);

            Path lDefFile = Paths.get(lRootPath.toString(), config.getExtensionDefFileName());
            if (!Files.exists(lDefFile)) {
                String lDefJson = jsonTool.toString(new AppExtensionDef[] { new AppExtensionDef() });
                Files.writeString(lDefFile, lDefJson, StandardEncoding, StandardOpenOption.CREATE);

                LOG.info(() -> String.format("%s default EMPTY App Extension definition created [%s]%s%s", INIT_LOGPRFX,
                        lDefFile, LS,
                        "--> edit and ADD Extension definitions! <--"));
                return;
            }

            // init plugable extensions defined in definition file
            String lExtensionDef = new String(Files.readAllBytes(lDefFile));
            List<AppExtensionDef> lExtensions = Arrays
                    .asList(jsonTool.toObject(lExtensionDef, AppExtensionDef[].class));

            AtomicInteger lCount = new AtomicInteger(0);
            for (AppExtensionDef def : lExtensions) {
                if (!def.isEmpty()) {
                    createExtension(def, lRootPath, lCount);
                }
            }

            if (lCount.get() > 0) {
                LOG.info(() -> String.format("%s [%s] app extensions installed from: [%s]", INIT_LOGPRFX, lCount.get(),
                        lDefFile));
            }
        } else {
            LOG.info(() -> String.format(
                    "%s Extensions are Disabled. To enable ensure libraries and set config [%s] property [extensions.enabled=true]",
                    INIT_LOGPRFX, PROPERTIES_NAME));
        }
    }

    /**
     * @throws AppExtensionDefinitionException
     */
    @SuppressWarnings("resource")
    protected void createExtension(AppExtensionDef pDef, Path pRootPath, AtomicInteger... pCount)
            throws AppExtensionDefinitionException {
        URLClassLoader lExtensionLoader;
        Class<?> lExtensionClass;
        List<URL> lUrls = new ArrayList<>();
        Path lFilePath;
        List<String> lErrors = new ArrayList<>();

        try {
            // the extension itself
            lFilePath = Paths.get(pRootPath.toString(), pDef.getLibName());
            Tool.createFileURL(lFilePath, lUrls, pDef, lErrors);

            // the extension libraries
            for (String lib : pDef.getLibs()) {
                lFilePath = Paths.get(pRootPath.toString(), lib);
                Tool.createFileURL(lFilePath, lUrls, pDef, lErrors);
            }

            if (!lErrors.isEmpty()) {
                throw new AppExtensionDefinitionException(
                        String.format("Invalid Extension definition: %s%s", LS, String.join(LS, lErrors)));
            }

            lExtensionLoader = new URLClassLoader(lUrls.toArray(new URL[lUrls.size()]),
                    Thread.currentThread().getContextClassLoader());

            lExtensionClass = lExtensionLoader.loadClass(pDef.getClassName());
            lExtensionClass.getDeclaredConstructor().newInstance();

            LOG.info(() -> String.format("Extension installed [%s]", pDef));
            if (pCount.length == 1) {
                pCount[0].getAndIncrement();
            }

        } catch (AppExtensionDefinitionException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AppExtensionDefinitionException(String.format("Failed to install AppExtension [%s]", pDef), e);
        }
    }

    /**
     * json isGetter visibility disabled
     */
    protected void initJsonTool() {
        jsonTool = new JamnServer.JsonToolWrapper() {
            private final ObjectMapper jack = new ObjectMapper()
                    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
                    .setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);

            @Override
            public <T> T toObject(String pSrc, Class<T> pType) throws IOException {
                return jack.readValue(pSrc, pType);
            }

            @Override
            public String toString(Object pObj) throws IOException {
                return jack.writeValueAsString(pObj);
            }
        };
        LOG.info(() -> String.format("%s json tool installed [%s]", INIT_LOGPRFX, ObjectMapper.class.getName()));
    }

    /**
     */
    protected void initServer() {
        server = new JamnServer(config.getProperties());
    }

    /**
     */
    protected void initContentProvider() throws IOException {
        // ensure the web file root folder
        String lRootPath = Tool.ensureSubDir(config.getWebFileRoot(), AppHome).toString();

        // create the provider with a webroot
        // no leading slash because relative path
        JamnWebContentProvider lWebContentProvider = JamnWebContentProvider.Builder(lRootPath)
                .setConfig(server.getConfig())
                .setFileEnricher(new DefaultFileEnricher((String pKey, Object pCtx) -> "TODO")).build();
        // add it to server
        server.addContentProvider(CONTENT_PROVIDER_ID, lWebContentProvider);
        LOG.info(() -> String.format("%s content provider installed [%s] on [%s]", INIT_LOGPRFX,
                JamnWebContentProvider.class.getSimpleName(), lRootPath));

    }

    /**
     * @throws IOException
     * @throws WebServiceDefinitionException
     */
    protected void initWebServiceProvider() throws IOException, WebServiceDefinitionException {

        // create the WebService provider
        webServiceProvider = JamnWebServiceProvider.Builder().setJsonTool(jsonTool);

        // set a predicate to identify an webservice request
        // by default all paths registered at the webservice provider
        // are ServiceRequests - see initContentDispatcher
        isServiceRequest = webServiceProvider::isServicePath;
        server.addContentProvider(SERVICE_PROVIDER_ID, webServiceProvider);

        LOG.info(() -> String.format("%s web service provider installed [%s]", INIT_LOGPRFX,
                JamnWebServiceProvider.class.getSimpleName()));

        // init default app internal services
        DefaultWebServices lDefaultWS = new DefaultWebServices(sysIFace);
        webServiceProvider.registerServices(lDefaultWS);
    }

    /**
     */
    protected void initWebSocketProvider() {
        // TODO
    }

    /**
     */
    protected void initContentDispatcher() {
        server.setContentProviderDispatcher((Map<String, String> pRequestAttributes) -> {
            String lPath = pRequestAttributes.getOrDefault(Field.HTTP_PATH, "");
            if (isServiceRequest.test(lPath)) {
                return SERVICE_PROVIDER_ID;
            }
            return CONTENT_PROVIDER_ID;
        });
    }

    /*********************************************************
     * Static app helper methods
     *********************************************************/

    /*********************************************************
     * App classes
     *********************************************************/
    /**
     */
    public static class Config {
        protected static final String DEFAULT_CONFIG = String.join(LS,
                "##",
                "## " + AppName + " Config Properties",
                "##", "",
                "#JamnWebContentProvider files root folder", "web.file.root=" + WEB_FILE_ROOT, "",
                "#Jamn Personal Server extension libraries root folder", "jps.extension.root=" + EXTENSION_ROOT, "",
                "#JPS Extension definition file name", "jps.extension.def.file=" + EXTENSION_DEF_FILE, "",
                "#Extensions enabled", "extensions.enabled=true", "",
                "#JavaScriptExtension script root folder", "script.root=" + SCRIPT_ROOT, "",
                "#CLI enabled", "cli.enabled=true", "",
                "#JavaScript enabled", "javascript.enabled=false", "",
                "#Server enabled", "server.enabled=true", "",
                "#Server autostart", "server.autostart=true", "",
                "#Windows shell encoding", "win.shell.encoding=Cp850", "",
                "#Unix shell encoding", "unix.shell.encoding=ISO8859_1", "");

        protected Properties props = new Properties();

        private Config() {
        }

        private Config(InputStream pPropsIn) throws IOException {
            props.load(pPropsIn);
        }

        public String getWebFileRoot() {
            return props.getProperty("web.file.root", WEB_FILE_ROOT);
        }

        public String getExtensionRoot() {
            return props.getProperty("jps.extension.root", EXTENSION_ROOT);
        }

        public String getExtensionDefFileName() {
            return props.getProperty("jps.extension.def.file", EXTENSION_DEF_FILE);
        }

        public boolean isExtensionsEnabled() {
            return Boolean.parseBoolean(props.getProperty("extensions.enabled", "true"));
        }

        public String getScriptRoot() {
            return props.getProperty("script.root", SCRIPT_ROOT);
        }

        public int getPort() {
            return Integer.valueOf(props.getProperty("port", "8099"));
        }

        public String getWinShellEncoding() {
            return props.getProperty("win.shell.encoding", "Cp850");
        }

        public String getUnixShellEncoding() {
            return props.getProperty("unix.shell.encoding", "ISO8859_1");
        }

        public boolean isCliEnabled() {
            return Boolean.parseBoolean(props.getProperty("cli.enabled", "false"));
        }

        public boolean isJavaScriptEnabled() {
            return Boolean.parseBoolean(props.getProperty("javascript.enabled", "false"));
        }

        public boolean isServerEnabled() {
            return Boolean.parseBoolean(props.getProperty("server.enabled", "true"));
        }

        public boolean isServerAutostart() {
            return Boolean.parseBoolean(props.getProperty("server.autostart", "true"));
        }

        public Properties getProperties() {
            return props;
        }
    }

    /**
     */
    public static class AppExtensionDef {
        protected String libName = "";
        protected String className = "";
        protected List<String> libs = new ArrayList<>();

        public AppExtensionDef() {
        }

        public AppExtensionDef(String pLibName, String pClassName, List<String> pLibs) {
            this();
            libName = pLibName.trim();
            className = pClassName.trim();
            libs.addAll(pLibs);
        }

        public String getLibName() {
            return libName;
        }

        public String getClassName() {
            return className;
        }

        public List<String> getLibs() {
            return libs;
        }

        @Override
        public String toString() {
            return String.format("AppExtensionDef [%s : %s]", className, libName);
        }

        // json excluded
        public boolean isEmpty() {
            return (libName.isEmpty() || className.isEmpty());
        }

    }

    /**
     * Exceptions thrown during Extension initialization/creation.
     */
    public static class AppExtensionDefinitionException extends Exception {
        private static final long serialVersionUID = 1L;

        AppExtensionDefinitionException(String pMsg) {
            super(pMsg);
        }

        AppExtensionDefinitionException(String pMsg, Throwable t) {
            super(pMsg, t);
        }

    }

    /*********************************************************
     * Common Helper class
     *********************************************************/
    /**
     */
    public static class CommonHelper {

        /**
         */
        public final Function<String[], Map<String, String>> defaultArgParser = (String[] args) -> {
            Map<String, String> lArgMap = new LinkedHashMap<>();
            String lMark = "-";
            String lDelim = "=";
            boolean lStrict = false;

            String[] lKeyValue;
            for (String arg : args) {
                if (!lStrict || arg.startsWith(lMark)) {
                    lKeyValue = arg.split(lDelim);
                    if (lKeyValue.length > 0) {
                        if (lKeyValue[0].startsWith(lMark)) {
                            lKeyValue[0] = lKeyValue[0].substring(1);
                        }
                        if (lKeyValue.length > 1) {
                            lArgMap.put(lKeyValue[0], lKeyValue[1]);
                        } else {
                            lArgMap.put(lKeyValue[0], lKeyValue[0]);
                        }
                    }
                }
            }
            return lArgMap;
        };

        /**
         */
        public String getStackTraceFrom(Throwable t) {
            return JamnServer.getStackTraceFrom(t);
        }

        /**
         */
        public Path ensureSubDir(String pName, Path pRoot) throws IOException {
            Path lPath = Paths.get(pRoot.toString(), pName);
            if (!Files.exists(lPath)) {
                Files.createDirectories(lPath);
                LOG.info(() -> String.format("Directories created [%s]", lPath));
            }
            return lPath;
        }

        /**
         */
        public String readToString(InputStream lInStream) {
            return new BufferedReader(new InputStreamReader(lInStream)).lines().collect(Collectors.joining(LS));
        }

        /**
         */
        public InputStream getAsInputStream(String pText) {
            return new ByteArrayInputStream(pText.getBytes(StandardEncoding));
        }

        /**
         */
        public void createFileURL(Path pFile, List<URL> pUrls, Object pInfo, List<String> pErrors)
                throws MalformedURLException {

            if (Files.exists(pFile) && !Files.isDirectory(pFile)) {
                pUrls.add(new URL("file:" + pFile.toString()));
            } else {
                pErrors
                        .add(String.format("File does NOT exist [%s] [%s]", pFile, pInfo));
            }
        }

    }
}
