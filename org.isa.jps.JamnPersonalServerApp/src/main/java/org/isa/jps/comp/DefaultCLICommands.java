/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.isa.jps.JamnPersonalServerApp;

/**
 * <pre>
 * The class realizes internal cli functions to be registered at the AppCommandLineInterface.
 * see: JamnPersonalServerApp.initCli
 * </pre>
 */
public class DefaultCLICommands {

    protected static final String LS = System.lineSeparator();
    protected static final Logger LOG = Logger.getLogger(DefaultCLICommands.class.getName());

    private DefaultCLICommands() {
    }

    public static void create() {
        JamnPersonalServerApp app = JamnPersonalServerApp.getInstance();
        CommandLineInterface cli = app.getCli();

        cli.newCommandBuilder()
                .name("server")
                .descr("Server command: server [start, stop]")
                .function(ctx -> {
                    if (ctx.hasArg("start")) {
                        app.getServer().start();
                    } else if (ctx.hasArg("stop")) {
                        app.getServer().stop();
                    }
                    return "";
                })
                .build();

        cli.newCommandBuilder()
                .name("cls")
                .descr("Cli command clear screen: cls")
                .function(ctx -> {
                    app.getSysIFace().functions().shellCmd(new String[] { "cls" }, null, true);
                    return "";
                })
                .build();

        cli.newCommandBuilder()
                .name("system")
                .descr("System command: system [config, properties, shutdown, kill]")
                .function(ctx -> {
                    String lResult = "";

                    if (ctx.hasArg("shutdown")) {
                        if (ctx.getConfirmation("Do you really want to shutdown (y/n) ?")) {
                            if (ctx.hasArg("kill")) {
                                LOG.info("Going to KILL application");
                                System.exit(1);
                            } else {
                                LOG.info("Going to shutdown application");
                                app.stop();
                                System.exit(0);
                            }
                        } else {
                            return "nothing done";
                        }
                    }

                    if (ctx.hasArg("config")) {
                        lResult = app.getConfig().getProperties().entrySet()
                                .stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(LS));
                    } else if (ctx.hasArg("properties")) {
                        lResult = System.getProperties().entrySet()
                                .stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(LS));
                    }
                    return lResult;
                })
                .build();
    }
}
