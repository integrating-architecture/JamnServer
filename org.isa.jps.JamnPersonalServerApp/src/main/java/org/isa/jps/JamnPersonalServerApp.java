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
import java.util.function.UnaryOperator;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnServer.RequestMessage;
import org.isa.ipc.JamnServer.UncheckedJsonException;
import org.isa.ipc.JamnWebContentProvider;
import org.isa.ipc.JamnWebContentProvider.DefaultFileEnricher;
import org.isa.ipc.JamnWebServiceProvider;
import org.isa.ipc.JamnWebServiceProvider.WebServiceDefinitionException;
import org.isa.ipc.JamnWebSocketProvider;
import org.isa.ipc.JamnWebSocketProvider.WsoMessageProcessor;
import org.isa.jps.comp.ChildProcessManager;
import org.isa.jps.comp.ChildProcessor;
import org.isa.jps.comp.CommandLineInterface;
import org.isa.jps.comp.DefaultCLICommands;
import org.isa.jps.comp.DefaultFileEnricherValueProvider;
import org.isa.jps.comp.DefaultJavaScriptHostAppAdapter;
import org.isa.jps.comp.DefaultServerAccessManager;
import org.isa.jps.comp.DefaultWebServices;
import org.isa.jps.comp.DefaultWebSocketMessageProcessor;
import org.isa.jps.comp.OperatingSystemInterface;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    public static final String AppName = "Jamn Personal Server";

    // a public helper tool for common functions
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

    protected static synchronized void closeInstance() {
        Instance = null;
    }

    /**
     * internal constants
     */
    protected static final String LS = System.lineSeparator();
    protected static final String LF = "\n";

    // internal ids
    protected static final String CONTENT_PROVIDER_ID = "ContentProvider";
    protected static final String SERVICE_PROVIDER_ID = "ServiceProvider";
    protected static final String WEBSOCKET_PROVIDER_ID = "WebSocketProvider";

    // folders under a user defined home directory
    protected static final String APP_LIBS_DIR = "libs";
    protected static final String WEB_FILE_ROOT = "http";
    protected static final String SCRIPT_ROOT = "scripts";
    protected static final String EXTENSION_ROOT = "extensions";
    protected static final String WORKSPACE_ROOT = "workspace";

    // files
    protected static final String PROPERTIES_NAME = "jps.properties";
    protected static final String LOGGING_PROPERTIES_NAME = "jps.logging.properties";
    protected static final String BUILD_INFO_PROPERTIES_NAME = "build.info.properties";
    protected static final String EXTENSION_DEF_FILE = "app-extension-defs.json";
    protected static final String DEV_LIBS_PATH = "dev.libs.path";

    public static final String JPS_PROFILE = "jps.profile";
    public static final String APP_PROFILE = "app";
    public static final String CHILD_PROFILE = "child";
    public static final String JPS_CHILD_ID = "jps.child.id";
    public static final String JPS_PARENT_URL = "jps.parent.url";

    // just a logging text snippet
    protected static final String INIT_LOGPRFX = "JPS init -";

    /**
     * internal static variables
     */
    protected static Logger LOG = Logger.getLogger(JamnPersonalServerApp.class.getName());
    protected static Path AppHome = Paths.get(System.getProperty("user.dir"));

    protected Charset standardEncoding = StandardCharsets.UTF_8;

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
    protected JamnWebSocketProvider webSocketProvider;

    // JavaScript Provider
    protected JavaScriptProvider javaScript;

    // internal components
    protected OperatingSystemInterface osIFace;
    protected CommandLineInterface jpsCli;
    protected ChildProcessManager childManager;
    protected ChildProcessor childProcessor;

    // a content dispatcher predicate
    // to distinguish content and services
    // will later be forwarded to the webServiceProvider
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
        initConfig(pArgs);

        if (config.hasAppProfile()) {
            doAppInitialization();
        } else if (config.hasChildProfile()) {
            doChildInitialization();
        } else {
            throw new UncheckedJPSException(String.format(
                    "NO valid App Profile defined. Supported profiles are: [profile= %s || %s]", APP_PROFILE,
                    CHILD_PROFILE));
        }

        return this;
    }

    /**
     * Default JamnPersonalServer-App initialization routine
     */
    protected void doAppInitialization()
            throws IOException, WebServiceDefinitionException, AppExtensionDefinitionException {

        osIFace = new OperatingSystemInterface(config, null);

        initJsonTool();
        initCli();

        if (config.isServerEnabled()) {
            initServer();
            initContentProvider();
            initWebServiceProvider();
            initWebSocketProvider();
            initContentDispatcher();
        }

        initChildManagement();
        initJavaScript();

        initExtensions();

    }

    /**
     * <pre>
     * Initialization of a Child-App
     * started from a remote JamnPersonalServerApp ChildProcessManager.
     * 
     * The child connects to the parent using WebSocket.
     * </pre>
     * 
     */
    protected void doChildInitialization() {
        initJsonTool();

        childProcessor = new ChildProcessor(config, jsonTool);
        childProcessor.connect();
    }

    /**
     * Top level Start method - called from main.
     */
    public JamnPersonalServerApp start(String[] pArgs) {
        try {
            initialize(pArgs);

            // if standard top level App profile
            if (config.hasAppProfile()) {
                if (config.isServerAutostart() && server != null) {
                    server.start();
                }
                if (config.isCliEnabled() && jpsCli != null) {
                    jpsCli.start();
                }
            } else if (config.hasChildProfile() && childProcessor != null) {
                // if Child profile
                childProcessor.start();
            }
        } catch (Exception e) {
            close();
            throw new UncheckedJPSException(
                    String.format("%sERROR starting %s: [%s]%s", LS, AppName, e.getMessage(), LS), e);
        }
        return this;
    }

    /**
     */
    public synchronized void close() {
        if (server != null) {
            server.stop();
        }
        if (jpsCli != null) {
            jpsCli.stop();
        }
        // only present in child process
        if (childProcessor != null) {
            childProcessor.stop();
        }
        closeInstance();
    }

    /**
     */
    public boolean isRunning() {
        return (server != null && server.isRunning());
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
    public OperatingSystemInterface getOsIFace() {
        return osIFace;
    }

    /**
     */
    public ChildProcessManager getChildProcessManager() {
        return childManager;
    }

    /**
     */
    public CommandLineInterface getCli() {
        return jpsCli;
    }

    /**
     */
    public JavaScriptProvider getJavaScript() {
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
    public void addWebSocketMessageProcessor(WsoMessageProcessor pProcessor) {
        if (webSocketProvider != null) {
            webSocketProvider.addMessageProcessor(pProcessor);
        }
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
    protected void initConfig(String[] pArgs) throws IOException {
        boolean lSaveConfig = false;
        String lDefaultConfig = "";
        Map<String, String> lArgs = Tool.defaultArgParser.apply(pArgs);
        Path lConfigPath = getHomePath(PROPERTIES_NAME);

        // load config
        if (Files.exists(lConfigPath)) {
            config = new Config(Files.newInputStream(lConfigPath));
            LOG.info(() -> String.format("%s user app config file read [%s]", INIT_LOGPRFX, lConfigPath));
        } else {
            lDefaultConfig = String.join(LS, Config.DEFAULT_CONFIG, JamnServer.Config.DEFAULT_CONFIG);
            config = new Config(Tool.getAsInputStream(lDefaultConfig));
            lSaveConfig = true;
        }

        // load build infos
        InputStream lIn = getClass().getResourceAsStream("/" + BUILD_INFO_PROPERTIES_NAME);
        if (lIn != null) {
            config.getBuildProperties().load(lIn);
        }

        // search -D options
        config.searchDynamicOption(JPS_PROFILE, APP_PROFILE);
        config.searchDynamicOption(DEV_LIBS_PATH, "");

        // at last merge program args
        config.getProperties().putAll(lArgs);

        standardEncoding = Charset.forName(config.getStandardEncoding());
        CommonHelper.setEncoding(standardEncoding);

        if (lSaveConfig && config.hasAppProfile()) {
            Files.writeString(lConfigPath, lDefaultConfig, standardEncoding, StandardOpenOption.CREATE);
            LOG.info(() -> String.format("%s default app config loaded and saved to [%s]", INIT_LOGPRFX, lConfigPath));
        }
    }

    /**
     * @throws IOException
     */
    protected void initChildManagement() throws IOException {
        if (osIFace != null && webSocketProvider != null) {
            childManager = ChildProcessManager.newBuilder()
                    .setWebSocketProvider(webSocketProvider)
                    .setOperatingSystemInterface(osIFace)
                    .setJsonTool(jsonTool)
                    .setAppHome(AppHome)
                    .setConfig(config)
                    .build();
            childManager.initialize();
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

            javaScript = new JavaScriptProvider(lScriptPath, config.getProperties());
            javaScript.setHostAppAdapter(new DefaultJavaScriptHostAppAdapter(javaScript, this));

            javaScript.initialize();

            String lAutoLoadScript = getConfig().getJsAutoLoadScript();
            if (javaScript.sourceExists(lAutoLoadScript)) {
                javaScript.eval(lAutoLoadScript);
            }

            LOG.info(() -> String.format("%s JavaScript Provider initialized [%s]", INIT_LOGPRFX, lScriptPath));
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
                Files.writeString(lDefFile, lDefJson, standardEncoding, StandardOpenOption.CREATE);

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
            public <T> T toObject(String pSrc, Class<T> pType) throws UncheckedJsonException {
                try {
                    return jack.readValue(pSrc, pType);
                } catch (JsonProcessingException e) {
                    throw new UncheckedJsonException(UncheckedJsonException.TOOBJ_ERROR, e);
                }
            }

            @Override
            public String toString(Object pObj) {
                try {
                    return jack.writeValueAsString(pObj);
                } catch (JsonProcessingException e) {
                    throw new UncheckedJsonException(UncheckedJsonException.TOJSON_ERROR, e);
                }
            }
        };
        LOG.info(() -> String.format("%s json tool installed [%s]", INIT_LOGPRFX, ObjectMapper.class.getName()));
    }

    /**
     */
    protected void initServer() {
        server = new JamnServer(config.getProperties());
        server.setAccessManager(new DefaultServerAccessManager(config));
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
                .setFileEnricher(new DefaultFileEnricher(
                        new DefaultFileEnricherValueProvider(AppHome, config)))
                .build();

        // a playful construct
        // using lambda functions like overwritable methods
        UnaryOperator<String> orgMapping = lWebContentProvider.fileHelper.doPathMapping;
        lWebContentProvider.fileHelper.doPathMapping = path -> {
            String lPath = orgMapping.apply(path);
            return lPath.equals("/index.html") ? config.getWebAppMainPage() : lPath;
        };

        // add it to server
        server.addContentProvider(CONTENT_PROVIDER_ID, lWebContentProvider);
        LOG.info(() -> String.format("%s content provider installed [%s] on [%s]", INIT_LOGPRFX,
                JamnWebContentProvider.class.getSimpleName(), lRootPath));

    }

    /**
     * @throws WebServiceDefinitionException
     */
    protected void initWebServiceProvider() throws WebServiceDefinitionException {
        if (this.server != null && config.isWebServiceEnabled()) {

            // create the WebService provider
            webServiceProvider = JamnWebServiceProvider.newBuilder().setJsonTool(jsonTool);

            // set a predicate to identify a webservice request
            // by default all paths registered at the webservice provider
            // are ServiceRequests - see initContentDispatcher
            isServiceRequest = webServiceProvider::isServicePath;
            server.addContentProvider(SERVICE_PROVIDER_ID, webServiceProvider);

            LOG.info(() -> String.format("%s web service provider installed [%s]", INIT_LOGPRFX,
                    JamnWebServiceProvider.class.getSimpleName()));

            // install default app web services
            DefaultWebServices lDefaultWS = new DefaultWebServices(osIFace);
            registerWebServices(lDefaultWS);
        }
    }

    /**
     */
    protected void initWebSocketProvider() {
        if (this.server != null && config.isWebSocketEnabled()) {
            // create the WebSocketProvider
            webSocketProvider = JamnWebSocketProvider.newBuilder()
                    .addConnectionPath(config.getDefaultWebSocketUrlPath())
                    .build();

            webSocketProvider.addMessageProcessor(
                    new DefaultWebSocketMessageProcessor(getConfig(), getJsonTool(), webSocketProvider,
                            standardEncoding));

            server.addContentProvider(WEBSOCKET_PROVIDER_ID, webSocketProvider);

            LOG.info(() -> String.format("%s web socket provider installed [%s] at [%s]", INIT_LOGPRFX,
                    JamnWebSocketProvider.class.getSimpleName(),
                    "ws://<host>:" + server.getConfig().getPort() + config.getDefaultWebSocketUrlPath()));

        }
    }

    /**
     */
    protected void initContentDispatcher() {
        server.setContentProviderDispatcher((RequestMessage pRequest) -> {
            String lPath = pRequest.getPath();
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
        protected static final String DEFAULT_CONFIG = String.join(LF,
                "##",
                "## " + AppName + " Config Properties",
                "##", "",
                "#JPS Profile", JPS_PROFILE + "=" + APP_PROFILE, "",
                "#WebContentProvider files root folder", "web.file.root=" + WEB_FILE_ROOT, "",
                "#Web File-Enricher root folder", "web.file.enricher.root=http/jsmod/html-components", "",
                "#JPS extension libraries root folder", "jps.extension.root=" + EXTENSION_ROOT, "",
                "#JPS workspace root folder", "jps.workspace.root=" + WORKSPACE_ROOT, "",
                "#JPS Extension definition file name", "jps.extension.def.file=" + EXTENSION_DEF_FILE, "",
                "#WebApp main Page", "webapp.main.page=/workbench.html", "",
                "#Extensions enabled", "extensions.enabled=false", "",
                "#JavaScriptProvider script root folder", "script.root=" + SCRIPT_ROOT, "",
                "#JavaScript auto-load script", "js.auto.load.script=js-auto-load.js", "",
                "#CLI enabled", "cli.enabled=true", "",
                "#JavaScript enabled", "javascript.enabled=false", "",
                "#JavaScript debug enabled", "javascript.debug.enabled=false", "",
                "#Server enabled", "server.enabled=true", "",
                "#WebService enabled", "webservice.enabled=true", "",
                "#WebSocket enabled", "websocket.enabled=true", "",
                "#WebSocket default url path", "default.websocket.url.path=/wsoapi", "",
                "#Child WebSocket default url path", "default.child.websocket.url.path=/childapi", "",
                "#JVM debug option",
                "jvm.debug.option=-agentlib:jdwp=transport=dt_socket,address=localhost:9009,server=y,suspend=y", "",
                "#Server autostart", "server.autostart=true", "",
                "#Child process debug", "child.process.debug.enabled=false", "",
                "#Standard encoding", "standard.encoding=UTF-8", "",
                "#Windows shell encoding", "win.shell.encoding=Cp850", "",
                "#Unix shell encoding", "unix.shell.encoding=ISO8859_1", "");

        protected Properties props = new Properties();
        protected Properties buildProps = new Properties();

        private Config() {
        }

        private Config(InputStream pPropsIn) throws IOException {
            props.load(pPropsIn);
        }

        public void searchDynamicOption(String pKey, String pDefault) {
            // overwriting: if -D present -> use it or leave current
            props.put(pKey, System.getProperty(pKey, props.getProperty(pKey, pDefault)));
        }

        public Properties getBuildProperties() {
            return buildProps;
        }

        public String getProfile() {
            return props.getProperty(JPS_PROFILE, APP_PROFILE);
        }

        public String getJPSChildId() {
            return props.getProperty(JPS_CHILD_ID, "");
        }

        public String getJPSParentUrl() {
            return props.getProperty(JPS_PARENT_URL, "");
        }

        public String getDevLibsPath() {
            return props.getProperty(DEV_LIBS_PATH, "");
        }

        public String getWebFileRoot() {
            return props.getProperty("web.file.root", WEB_FILE_ROOT);
        }

        public String getWebFileEnricherRoot() {
            return props.getProperty("web.file.enricher.root", "http/jsmod/html-components");
        }

        public String getExtensionRoot() {
            return props.getProperty("jps.extension.root", EXTENSION_ROOT);
        }

        public String getWorkspaceRoot() {
            return props.getProperty("jps.workspace.root", WORKSPACE_ROOT);
        }

        public String getExtensionDefFileName() {
            return props.getProperty("jps.extension.def.file", EXTENSION_DEF_FILE);
        }

        public String getWebAppMainPage() {
            return props.getProperty("webapp.main.page", "/workbench.html");
        }

        public boolean isExtensionsEnabled() {
            return Boolean.parseBoolean(props.getProperty("extensions.enabled", "true"));
        }

        public String getScriptRoot() {
            return props.getProperty("script.root", SCRIPT_ROOT);
        }

        public String getJsAutoLoadScript() {
            return props.getProperty("js.auto.load.script", "js-auto-load.js");
        }

        public int getPort() {
            return Integer.valueOf(props.getProperty("port", "8099"));
        }

        public String getStandardEncoding() {
            return props.getProperty("standard.encoding", "UTF-8");
        }

        public String getWinShellEncoding() {
            return props.getProperty("win.shell.encoding", "Cp850");
        }

        public String getUnixShellEncoding() {
            return props.getProperty("unix.shell.encoding", "ISO8859_1");
        }

        public String getJVMDebugOption() {
            return props.getProperty("jvm.debug.option", "");
        }

        public String getDefaultWebSocketUrlPath() {
            return props.getProperty("default.websocket.url.path", "/wsoapi");
        }

        public String getDefaultChildWebSocketUrlPath() {
            return props.getProperty("default.child.websocket.url.path", "/childapi");
        }

        public boolean hasAppProfile() {
            return props.getProperty(JPS_PROFILE, APP_PROFILE).equals(APP_PROFILE);
        }

        public boolean hasChildProfile() {
            return props.getProperty(JPS_PROFILE, "").equals(CHILD_PROFILE);
        }

        public boolean isCliEnabled() {
            return Boolean.parseBoolean(props.getProperty("cli.enabled", "false"));
        }

        public boolean isChildProcessDebugEnabled() {
            return Boolean.parseBoolean(props.getProperty("child.process.debug.enabled", "false"));
        }

        public boolean isJavaScriptEnabled() {
            return Boolean.parseBoolean(props.getProperty("javascript.enabled", "false"));
        }

        public boolean isJavaScriptDebugEnabled() {
            return Boolean.parseBoolean(props.getProperty("javascript.debug.enabled", "false"));
        }

        public boolean isServerEnabled() {
            return Boolean.parseBoolean(props.getProperty("server.enabled", "true"));
        }

        public boolean isWebServiceEnabled() {
            return Boolean.parseBoolean(props.getProperty("webservice.enabled", "true"));
        }

        public boolean isWebSocketEnabled() {
            return Boolean.parseBoolean(props.getProperty("websocket.enabled", "true"));
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

    /**
     */
    public static class UncheckedJPSException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UncheckedJPSException(String pMsg) {
            super(pMsg);
        }

        UncheckedJPSException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }

    /*********************************************************
     * Common Helper class
     *********************************************************/
    /**
     */
    public static class CommonHelper {

        protected static Charset StandardEncoding = StandardCharsets.UTF_8;

        protected static void setEncoding(Charset pEncoding) {
            StandardEncoding = pEncoding;
        }

        /**
         * Arg Format: [-]<name>=<value>
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

        /**
         */
        public String[] rebuildQuotedWhitespaceStrings(String[] pToken) {
            List<String> newToken = new ArrayList<>();
            StringBuilder lBuffer = new StringBuilder();
            String tok = "";
            boolean inQuote = false;

            for (int i = 0; i < pToken.length; i++) {
                tok = pToken[i];

                if (tok.trim().startsWith("\"") && tok.trim().endsWith("\"")) {
                    newToken.add(tok);
                } else {
                    if (!inQuote && tok.contains("\"")) {
                        inQuote = true;
                        lBuffer = new StringBuilder(tok);
                        continue;
                    }
                    if (inQuote && tok.contains("\"")) {
                        inQuote = false;
                        lBuffer.append(" ").append(tok);
                        newToken.add(lBuffer.toString());
                    } else if (inQuote) {
                        lBuffer.append(" ").append(tok);
                    } else {
                        newToken.add(tok);
                    }
                }
            }

            if (inQuote) {
                throw new RuntimeException("Missing start/end quote in command line string");
            }

            return newToken.toArray(new String[newToken.size()]);
        }

    }
}
