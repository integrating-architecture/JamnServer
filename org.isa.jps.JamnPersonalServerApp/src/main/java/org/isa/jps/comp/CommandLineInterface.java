/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import java.io.Console;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.isa.jps.JamnPersonalServerApp;
import org.isa.jps.JamnPersonalServerApp.CommonHelper;

/**
 * This component realizes a simple REPL console interface.
 */
public class CommandLineInterface {

    protected static final String LS = System.lineSeparator();

    protected static final Logger LOG = Logger.getLogger(CommandLineInterface.class.getName());
    protected static final CommonHelper Tool = JamnPersonalServerApp.Tool;
    protected static final CliCommand UnknownCommand = new CliCommand("", args -> "unknown command");

    protected Thread worker;
    protected boolean work = false;
    protected Map<String, CliCommand> commands = new HashMap<>();
    protected Function<String, String> commandProcessor;
    protected ConsoleWrapper console;

    protected Predicate<String> defaultHelpChecker = (
            input) -> ("?".equals(input) || "h".equals(input) || "help".equals(input));

    public CommandLineInterface() {
        commandProcessor = createDefaultCommandProcessor();
        console = new ConsoleWrapper(() -> "jps>");
    }

    /**
     */
    public synchronized void start() {
        if (worker == null) {
            if (console.useSysConsole()) {
                LOG.info(() -> "Start CLI on system console");
            } else {
                LOG.info(() -> "Start CLI on Standard-IO");
            }
            work = true;
            worker = new Thread(() -> run(commandProcessor));
            worker.start();
        }
    }

    /**
     */
    public synchronized void stop() {
        work = false;
    }

    /*********************************************************
     * internal methods and classes
     *********************************************************/

    /**
     * <pre>
     * Default implementation of a command processor
     * that gets invoked for a command line typed and submitted in the REPL console.
     * </pre>
     */
    protected Function<String, String> createDefaultCommandProcessor() {
        return (commandLine) -> {
            if (commandLine != null) {
                commandLine = commandLine.trim();
            }
            String name = "";
            String[] args = {};
            String[] token = tokenizeCmdLine(commandLine);

            if (token.length >= 1) {
                name = token[0];
            }
            if (token.length >= 2) {
                args = new String[token.length - 1];
                System.arraycopy(token, 1, args, 0, args.length);
            }
            if (name.isEmpty()) {
                return "";
            }
            if (defaultHelpChecker.test(name)) {
                return getHelp();
            }
            CliCommand cmd = commands.getOrDefault(name, UnknownCommand);
            if (args.length > 0 && defaultHelpChecker.test(args[0])) {
                return cmd.getDescr();
            }
            return cmd.execute(args, console);
        };
    }

    /**
     * The REPL implementation
     */
    protected void run(Function<String, String> pCommandProcessor) {
        String lInputLine = "";
        String lResultValue = "";

        while (work) {
            try {
                // R-ead a line from System.in
                lInputLine = console.readNextLine();
                // E-val command line with the pCommandProcessor
                lResultValue = pCommandProcessor.apply(lInputLine);
                // P-rint a possible return to out stream
                if (lResultValue != null && !lResultValue.trim().isEmpty()) {
                    console.printValue(lResultValue);
                }
                // L-oop
            } catch (Throwable t) {
                LOG.severe(
                        () -> String.format("CLI ERROR [%s]%s%s", t, LS, Tool.getStackTraceFrom(t)));
            }
        }
    }

    /**
     */
    protected void addCommand(CliCommand pCmd) {
        commands.put(pCmd.getName(), pCmd);
    }

    /**
     */
    protected String getHelp() {
        return commands.entrySet()
                .stream()
                .map(e -> e.getValue().getDescr())
                .collect(Collectors.joining(LS));
    }

    /**
     */
    public static class CommandCallContext {
        private List<String> args;
        private ConsoleWrapper console;
        private Map<String, String> argMap;

        private CommandCallContext() {
        }

        private CommandCallContext(String[] pArgs, ConsoleWrapper pConsole) {
            args = Arrays.asList(pArgs);
            console = pConsole;
        }

        public String get(int pIdx) {
            return args.get(pIdx);
        }

        public boolean hasArg(String pKey) {
            return args.contains(pKey);
        }

        public List<String> getArgs() {
            return args;
        }

        Map<String, String> parseArgsToMap() {
            return parseArgsToMap(Tool.defaultArgParser);
        }

        Map<String, String> parseArgsToMap(Function<String[], Map<String, String>> pParser) {
            if (argMap == null) {
                argMap = pParser.apply(args.toArray(new String[] {}));
            }
            return argMap;
        }

        public boolean getConfirmation(String pMessage) {
            return getConfirmation(pMessage, (answer) -> "y".equals(answer));
        }

        public boolean getConfirmation(String pMessage, Predicate<String> pPredicate) {
            String lRet = console.readConfirmationLine(pMessage);
            return pPredicate.test(lRet);
        }

    }

    /**
     */
    private static class CliCommand {
        private String name = "";
        private String descr = "";
        private Function<CommandCallContext, String> function = (ctx) -> "";

        private CliCommand() {
        }

        private CliCommand(String pKey, Function<CommandCallContext, String> pFunction) {
            name = pKey;
            function = pFunction;
        }

        public String execute(String[] pArgs, ConsoleWrapper pConsole) {
            return function.apply(new CommandCallContext(pArgs, pConsole));
        }

        public String getName() {
            return name;
        }

        public String getDescr() {
            return descr;
        }
    }

    public CommandBuilder newCommandBuilder() {
        return new CommandBuilder();
    }

    public class CommandBuilder {
        private CliCommand cmd = new CliCommand();

        private CommandBuilder() {
        }

        public CommandBuilder name(String pName) {
            cmd.name = pName;
            return this;
        }

        public CommandBuilder descr(String pDescr) {
            cmd.descr = pDescr;
            return this;
        }

        public CommandBuilder function(Function<CommandCallContext, String> pFunction) {
            cmd.function = pFunction;
            return this;
        }

        public void build() {
            addCommand(cmd);
        }
    }

    /**
     */
    public static class ConsoleWrapper {
        private Supplier<String> prompt;
        private PrintStream outStream;
        private Console sysConsole;
        private Scanner scanner;

        private ConsoleWrapper() {
        }

        private ConsoleWrapper(Supplier<String> pPrompt) {
            this();
            prompt = pPrompt;
            sysConsole = System.console();
            if (sysConsole == null) {
                outStream = System.out;
                scanner = new Scanner(System.in);
            }
        }

        /**
         */
        public boolean useSysConsole() {
            return sysConsole != null;
        }

        /**
         */
        public String readNextLine() {
            String lInput = "";
            if (useSysConsole()) {
                printPrompt();
                lInput = sysConsole.readLine();
            } else {
                printPrompt();
                lInput = scanner.nextLine();
            }
            if (lInput != null) {
                lInput = lInput.trim();
            }
            return lInput;
        }

        /**
         */
        public String readConfirmationLine(String pMsg) {
            String lInput = "";
            if (useSysConsole()) {
                sysConsole.printf(pMsg);
                lInput = sysConsole.readLine();
            } else {
                outStream.print(pMsg);
                lInput = scanner.nextLine();
            }
            if (lInput != null) {
                lInput = lInput.trim();
            }
            return lInput;
        }

        /**
         */
        protected void printValue(Object pValue) {
            if (pValue != null) {
                if (useSysConsole()) {
                    sysConsole.printf("%s%s", pValue, LS);
                    sysConsole.printf(LS);
                } else {
                    outStream.println(pValue);
                    outStream.print(LS);
                }
            }
        }

        /**
         */
        protected void printPrompt() {
            if (useSysConsole()) {
                sysConsole.printf(prompt.get());
            } else {
                outStream.print(prompt.get());
            }
        }

    }

    /**
     */
    protected static String[] tokenizeCmdLine(String pCmdLine) {
        return rebuildQuotedWhitespaceStrings(pCmdLine.split(" "), false);
    }

    /**
     */
    protected static String[] rebuildQuotedWhitespaceStrings(String[] pToken, boolean pRemoveQuotes) {
        List<String> newToken = new ArrayList<>();
        StringBuilder lBuffer = new StringBuilder();
        String tok = "";
        boolean inQuote = false;

        for (int i = 0; i < pToken.length; i++) {
            tok = pToken[i];
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

        if (inQuote) {
            throw new RuntimeException("Missing start/end quote in command line string");
        }

        return newToken.toArray(new String[newToken.size()]);
    }
}
