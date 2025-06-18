/* Authored by www.integrating-architecture.de */
package org.isa.jps;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.isa.ipc.JamnServer.JsonToolWrapper;
import org.isa.ipc.JamnWebServiceProvider.WebService;
import org.isa.ipc.JamnWebServiceProvider.WebServiceDefinitionException;

/**
 * <pre>
 * Jamn sample extension using app access to provide functionality as web services.
 * The sample provides db connection informations.
 * 
 * Installation json file e.g.: "sample.Extension.json" - in <jps-home>/extensions
 * {
 *   "binPath": "./bin/org.isa.jps.SampleExtension-0.0.1-SNAPSHOT.jar",
 *   "className" : "org.isa.jps.SampleExtension",
 *   "scope" : "app"
 * }
 * 
 * </pre>
 */
public class SampleExtension {

    // WebApi service end point names
    protected static final String WSP_get_dbconnections = "/service/get-db-connections";
    protected static final String WSP_save_dbconnections = "/service/save-db-connections";
    protected static final String WSP_delete_dbconnections = "/service/delete-db-connections";

    protected static Logger LOG = Logger.getLogger(SampleExtension.class.getName());
    protected static final String LS = System.lineSeparator();

    protected Charset standardEncoding;
    protected JsonToolWrapper jsonTool;
    protected Map<String, DbConnectionDef> connectionMap;
    protected Path extRootPath;
    protected Path dataFile;
    protected String name = "";

    protected Map<String, Object> ctx;

    /**
     * <pre>
     * Public default constructor with app context map.
     * </pre>
     */
    public SampleExtension(Map<String, Object> pCtx) throws WebServiceDefinitionException, IOException {
        ctx = pCtx;
        name = (String)ctx.getOrDefault("name", "unknown");
        initialize();
    }

    /**
     */
    protected void initialize() throws IOException, WebServiceDefinitionException  {
        JamnPersonalServerApp lApp = JamnPersonalServerApp.getInstance();
        standardEncoding = Charset.forName(lApp.getConfig().getStandardEncoding());
        jsonTool = lApp.getJsonTool();
        extRootPath = lApp.getHomePath(lApp.getConfig().getExtensionRoot());
        dataFile = Paths.get(extRootPath.toString(), lApp.getConfig().getExtensionData(), String.join("", name, ".data.json"));
        loadConnections();

        //register the webservice interface
        lApp.registerWebServices(new DbConnectionWebServiceApi());

        //register a commad to show all known db connections on the cli
        lApp.getCli().newCommandBuilder()
                .name("dbconnections")
                .descr(cmdName -> lApp.getCli().newDefaultDescr(cmdName, "", "Show all defined db connections"))
                .function(cmdCtx -> 
                     connectionMap.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> e.getKey() + " = " + e.getValue().getUrl())
                            .collect(Collectors.joining(LS))
                )
                .build();
    }

    /**
     */
    protected void loadConnections() throws IOException {
        String lData;
        connectionMap = new HashMap<>();

        if (!Files.exists(dataFile)) {
            lData = jsonTool.toString(createSampleData());
            Files.writeString(dataFile, lData, standardEncoding, StandardOpenOption.CREATE);

            LOG.info(() -> String.format("Extension [%s] default datafile created [%s]", getClass().getSimpleName(),
                    dataFile));
        }

        lData = new String(Files.readAllBytes(dataFile), standardEncoding);
        DbConnectionDef[] lDefList = jsonTool.toObject(lData, DbConnectionDef[].class);
        for (DbConnectionDef def : lDefList) {
            connectionMap.put(def.getName(), def);
        }
    }

    /**
     */
    protected List<DbConnectionDef> createSampleData() {
        List<DbConnectionDef> lDefList = new ArrayList<>();
        lDefList.add(new DbConnectionDef()
                .setName("Oracle Test-Server")
                .setType("oracle")
                .setUrl("jdbc:oracle:thin:@TS-ORA:1521/XEPDB1")
                .setUser("admin"));
        lDefList.add(new DbConnectionDef()
                .setName("MySQL Development-Server")
                .setType("mysql")
                .setUrl("jdbc:mysql://DS-MSQL:3306/DVLPDB3")
                .setUser("devel"));

        return lDefList;
    }

    /**
     * Encapsulate the webservices in a separate api class
     */
    protected class DbConnectionWebServiceApi {
        protected static final String StatusOk = "ok";

        protected DbConnectionWebServiceApi() {
            // keep instantiation internal
        }

        @WebService(path = WSP_get_dbconnections, methods = { "POST" }, contentType = APPLICATION_JSON)
        public DbConnectionResponse getDbConnections() {
            DbConnectionResponse lResponse = new DbConnectionResponse();
            lResponse.addAllConnections(connectionMap);
            return lResponse;
        }

        @WebService(path = WSP_save_dbconnections, methods = { "POST" }, contentType = APPLICATION_JSON)
        public DbConnectionResponse saveDbConnections(DbConnectionRequest pRequest) {
            DbConnectionResponse lResponse = new DbConnectionResponse();
            return lResponse.setStatusOk();
        }

        @WebService(path = WSP_delete_dbconnections, methods = { "POST" }, contentType = APPLICATION_JSON)
        public DbConnectionResponse deleteDbConnections(DbConnectionRequest pRequest) {
            DbConnectionResponse lResponse = new DbConnectionResponse();
            lResponse.setStatus(StatusOk);
            return lResponse;
        }

        /**
         * the webservice dtos
         */
        /**
         */
        public static class DbConnectionRequest {
            private List<DbConnectionDef> connections = new ArrayList<>();

            public List<DbConnectionDef> getConnections() {
                return connections;
            }
        }

        /**
         */
        public static class DbConnectionResponse {
            private List<DbConnectionDef> connections = new ArrayList<>();
            private String status = "";

            public DbConnectionResponse setStatusOk() {
                return setStatus(StatusOk);
            }

            public boolean isStatus(String pVal) {
                return status.equals(pVal);
            }

            public DbConnectionResponse setStatus(String status) {
                this.status = status;
                return this;
            }

            public DbConnectionResponse addConnection(DbConnectionDef pDef) {
                connections.add(pDef);
                return this;
            }

            public DbConnectionResponse addAllConnections(Map<String, DbConnectionDef> pConnections) {
                connections.addAll(pConnections.values());
                return this;
            }
        }
    }

    /**
     * a db connection data object
     */
    public static class DbConnectionDef {
        protected String name;
        protected String type;
        protected String url;
        protected String user;
        protected String owner;

        public String getName() {
            return name;
        }

        public DbConnectionDef setName(String name) {
            this.name = name;
            return this;
        }

        public String getType() {
            return type;
        }

        public DbConnectionDef setType(String type) {
            this.type = type;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public DbConnectionDef setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getUser() {
            return user;
        }

        public DbConnectionDef setUser(String user) {
            this.user = user;
            return this;
        }

        public String getOwner() {
            return owner;
        }

        public DbConnectionDef setOwner(String owner) {
            this.owner = owner;
            return this;
        }
    }

}
