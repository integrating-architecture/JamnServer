/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import org.isa.ipc.JamnWebContentProvider.ExprString.ValueProvider;
import org.isa.ipc.JamnWebContentProvider.WebFile;

/**
 * 
 */
public class DefaultFileEnricherValueProvider implements ValueProvider {

    @Override
    public String getValueFor(String pKey, Object pCtx) {
        WebFile lWebFile = (WebFile) pCtx;
        return "";
    }

}
