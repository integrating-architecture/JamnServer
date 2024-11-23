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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.isa.ipc.JamnServer.JsonToolWrapper;

/**
 * <pre>
 * This class realizes a simple Web Content Provider.
 * Which is essentially a rudimentary web server 
 * with the ability to create dynamic content (see SampleWebContentApp).
 * </pre>
 */
public class JamnWebContentProvider implements JamnServer.ContentProvider {

    protected static final String LS = System.getProperty("line.separator");
    protected static Logger LOG = Logger.getLogger(JamnWebContentProvider.class.getName());
    protected static JsonToolWrapper JSON;
    // development.mode - disables e.g. caching if true
    protected static boolean DvlpMode = true;

    protected static ExtensionFactory ExtensionFactory = new ExtensionFactory();

    /**
     * Set an ExtensionInterfaceFactory to customize file providing, caching and
     * enrichment.
     */
    public static void setExtensionFactory(ExtensionFactory pFactory) {
        ExtensionFactory = pFactory;
    }

    /**
     * Set JSON externally.
     */
    public static void setJsonTool(JsonToolWrapper pTool) {
        JSON = pTool;
    }

    protected WebFileHandler fileHandler;

    /**
     */
    public JamnWebContentProvider(String pWebRoot) {
        fileHandler = new WebFileHandler(pWebRoot);
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
            LOG.severe(String.format("Internal Error GET [%s]%s%s%s%s", pPath, LS, e.toString(), LS,
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
        protected FileCache fileCache = ExtensionFactory.createFileCache();
        protected FileEnricher fileEnricher = ExtensionFactory.createFileEnricher();
        protected FileProvider fileProvider = ExtensionFactory.createFileProvider();

        public WebFileHandler(String pRoot) {
            rootPath = pRoot;
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
     * The factory to create the plugable extension objects.
     */
    public static class ExtensionFactory {
        /**
         */
        public FileProvider createFileProvider() {
            return (WebFile pWebFile) -> pWebFile.setData(Files.readAllBytes(Paths.get(pWebFile.filePath)));
        }

        /**
         */
        public FileEnricher createFileEnricher() {
            return (WebFile pFile) -> {
                // nothing to do by default
            };
        }

        /**
         */
        public FileCache createFileCache() {
            return new FileCache() {
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
        }
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
