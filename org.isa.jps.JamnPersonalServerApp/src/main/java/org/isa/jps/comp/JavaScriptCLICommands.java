/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import org.isa.jps.JavaScriptExtension;

/**
 * <pre>
 * </pre>
 */
public class JavaScriptCLICommands {

    protected JavaScriptExtension javaScript;

    public JavaScriptCLICommands(JavaScriptExtension pJavaScript) {
        javaScript = pJavaScript;
    }

    /**
     */
    protected String evalScript(String pFileName) {
        Object lResult = javaScript.eval(pFileName);
        return lResult != null ? lResult.toString() : "";
    }

    public void createCommandsFor(CommandLineInterface pCli) {
        pCli.newCommandBuilder()
                .name("jsinfo")
                .descr("Show JavaScript Infos: jsinfo")
                .function((ctx) -> evalScript("js-info.js"))
                .build();

        pCli.newCommandBuilder()
                .name("jsrun")
                .descr("Run a JS script: jsrun <filename>")
                .function((ctx) -> {
                    String name = ctx.get(0);
                    return evalScript(name);
                })
                .build();

    }
}
