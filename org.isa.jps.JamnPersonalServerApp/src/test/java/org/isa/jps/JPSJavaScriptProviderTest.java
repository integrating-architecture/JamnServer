/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.JsonToolWrapper;
import org.isa.jps.JavaScriptProvider.JsValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 
 */
@DisplayName("JPS JavaScriptProvider Test")
public class JPSJavaScriptProviderTest {

    private static Logger LOG = Logger.getLogger(JPSJavaScriptProviderTest.class.getName());

    private static JamnPersonalServerApp ServerApp;
    private static JsonToolWrapper Json;
    private static JavaScriptProvider JavaScript;

    @BeforeAll
    public static void setupEnvironment() {
        ServerApp = JamnPersonalServerApp.getInstance();

        // IMPORTANT
        // uses the default runtime script path
        // - org.isa.jps.JamnPersonalServerApp\scripts
        // NOT test/resources
        ServerApp.start(new String[] {
                "javascript.enabled=true"
        });
        assertTrue(ServerApp.isRunning(), "Test Server start FAILED");

        Json = ServerApp.getJsonTool();
        assertNotNull(Json, "Json init FAILED");

        JavaScript = ServerApp.getJavaScript();
        assertNotNull(JavaScript, "JavaScript init FAILED");
    }

    @AfterAll
    public static void shutDownServer() {
        ServerApp.close();
        LOG.info("Test(s) finished");
    }

    @Test
    void testCliJsInfoCommand() {
        String lVal = ServerApp.getCli().execCmdBlank("jsinfo");
        assertTrue(lVal.contains("Graal.version"), "Error js command");
    }

    @Test
    void testJsShellCommand() {
        String lMsg = "JsShellCommand error";

        // call a os command - e.g. dir
        JsValue lVal = JavaScript.eval("/junit_tests/sh-test.mjs");
        assertTrue(lVal.asString().contains("jps.properties"), lMsg);

        // call a script file with path - work dir is current app dir
        // general "/" DOES NOT work in command on windows
        String lCmd = "scripts/junit_tests/test.cmd";
        lVal = JavaScript.eval("/junit_tests/sh-test.mjs", lCmd);
        assertFalse(lVal.asString().contains("jps.properties"), lMsg);

        lCmd = Paths.get(lCmd).toString();
        lVal = JavaScript.eval("/junit_tests/sh-test.mjs", lCmd);
        assertTrue(lVal.asString().contains("jps.properties"), lMsg);

        // call a script file in a work dir - general "/" works in work dir path
        lVal = JavaScript.eval("/junit_tests/sh-test.mjs", "test.cmd", "scripts/junit_tests");
        assertTrue(lVal.asString().contains("test.cmd"), lMsg);

    }

}
