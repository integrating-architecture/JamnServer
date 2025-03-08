/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
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
                    String lCmd = app.getOsIFace().isOnUnix() ? "clear" : "cls";
                    app.getOsIFace().fnc().shellCmd(new String[] { lCmd }, null, true, null);
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
                                app.close();
                                System.exit(0);
                            }
                        } else {
                            return "nothing done";
                        }
                    }

                    if (ctx.hasArg("config")) {
                        lResult = propsToMap.apply(app.getConfig().getProperties())
                                .entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(LS));
                    } else if (ctx.hasArg("properties")) {
                        lResult = propsToMap.apply(System.getProperties())
                                .entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(LS));
                    }
                    return lResult;
                })
                .build();

        cli.newCommandBuilder()
                .name("process")
                .descr("Child process Manager: process")
                .function(ctx -> {
                    String lResult = "";
                    if (ctx.hasArg(0, "create")) {
                        lResult = app.getChildProcessManager().createProcess();
                    } else if (ctx.hasArg(0, "close")) {
                        app.getChildProcessManager().closeProcess(ctx.get(1));
                    } else if (ctx.hasArg(0, "list")) {
                        lResult = String.join(LS, app.getChildProcessManager().getProcessList());
                    } else if (ctx.hasArg(0, "send")) {
                        lResult = app.getChildProcessManager().sendCommand(ctx.get(1), ctx.get(2));
                    }
                    return lResult;
                })
                .build();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<Properties, Map<String, String>> propsToMap = props -> new HashMap<>((Map) props);
}
