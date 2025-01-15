/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.logging.Logger;

import org.isa.jps.JavaScriptProvider;
import org.isa.jps.JavaScriptProvider.JavaScriptHostApp;

/**
 * <pre>
 * </pre>
 */
public class DefaultJavaScriptHostApp implements JavaScriptHostApp {

    protected static Logger LOG = Logger.getLogger(DefaultJavaScriptHostApp.class.getName());

    protected String name;
    protected JavaScriptProvider javaScript;
    protected CommandLineInterface cli;

    public DefaultJavaScriptHostApp(String pName, JavaScriptProvider pJavaScript, CommandLineInterface pCli) {
        name = pName;
        javaScript = pJavaScript;
        cli = pCli;
    }

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

    /**
     * <pre>
     * In case of Personal Server App 
     * installing a js script means - creating a cli command that calls the script.
     * </pre>
     */
    @Override
    public void installScript(String pSource, String pName, String pDescr, String pType) {
        if (cli != null) {
            if (javaScript.sourceExists(pSource)) {
                cli.newCommandBuilder()
                        .name(pName)
                        .descr(pDescr)
                        .function(ctx -> evalScript(pSource, ctx.getArgsArray()))
                        .build();
            } else {
                LOG.severe(() -> String.format("ERROR WARNING - JS HostApp script source NOT FOUND [%s] [%s]", pName,
                        javaScript.getSourcePath(pSource)));
            }
        }
    }

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
}
