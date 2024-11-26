/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import java.util.logging.Logger;

import org.isa.ipc.sample.data.ServerInfo;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The value provider is the end point interface of the sample server side
 * templating and preprocessing mechanism.
 */
public class SampleFileEnricherValueProvider implements Helper.ExprString.ValueProvider {

    protected static final String LS = System.getProperty("line.separator");
    protected static Logger LOG = Logger.getLogger(SampleFileEnricherValueProvider.class.getName());
    protected static ObjectMapper JSON = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    @Override
    public String getValue(String pKey, Object pCtx) {
        try {
            if ("app.title".equals(pKey)) {
                return "JamnWeb Sample";
            }
            if ("server.info".equals(pKey)) {
                return JSON.writeValueAsString(new ServerInfo().setName("JamnServer").setVersion("0.0.1-SNAPSHOT")
                        .setDescription("Just another micro node Server"));
            }
        } catch (Exception e) {
            LOG.severe(() -> String.format("ERROR retrieving template value for: [%s] in context [%s] %s%s%s", pKey,
                    pCtx, e.getMessage(), LS,
                    Helper.getStackTraceFrom(e)));
        }

        LOG.fine(() -> String.format("No template value for: [%s] in context [%s]", pKey, pCtx));
        return "";
    }

}
