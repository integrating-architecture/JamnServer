/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import java.io.IOException;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebServiceProvider;
import org.isa.ipc.sample.web.api.SampleWebApiServices;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * A test app for starting a JamnServer with the sample Service API.
 * </pre>
 */
public class WebServiceProviderSampleApp {

    public static void main(String[] args) throws Exception {

        // create a Jamn server
        JamnServer lServer = new JamnServer();
        // required for using Testfiles in a browser
        lServer.getConfig().setCORSEnabled(true);

        // create the WebService provider
        JamnWebServiceProvider lWebServiceProvider = JamnWebServiceProvider.newBuilder().setConfig(lServer.getConfig())
                // create a json tool wrapper for the JamnWebServiceProvider
                .setJsonTool(new JamnServer.JsonToolWrapper() {
                    private final ObjectMapper jack = new ObjectMapper().setVisibility(PropertyAccessor.FIELD,
                            Visibility.ANY);

                    @Override
                    public <T> T toObject(String pSrc, Class<T> pType) throws IOException {
                        return jack.readValue(pSrc, pType);
                    }

                    @Override
                    public String toString(Object pObj) throws IOException {
                        return jack.writeValueAsString(pObj);
                    }
                })
                // register the Web-API Services
                .registerServices(SampleWebApiServices.class).build();

        // add the provider to the server
        lServer.addContentProvider("WebServiceProvider", lWebServiceProvider);
        // start server
        lServer.start();
    }
}
