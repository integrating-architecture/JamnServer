/* Authored by www.integrating-architecture.de */
package org.any;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_PLAIN;

import org.isa.ipc.JamnWebServiceProvider.WebService;
import org.isa.ipc.JamnWebServiceProvider.WebServiceDefinitionException;
import org.isa.jps.JamnPersonalServerApp;

/**
 * 
 */
public class TestWebService {

    public TestWebService() throws WebServiceDefinitionException {
        JamnPersonalServerApp.getInstance().registerWebServices(this);
    }

    @WebService(path = "/testws", methods = { "GET" }, contentType = TEXT_PLAIN)
    public String testws() {
        return String.format("Hello from Test WebService Extension [%s]", getClass().getName());
    }

}
