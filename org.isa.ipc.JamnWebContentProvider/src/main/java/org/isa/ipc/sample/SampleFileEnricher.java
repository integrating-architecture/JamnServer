/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import java.nio.charset.Charset;

import org.isa.ipc.JamnWebContentProvider.FileEnricher;
import org.isa.ipc.JamnWebContentProvider.WebFile;

/**
 * <pre>
 * The FileEnricher is the interface used by a FileProvider to preprocess requested files.
 * 
 * This sample enricher first looks for a TEMPLATE_MARKER at the head/top of the file.
 * If such a marker is present an ExprString is used that calls the sample ValueProvider
 * for all expressions like ${sometext}.
 * </pre>
 */
public class SampleFileEnricher implements FileEnricher {

    // the marker is expected as a file type comment e.g. like
    // <!--jamn.web.template-->, /*jamn.web.template*/
    // where the comment characters are NOT searched or parsed
    private static final String TEMPLATE_MARKER = "jamn.web.template";
    private static int MarkLen = TEMPLATE_MARKER.length() + 10;// marklen + a surcharge for comment chars
    private static Charset Encoding = Charset.forName("UTF-8");

    // the sample ValueProvider for expressions like "${sometext}"
    protected SampleFileEnricherValueProvider lValueProvider = new SampleFileEnricherValueProvider();

    @Override
    public void enrich(WebFile pFile) throws Exception {
        String lContent = "";
        // only process if file has text format and a TEMPLATE_MARKER
        if (pFile.isTextFormat() && hasTemplateMarker(pFile)) {
            lContent = new String(pFile.getData(), Encoding);
            lContent = new Helper.ExprString(lContent, lValueProvider).build(pFile);
            pFile.setData(lContent.getBytes(Encoding));
        }
    }

    /**
     * Read the first MarkLen bytes of a file and ckeck for the template marker.
     */
    protected boolean hasTemplateMarker(WebFile pFile) {
        String lHead = "";
        byte[] lBuffer = new byte[MarkLen];
        if (pFile.getData() != null && pFile.getData().length > MarkLen) {
            System.arraycopy(pFile.getData(), 0, lBuffer, 0, MarkLen);
            lHead = new String(lBuffer, Encoding);
        }
        return lHead.contains(TEMPLATE_MARKER);
    }
}
