/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.isa.jps.JamnPersonalServerApp.Config;

/**
 */
public class OperatingSystemInterface {

    protected static boolean Windows = true;
    protected static boolean Unix = false;

    protected OSFunctions osFunctions;

    /**
     */
    protected OperatingSystemInterface() {
        String lName = System.getProperty("os.name").toLowerCase();
        Windows = lName.contains("win");
        Unix = (lName.contains("nix") || lName.contains("nux") || lName.contains("aix"));
    }

    /**
     */
    public OperatingSystemInterface(Config pConfig) {
        this();
        if (Windows) {
            osFunctions = new WinowsFunctions(pConfig);
        } else if (Unix) {
            osFunctions = new UnixFunctions(pConfig);
        }
    }

    /**
     */
    public boolean isOnWindows() {
        return Windows;
    }

    /**
     */
    public boolean isOnUnix() {
        return Unix;
    }

    /**
     */
    public OSFunctions functions() {
        return osFunctions;
    }

    /**
     */
    public static interface OSFunctions {
        /**
         */
        public Charset getShellEncoding();

        /**
         */
        public List<String> shellCmd(String[] pCmdParts, String pWorkingDir, boolean pInherit);
    }

    /**
     */
    protected abstract static class AbstractOSFunctions implements OSFunctions {
        protected Charset shellEncoding;
        protected Config config;

        AbstractOSFunctions(Config pConfig) {
            config = pConfig;
        }

        @Override
        public Charset getShellEncoding() {
            if (shellEncoding == null) {
                if (Windows) {
                    shellEncoding = Charset.forName(config.getWinShellEncoding());
                } else if (Unix) {
                    shellEncoding = Charset.forName(config.getUnixShellEncoding());
                } else {
                    shellEncoding = StandardCharsets.UTF_8;
                }
            }
            return shellEncoding;
        }

        @Override
        public List<String> shellCmd(String[] pCmdParts, String pWorkingDir, boolean pInherit) {
            String line = "";
            ProcessBuilder builder = null;
            Process process = null;
            BufferedReader stdInput = null;
            BufferedReader stdError = null;
            List<String> result = new ArrayList<>();
            List<String> errResult = new ArrayList<>();

            List<String> command = new ArrayList<>(Arrays.asList(pCmdParts));

            if (Windows) {
                command.add(0, "cmd");
                command.add(1, "/c");
            }

            try {
                try {
                    builder = new ProcessBuilder();
                    builder.command(command);
                    if (pInherit) {
                        builder.inheritIO();
                    }
                    if (pWorkingDir != null && !pWorkingDir.isEmpty()) {
                        builder.directory(new File(pWorkingDir));
                    }
                    process = builder.start();

                    stdInput = new BufferedReader(new InputStreamReader(process.getInputStream(), getShellEncoding()));
                    while ((line = stdInput.readLine()) != null) {
                        result.add(line);
                    }
                    stdError = new BufferedReader(new InputStreamReader(process.getErrorStream(), getShellEncoding()));
                    while ((line = stdError.readLine()) != null) {
                        errResult.add(line);
                    }

                    if (result.isEmpty()) {
                        result.addAll(errResult);
                    }

                    process.waitFor();

                } finally {
                    if (process != null) {
                        process.destroy();
                        process.destroyForcibly();
                    }
                    if (stdInput != null) {
                        stdInput.close();
                    }
                    if (stdError != null) {
                        stdError.close();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }

    /**
     */
    protected static class WinowsFunctions extends AbstractOSFunctions {
        protected WinowsFunctions(Config pConfig) {
            super(pConfig);
        }
    }

    /**
     */
    protected static class UnixFunctions extends AbstractOSFunctions {
        protected UnixFunctions(Config pConfig) {
            super(pConfig);
        }
    }
}
