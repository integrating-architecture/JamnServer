/* Authored by www.integrating-architecture.de */
package org.any;

import org.isa.jps.JamnPersonalServerApp;
import org.isa.jps.comp.CommandLineInterface;

/**
 * 
 */
public class TestExtension {

    public TestExtension() {
        createCliCommands();
    }

    private void createCliCommands() {
        CommandLineInterface cli = JamnPersonalServerApp.getInstance().getCli();

        cli.newCommandBuilder()
                .name("test")
                .descr("Extension Test command: test")
                .function(ctx -> "Hello from Test Command")
                .build();
    }

}
