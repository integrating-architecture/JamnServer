/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import org.isa.jps.JamnPersonalServerApp;
import org.isa.jps.JavaScriptProvider;
import org.isa.jps.JavaScriptProvider.JavaScriptHostApp;

/**
 * <pre>
 * Rudimentary example of a JavaScript Host App Interface. 
 * 
 * The JavaScriptHostApp is used as an Interface between the PersonalServerApp and the JavaScript Provider.
 * The purpose is - precisely because it is easily possible - 
 * to avoid using app specific Java classes in JavaScript.
 * </pre>
 */
public class DefaultJavaScriptHostApp implements JavaScriptHostApp {

    protected static Logger LOG = Logger.getLogger(DefaultJavaScriptHostApp.class.getName());

    protected String name;
    protected JavaScriptProvider javaScript;
    protected CommandLineInterface cli;
    protected OperatingSystemInterface osIFace;

    /**
     */
    public DefaultJavaScriptHostApp(JamnPersonalServerApp pApp) {
        name = JamnPersonalServerApp.AppName;
        javaScript = pApp.getJavaScript();
        cli = pApp.getCli();
        osIFace = pApp.getOsIFace();
    }

    /*******************************************************************************
     * Public interface
     *******************************************************************************/

    /**
     */
    @Override
    public void initialize() {
        createDefaultCliCommands();
    }

    @Override
    public String name() {
        return name;
    }

    public String ls() {
        return System.lineSeparator();
    }

    /**
     */
    public String path(String pPath, String... pParts) {
        return Paths.get(pPath, pParts).toString();
    }

    /**
     * Create a Cli Command for a JavaScript.
     */
    public void createJSCliCommand(String pName, String pSource, String pDescr) {
        if (cli != null) {
            if (javaScript.sourceExists(pSource)) {
                cli.newCommandBuilder()
                        .name(pName)
                        .descr(pDescr)
                        .function(ctx -> evalScript(pSource, ctx.getArgsArray()))
                        .build();
            } else {
                throw new UncheckedJavaScriptHostException(
                        String.format("JS HostApp - cli command script NOT FOUND [%s] [%s]", pName,
                                javaScript.getSourcePath(pSource)));
            }
        }
    }

    /**
     */
    public List<String> shellCmd(String pCmdLine, String pWorkingDir) {
        List<String> lResult;
        String[] lCmdParts = JamnPersonalServerApp.Tool.rebuildQuotedWhitespaceStrings(pCmdLine.split(" "), false);

        lResult = osIFace.fnc().shellCmd(lCmdParts, pWorkingDir, false);
        return lResult;
    }

    /*******************************************************************************
     * Internals
     *******************************************************************************/

    /**
     */
    protected String evalScript(String pFileName, String... pArgs) {
        Object lResult = javaScript.eval(pFileName, pArgs);
        return lResult != null ? lResult.toString() : "";
    }

    /**
     */
    protected void createDefaultCliCommands() {
        if (cli != null) {
            cli.newCommandBuilder()
                    .name("runjs")
                    .descr("Run a JS script: runjs <filename> <args ...>")
                    .function(ctx -> {
                        String scriptName = ctx.getArgsList().remove(0);
                        return evalScript(scriptName, ctx.getArgsArray());
                    })
                    .build();
        }
    }

    /*******************************************************************************
     *******************************************************************************/
    /**
     */
    public static class UncheckedJavaScriptHostException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedJavaScriptHostException(String pMsg) {
            super(pMsg);
        }

        public UncheckedJavaScriptHostException(String pMsg, Exception pCause) {
            super(pMsg, pCause);
        }
    }

}
