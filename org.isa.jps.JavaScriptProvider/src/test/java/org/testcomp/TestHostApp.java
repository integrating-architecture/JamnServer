/* Authored by www.integrating-architecture.de */
package org.testcomp;

import org.isa.jps.JavaScriptProvider.JavaScriptHostApp;

public class TestHostApp implements JavaScriptHostApp {

    private String name;

    public TestHostApp(String pName) {
        name = pName;
    }

    @Override
    public String name() {
        return name;
    }

}
