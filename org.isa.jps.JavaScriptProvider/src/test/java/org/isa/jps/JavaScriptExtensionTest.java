/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 
 */
@DisplayName("JavaScriptExtension Test")
public class JavaScriptExtensionTest {

    private static Path SourcePathBase;
    private static Context JS;
    private static final String AppName = "Jamn Personal Server";

    private static Source getSourceFile(String pName) throws Exception {
        Path lSrcPath = Paths.get(SourcePathBase.toString(), pName);
        return Source.newBuilder("js", lSrcPath.toFile()).build();
    }

    @BeforeAll
    public static void setupEnvironment() {
        SourcePathBase = Paths.get("src", "test", "resources", "jsmod");
        JS = Context.newBuilder("js")
                .allowIO(true)
                .currentWorkingDirectory(SourcePathBase.toAbsolutePath())
                .option("engine.WarnInterpreterOnly", "false")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build();
    }

    @Test
    void testBasicExportImport() throws Exception {

        Value lVal = JS.eval(getSourceFile("call-greeter.mjs"));
        assertEquals("Hello User", lVal.asString());
    }

    @Test
    void testBasicJavaAccess() throws Exception {

        // instantiate java class and export js methods
        Value lAppIFace = JS.eval(getSourceFile("test-interface.mjs"));
        Value lVal = lAppIFace.getMember("appName").execute();
        assertEquals(AppName, lVal.asString());

        // call js method dynamically
        Value lBindings = JS.getBindings("js");
        lBindings.putMember("testMethod", "appName");
        lVal = JS.eval(getSourceFile("call-test-interface.mjs"));
        assertEquals(AppName, lVal.asString());
    }

    /**
     */
    public static class JavaTestInterfaceClass {

        public String appName() {
            return AppName;
        }

        public List<String> shellCmd(List<String> pCmdParts, String pWorkingDir) {
            return new ArrayList<>(pCmdParts);
        }
    }
}
