/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.graalvm.polyglot.Value;
import org.isa.jps.JavaScriptProvider.JsValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 
 */
@DisplayName("JavaScriptProvider Test")
class JavaScriptProviderTest {

    private static Path SourcePathBase = Paths.get("src", "test", "resources", "jsmod");

    private static JavaScriptProvider JavaScript;

    @BeforeAll
    private static void setupEnvironment() {

        Properties lConfig = new Properties();
        JavaScript = new JavaScriptProvider(SourcePathBase, lConfig);
        JavaScript.setHostAppAdapter(new TestJSHostAppAdapter());
        JavaScript.initialize();

    }

    @Test
    void testEvalAndArgs() {

        JsValue lVal = JavaScript.eval("js-eval-and-args.js");
        assertEquals("no args", lVal.asString(), "EvalAndArgs error");

        lVal = JavaScript.eval("js-eval-and-args.js", "arg-0");
        assertEquals("arg-0", lVal.asString(), "EvalAndArgs error");

    }

    @Test
    void testJavaClassAccess() {

        JsValue lVal = JavaScript.eval("java-class-access.js");
        assertEquals("Name is: TestClass", lVal.asString(), "Java Class error");
    }

    @Test
    void testJsNullReturn() {

        JsValue lVal = JavaScript.eval("js-return-null.js");
        assertTrue(lVal.isNull());
        assertEquals(null, lVal.asString());
    }

    @Test
    void testJsFunctionReturn() {

        JsValue lVal = JavaScript.eval("js-return-function.js");
        assertFalse(lVal.isNull());
        assertEquals("function return value", lVal.asString());
    }

    @Test
    void testJsModuleImport() {

        JsValue lVal = JavaScript.eval("js-mod-fnc-import.mjs");
        assertFalse(lVal.isNull());
        assertEquals("Hello User", lVal.asString());
    }

    /********************************************************************************
     ********************************************************************************/

    @SuppressWarnings("unused")
    private static void inspect(Value pVal, String pName) {
        System.out.println(pName);
        System.out.println("hasMembers: " + pVal.hasMembers());
        System.out.println("getMemberKeys: " + pVal.getMemberKeys());
        System.out.println("isHostObject: " + pVal.isHostObject());
        System.out.println("isIterator: " + pVal.isIterator());
        System.out.println("isMetaObject : " + pVal.isMetaObject());
        System.out.println("isNativePointer : " + pVal.isNativePointer());
        System.out.println("isProxyObject : " + pVal.isProxyObject());

        System.out.println("hasArrayElements : " + pVal.hasArrayElements());
        System.out.println("hasHashEntries : " + pVal.hasHashEntries());
        System.out.println("hasIterator : " + pVal.hasIterator());
        System.out.println("getIterator : " + pVal.getIterator());
        System.out.println("\n");

    }
}
