/* Authored by www.integrating-architecture.de */
package org.isa.ipc;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.IMAGE;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.IMAGE_X_ICON;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_CSS;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_HTML;
import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.TEXT_JS;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_200_OK;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_204_NO_CONTENT;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_404_NOT_FOUND;
import static org.isa.ipc.JamnServer.HttpHeader.Status.SC_500_INTERNAL_ERROR;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.isa.ipc.JamnServer.Config;
import org.isa.ipc.JamnServer.JsonToolWrapper;
import org.isa.ipc.JamnWebContentProvider.ExprString.ValueProvider;

/**
 * <pre>
 * This class realizes a simple Web Content Provider.
 * Which is essentially a rudimentary web server 
 * with the ability to create dynamic content (see SampleWebContentApp).
 * </pre>
 */
public class JamnWebContentProvider implements JamnServer.ContentProvider {

    protected static final String LS = System.lineSeparator();
    protected static Logger LOG = Logger.getLogger(JamnWebContentProvider.class.getName());
    // development.mode - disables e.g. caching if true
    protected static boolean DvlpMode = true;

    protected JsonToolWrapper jsonTool;

    protected Config config = new Config();
    protected WebFileHandler fileHandler;
    protected String webroot;

    /**
     * <pre>
     * The content provider supports two interfaces to customize content.
     * A fileProvider and a fileEnricher. 
     * 
     * The fileProvider provides the actual byte content of what is called a WebFile.
     * This can be real files from an underlying filesystem (default)
     * or anything else a user wants to be associated with a resource name.
     * </pre>
     */
    protected FileProvider fileProvider = (WebFile pFile) -> pFile
            .setData(Files.readAllBytes(Paths.get(pFile.filePath)));

    /**
     * <pre>
     * The fileEnricher is an interface used by the fileProvider
     * to modify the file/resource content before provision.
     * By default files can be marked as templates with placeholders like ${name} that become resolved.
     * </pre>
     */
    protected FileEnricher fileEnricher = (WebFile pFile) -> {
    };

    protected FileCache fileCache = new FileCache() {
        private Map<String, WebFile> cacheMap = Collections.synchronizedMap(new HashMap<>());

        @Override
        public synchronized void put(String pKey, WebFile pFile) {
            cacheMap.put(pKey, pFile);
        }

        @Override
        public boolean contains(String pKey) {
            return cacheMap.containsKey(pKey);
        }

        @Override
        public WebFile get(String pKey) {
            return cacheMap.get(pKey);
        }
    };

    private JamnWebContentProvider() {
    }

    private JamnWebContentProvider(String pRoot) {
        webroot = pRoot;
    }

    /**
     */
    public static JamnWebContentProvider Builder(String pWebRoot) {
        JamnWebContentProvider lProvider = new JamnWebContentProvider(pWebRoot);
        return lProvider;
    }
    
    /**
     */
    public JamnWebContentProvider setJsonTool(JsonToolWrapper pTool) {
        jsonTool = pTool;
        return this;
    }

    /**
     */
    public JamnWebContentProvider setConfig(Config pConfig) {
        config = pConfig;
        return this;
    }

    /**
     */
    public JamnWebContentProvider setFileEnricher(FileEnricher pFileEnricher) {
        this.fileEnricher = pFileEnricher;
        return this;
    }

    /**
     */
    public JamnWebContentProvider setFileCache(FileCache fileCache) {
        this.fileCache = fileCache;
        return this;
    }

    /**
    */
    public JamnWebContentProvider setFileProvider(FileProvider fileProvider) {
        this.fileProvider = fileProvider;
        return this;
    }

    /**
     */
    public JamnWebContentProvider build() {
        fileHandler = new WebFileHandler(webroot, fileCache, fileEnricher, fileProvider);
        return this;
    }

    /**
     * JamnServer.ContentProvider Interface method.
     */
    @Override
    public String createResponseContent(Map<String, String> pResponseAttributes, OutputStream pResponseContent,
            String pMethod, String pPath, String pRequestBody, Map<String, String> pRequestAttributes) {

        // be gently
        String lStatus = SC_200_OK;
        RequestHeader lRequestHeader = new RequestHeader(pRequestAttributes);
        ResponseHeader lResponseHeader = new ResponseHeader(pResponseAttributes);

        WebFile lContent;
        try {
            if (lRequestHeader.isGET()) {
                lContent = fileHandler.getFileContent(lRequestHeader, lResponseHeader);

                if (!lContent.isEmpty()) {
                    pResponseContent.write(lContent.getData());
                } else {
                    lStatus = SC_204_NO_CONTENT;
                }
            }
        } catch (WebContentException ce) {
            LOG.fine(() -> String.format("WebContent Error: [%s]%s%s", ce.getMessage(), LS, getStackTraceFrom(ce)));
            lStatus = ce.getHttpStatus();
        } catch (Exception e) {
            LOG.severe(String.format("Internal Error GET [%s]%s%s%s%s", pPath, LS, e, LS,
                    getStackTraceFrom(e)));
            lStatus = SC_500_INTERNAL_ERROR;
        }

        return lStatus;
    }

    /*********************************************************
     * Internal Provider classes.
     *********************************************************/
    /**
     */
    private static class WebFileHandler {
        protected String rootPath = "";
        protected FileCache fileCache;
        protected FileEnricher fileEnricher;
        protected FileProvider fileProvider;

        public WebFileHandler(String pRoot, FileCache pFileCache, FileEnricher pFileEnricher,
                FileProvider pFileProvider) {
            rootPath = pRoot;
            fileCache = pFileCache;
            fileEnricher = pFileEnricher;
            fileProvider = pFileProvider;
        }

        /**
         */
        public WebFile getFileContent(RequestHeader pRequest, ResponseHeader lResponse) throws WebContentException {
            WebFile lWebFile = new WebFile(pRequest.getPath());

            if (!DvlpMode && fileCache.contains(lWebFile.getId())) {
                lWebFile = fileCache.get(lWebFile.getId());
                lResponse.setContentType(lWebFile.getContentType());
                return lWebFile;
            }

            if (pRequest.isIndexRequest()) {
                // normalize to index.html name
                lWebFile.filePath = getFilePathFor("/index.html");
            } else {
                lWebFile.filePath = getFilePathFor(lWebFile.requestPath);
            }

            try {
                // by default we assume html
                lResponse.setContentType(TEXT_HTML);
                if (pRequest.isStyleSheetRequest()) {
                    lResponse.setContentType(TEXT_CSS);
                } else if (pRequest.isJavaScriptRequest()) {
                    lResponse.setContentType(TEXT_JS);
                } else if (pRequest.isImageRequest()) {
                    lResponse.setContentType(getImageTypeFrom(lWebFile.filePath));
                    lWebFile.setTextFormat(false);
                }

                lWebFile.setContentType(lResponse.getContentType());
                fileProvider.readAllFileBytes(lWebFile);
                fileEnricher.enrich(lWebFile);
                fileCache.put(lWebFile.requestPath, lWebFile);

            } catch (Exception e) {
                throw new WebContentException(SC_404_NOT_FOUND,
                        String.format("Could NOT read file data [%s]", lWebFile.filePath), e);
            }

            return lWebFile;
        }

        /**
         */
        public String getFilePathFor(String pRequestPath) {
            return new StringBuilder(rootPath).append(pRequestPath).toString();
        }

        /**
         */
        public String getImageTypeFrom(String pPath) {
            return pPath.endsWith("/favicon.ico") ? IMAGE_X_ICON
                    : IMAGE + pPath.substring(pPath.lastIndexOf(".") + 1, pPath.length());
        }

    }

    /*********************************************************
     * Plugable extension interfaces, classes and factory.
     *********************************************************/
    /**
     * A file provider delivers the concrete file content as a byte array. This
     * might be a filesystem file, or a database record or what ever.
     */
    public static interface FileProvider {
        void readAllFileBytes(WebFile pWebFile) throws Exception;
    }

    /**
     * A file enricher is used to pre process loaded web files to dynamically inject
     * values, text or code.
     */
    public static interface FileEnricher {
        void enrich(WebFile pFile) throws Exception;
    }

    /**
     * File cache abstraction.
     */
    public static interface FileCache {
        void put(String pKey, WebFile pFile);

        boolean contains(String pKey);

        WebFile get(String pKey);
    }

    /**
     */
    public static class WebFile {
        protected String requestPath = "";
        protected String filePath = "";
        protected String contentType = "";
        protected byte[] data = new byte[0];
        protected boolean isTextFormat = true;

        public WebFile(String pPath) {
            requestPath = pPath;
        }

        public String getId() {
            return requestPath;
        }

        public boolean isEmpty() {
            return data.length == 0;
        }

        public String toString() {
            return requestPath;
        }

        public String getRequestPath() {
            return requestPath;
        }

        public void setRequestPath(String requestPath) {
            this.requestPath = requestPath;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public boolean isTextFormat() {
            return isTextFormat;
        }

        public void setTextFormat(boolean isTextFormat) {
            this.isTextFormat = isTextFormat;
        }
    }

    /**
     * Internal Exceptions thrown by this WebContentProvider
     */
    private static class WebContentException extends Exception {
        private static final long serialVersionUID = 1L;
        protected final String httpStatus;

        public WebContentException(String pHttpStatus, String pMsg, Throwable pCause) {
            super(pMsg, pCause);
            httpStatus = pHttpStatus;
        }

        public String getHttpStatus() {
            return httpStatus;
        }
    }

    /**
     * <pre>
     * The FileEnricher is the interface used by a FileProvider to preprocess requested files.
     * 
     * This Default enricher first looks for a TemplateMarker at the head/top of the file.
     * If such a marker is present an ExprString is used that calls the a ValueProvider
     * for all expressions like ${valuekey}.
     * </pre>
     */
    public static class DefaultFileEnricher implements FileEnricher {

        // the marker is expected as a file type comment e.g. like
        // <!--jamn.web.template-->, /*jamn.web.template*/
        // where the comment characters are NOT searched or parsed
        protected static String TemplateMarker = "jamn.web.template";
        protected static int MarkLen = TemplateMarker.length() + 10;// marklen + a surcharge for comment chars
        protected static Charset Encoding = Charset.forName("UTF-8");

        // the ValueProvider for expressions like "${valuekey}"
        protected ValueProvider lValueProvider = null;

        protected DefaultFileEnricher() {
        }

        public DefaultFileEnricher(ValueProvider pProvider) {
            lValueProvider = pProvider;
        }

        @Override
        public void enrich(WebFile pFile) throws Exception {
            String lContent = "";
            // only process if file has text format and a TEMPLATE_MARKER
            if (pFile.isTextFormat() && hasTemplateMarker(pFile)) {
                lContent = new String(pFile.getData(), Encoding);
                lContent = new ExprString(lContent, lValueProvider).build(pFile);
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
            return lHead.contains(TemplateMarker);
        }
    }

    /**
     * <pre>
     * A simple class implementing template strings that include variable expressions.
     *
     * e.g. new ExprString("Hello ${visitor} I'am ${me}")
     *          .put("visitor", "John")
     *          .put("me", "Andreas")
     *          .build();
     * results in: "Hello John I'am Andreas"
     * </pre>
     */
    public static class ExprString {
        protected static String PatternStart = "${";
        protected static String PatternEnd = "}";
        protected static Pattern ExprPattern = Pattern.compile("\\$\\{(\\w.+)\\}");

        protected String template = "";
        protected Map<String, String> valueMap = new HashMap<>();
        protected ValueProvider provider = (String pKey, Object pCtx) -> valueMap.getOrDefault(pKey, "");

        /**
         */
        protected ExprString() {
        }

        /**
         */
        public ExprString(String pTemplate) {
            this();
            template = pTemplate;
        }

        /**
         */
        public ExprString(String pTemplate, ValueProvider pProvider) {
            this(pTemplate);
            provider = pProvider;
        }

        /**
         */
        @Override
        public String toString() {
            return template;
        }

        /**
         */
        public ExprString put(String pKey, String pValue) {
            valueMap.put(pKey, pValue);
            return this;
        }

        /**
         */
        public String build() {
            return build(null);
        }

        /**
         */
        public String build(Object pCtx) {
            StringBuilder lResult = new StringBuilder();
            String lPart = "";
            String lName = "";
            String lValue = "";
            Matcher lMatcher = ExprPattern.matcher(template);

            int lCurrentPos = 0;
            while (lMatcher.find()) {
                lPart = template.substring(lCurrentPos, lMatcher.start());
                lName = lMatcher.group().replace(PatternStart, "").replace(PatternEnd, "");
                lValue = provider.getValueFor(lName, pCtx);
                lResult.append(lPart).append(lValue);
                lCurrentPos = lMatcher.end();
            }
            if (lCurrentPos < template.length()) {
                lPart = template.substring(lCurrentPos, template.length());
                lResult.append(lPart);
            }
            return lResult.toString();
        }

        /**
         */
        public static interface ValueProvider {
            String getValueFor(String pKey, Object pCtx);
        }
    }

    /*********************************************************
     *********************************************************/

    /**
     * Internal Request HttpHeader.
     */
    private static class RequestHeader extends JamnServer.HttpHeader {
        protected String decodedPath = "";

        public RequestHeader(Map<String, String> pAttributes) {
            super(pAttributes);
            decodeRequestPath();
        }

        protected void decodeRequestPath() {
            // if request has to be decoded
            String lPath = super.getPath();
            decodedPath = lPath;
        }

        /**
         */
        @Override
        public String getPath() {
            return decodedPath;
        }

        public boolean isIndexRequest() {
            String lPath = getPath();
            return (lPath.equals("/") || lPath.equals("/index") || lPath.equals("/index.html")
                    || lPath.equals("/index.htm"));
        }

        public boolean isStyleSheetRequest() {
            String lPath = getPath();
            return (lPath.endsWith(".css"));
        }

        public boolean isJavaScriptRequest() {
            String lPath = getPath();
            return (lPath.endsWith(".js") || lPath.endsWith(".mjs"));
        }

        public boolean isImageRequest() {
            String lPath = getPath();
            return (lPath.endsWith(".png") || lPath.endsWith(".jpg") || lPath.endsWith(".gif")
                    || lPath.endsWith(".ico"));
        }
    }

    /**
     * Internal Response HttpHeader.
     */
    private static class ResponseHeader extends JamnServer.HttpHeader {

        public ResponseHeader(Map<String, String> pAttributes) {
            super(pAttributes);
        }
    }

    /*********************************************************
     * Internal static helper methods.
     *********************************************************/
    /**
     */
    protected static String getStackTraceFrom(Throwable t) {
        return JamnServer.getStackTraceFrom(t);
    }
}
