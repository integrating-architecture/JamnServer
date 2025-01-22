/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.isa.ipc.JamnWebSocketProvider;
import org.isa.jps.JamnPersonalServerApp;
import org.isa.jps.JamnPersonalServerApp.CommonHelper;
import org.isa.jps.JamnPersonalServerApp.Config;
import org.isa.jps.comp.OperatingSystemInterface.ShellProcess;
import org.isa.jps.comp.OperatingSystemInterface.ShellProcessListener;

/**
 * <pre>
 * The ChildProcessManager provides the ability to create JamnPersonalServerApp instances 
 * as a separate java child process running in a their own JavaVM.
 * 
 * The instance is created with a "profile" parameter
 * that specifies functionality and features of that instance.
 * 
 * The default use case is the "child" profile.
 * </pre>
 */
public class ChildProcessManager implements ShellProcessListener {

    protected static Logger LOG = Logger.getLogger(ChildProcessManager.class.getName());

    protected static CommonHelper Tool = JamnPersonalServerApp.Tool;
    protected static final String PROFILE = "-" + JamnPersonalServerApp.JPS_PROFILE;
    protected static final String CHILD = JamnPersonalServerApp.CHILD_PROFILE;
    protected static final String CHILD_ID = "-" + JamnPersonalServerApp.JPS_CHILD_ID;
    protected static final String PARENT_URL = "-" + JamnPersonalServerApp.JPS_PARENT_URL;

    protected static String Arg(String pKey, String pVal) {
        return pKey + "=" + pVal;
    }

    protected Map<String, ChildProcess> openChildren = Collections.synchronizedMap(new HashMap<>());

    protected OperatingSystemInterface osIFace;
    protected String appHome;
    protected Config config;
    protected String workspaceRoot;
    protected ChildProcessDef defaultChildDef;
    protected String defaultParentUrl;

    protected ChildProcessManager() {
    }

    public ChildProcessManager(OperatingSystemInterface pOsIFace, Path pAppHome, Config pConfig) throws IOException {
        this();
        osIFace = pOsIFace;
        appHome = pAppHome.toString();
        config = pConfig;
        initialize();
    }

    /**
     * @throws IOException
     */
    protected void initialize() throws IOException {
        workspaceRoot = Tool.ensureSubDir(config.getWorkspaceRoot(), Paths.get(appHome)).toString();

        defaultChildDef = new ChildProcessDef()
                .setClassPath(Paths.get(appHome, "libs").toString() + File.separator + "*")
                .setOptions("-Dpolyglot.engine.WarnInterpreterOnly=false")
                .setMainClass(JamnPersonalServerApp.class.getName())
                .setWorkDir(workspaceRoot);

        defaultParentUrl = "ws://localhost:" + config.getPort() + JamnWebSocketProvider.DefaultPath;
    }

    /**
     */
    public String createProcess() {

        ChildProcess lChild = new ChildProcess(UUID.randomUUID().toString());
        ChildProcessDef lDef = new ChildProcessDef(defaultChildDef);

        // since child processes run in their own VM they do NOT share an IDE classpath
        // if present set an alternative class path for development
        // e.g. something like -Ddev.libs.path =
        // c:\..\..\org.isa.jps.JamnPersonalServerApp\dist\jps.home\libs\*
        if (!config.getDevLibsPath().isEmpty()) {
            lDef.setClassPath(config.getDevLibsPath());
        }

        String[] lArgs = new String[] {
                Arg(PROFILE, CHILD),
                Arg(CHILD_ID, lChild.getId()),
                Arg(PARENT_URL, defaultParentUrl)
        };
        lChild.setProcess(osIFace.new ShellProcess(lChild.getId())
                .setWorkingDir(lDef.getWorkDir())
                .setCommand(lDef.getCommandLineFor(lChild, lArgs))
                .setListener(this));

        openChildren.put(lChild.getId(), lChild);

        new Thread(() -> lChild.getProcess().start()).start();

        return lChild.getId();
    }

    @Override
    public void onClose(String pId) {
        if (openChildren.containsKey(pId)) {
            openChildren.remove(pId);
        }
    }

    /**
     */
    public void closeProcess(String pId) {
        ChildProcess lChild;
        if (openChildren.containsKey(pId)) {
            lChild = openChildren.get(pId);
            lChild.getProcess().close();
        }
    }

    /**
     */
    public Set<String> getProcessList() {
        return openChildren.keySet();
    }

    /**
     */
    public static class ChildProcessDef {
        protected String processId;
        protected String javaHome;
        protected String javaExe;
        protected String classPath;
        protected String options;
        protected String mainClass;
        protected String workDir;

        public ChildProcessDef() {
            javaHome = System.getProperty("java.home");
            javaExe = Paths.get(javaHome, "bin", "java.exe").toString();
        }

        public ChildProcessDef(ChildProcessDef pOrg) {
            this.javaHome = pOrg.javaHome;
            this.javaExe = pOrg.javaExe;
            this.classPath = pOrg.classPath;
            this.options = pOrg.options;
            this.mainClass = pOrg.mainClass;
            this.workDir = pOrg.workDir;
        }

        /**
         */
        public String[] getCommandLineFor(ChildProcess pChild, String... pArgs) {
            processId = pChild.getId();
            List<String> lCmdParts = new ArrayList<>();

            lCmdParts.add(javaExe);
            lCmdParts.add(options);
            lCmdParts.add("-cp");
            lCmdParts.add(classPath);
            lCmdParts.add(mainClass);
            if (pArgs != null && pArgs.length > 0) {
                lCmdParts.addAll(Arrays.asList(pArgs));
            }
            return lCmdParts.toArray(new String[] {});
        }

        public ChildProcessDef setJavaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public ChildProcessDef setJavaExe(String javaExe) {
            this.javaExe = javaExe;
            return this;
        }

        public ChildProcessDef setClassPath(String classPath) {
            this.classPath = classPath;
            return this;
        }

        public ChildProcessDef setOptions(String options) {
            this.options = options;
            return this;
        }

        public ChildProcessDef setMainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public ChildProcessDef setWorkDir(String workDir) {
            this.workDir = workDir;
            return this;
        }

        public String getJavaHome() {
            return javaHome;
        }

        public String getJavaExe() {
            return javaExe;
        }

        public String getClassPath() {
            return classPath;
        }

        public String getOptions() {
            return options;
        }

        public String getMainClass() {
            return mainClass;
        }

        public String getWorkDir() {
            return workDir;
        }
    }

    /**
     */
    protected class ChildProcess {
        protected String id = "";
        protected ShellProcess process;

        protected ChildProcess(String pId) {
            id = pId;
        }

        public String getId() {
            return id;
        }

        public ShellProcess getProcess() {
            return process;
        }

        public void setProcess(ShellProcess process) {
            this.process = process;
        }
    }
}
