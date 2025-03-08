/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.isa.ipc.JamnWebServiceProvider.WebService;
import org.isa.jps.JamnPersonalServerApp;

/**
 * <pre>
 * A few default web services. The services use request/response dtos
 * you can do it that way - but it is not mandatory (see WebServiceProvider).
 * 
 * Defining path and contentType as static constants of dtos 
 * is also just a syntactic gimmick - relevant is the annotation @WebService
 * </pre>
 */
public class DefaultWebServices {

    protected OperatingSystemInterface osIFace;

    /**
     */
    protected DefaultWebServices() {
    }

    /**
     */
    public DefaultWebServices(OperatingSystemInterface pComp) {
        osIFace = pComp;
    }

    /**********************************************************************************
     **********************************************************************************/
    /**
     */
    @WebService(methods = { "POST" }, path = ShellRequest.Path, contentType = ShellRequest.ContentType)
    public ShellResponse runShellCommand(ShellRequest pRequest) {
        ShellResponse lResponse = new ShellResponse();
        String[] lCommand = pRequest.command.toArray(new String[0]);

        List<String> lResult = osIFace.fnc().shellCmd(lCommand, pRequest.workingDir, false, null);

        lResponse.output.addAll(lResult);
        return lResponse;
    }

    /**
     * {"command":[], "workingDir":""}
     */
    public static class ShellRequest {
        public static final String Path = "/api/shell";
        public static final String ContentType = APPLICATION_JSON;

        protected String workingDir = "";
        protected List<String> command = new ArrayList<>();

        public ShellRequest() {
        }

        public ShellRequest(String pCmd) {
            command.add(pCmd);
        }

        public ShellRequest add(String pPart) {
            command.add(pPart);
            return this;
        }

        public String getWorkingDir() {
            return workingDir;
        }

        public ShellRequest setWorkingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }
    }

    /**
     * {"output":[]}
     */
    public static class ShellResponse {
        protected List<String> output = new ArrayList<>();

        public List<String> getOutput() {
            return output;
        }
    }

    /**********************************************************************************
     **********************************************************************************/
    /**
     */
    @WebService(methods = { "POST" }, path = SystemInfoRequest.Path, contentType = SystemInfoRequest.ContentType)
    public SystemInfoResponse getSystemInfo(SystemInfoRequest pRequest) {
        Properties lBuildProps = JamnPersonalServerApp.getInstance().getConfig().getBuildProperties();
        return new SystemInfoResponse()
                .setName(lBuildProps.getProperty("appname"))
                .setVersion(lBuildProps.getProperty("version"))
                .setDescription(lBuildProps.getProperty("description"))
                .addLink("app.scm", lBuildProps.getProperty("url"))
                .setConfig(JamnPersonalServerApp.getInstance().getConfig().getProperties());
    }

    /**
     */
    public static class SystemInfoRequest {
        public static final String Path = "/api/system-infos";
        public static final String ContentType = APPLICATION_JSON;

        protected SystemInfoRequest() {
        }
    }

    /**
     */
    public static class SystemInfoResponse {
        protected String name = "";
        protected String version = "";
        protected String description = "";

        protected Map<String, String> links = new HashMap<>();
        protected Properties config = new Properties();

        public SystemInfoResponse setConfig(Properties pProps) {
            this.config.putAll(pProps);
            return this;
        }

        public Map<String, String> getLinks() {
            return links;
        }

        public SystemInfoResponse addLink(String pKey, String pValue) {
            links.put(pKey, pValue);
            return this;
        }

        public String getName() {
            return name;
        }

        public SystemInfoResponse setName(String name) {
            this.name = name;
            return this;
        }

        public String getVersion() {
            return version;
        }

        public SystemInfoResponse setVersion(String version) {
            this.version = version;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public SystemInfoResponse setDescription(String description) {
            this.description = description;
            return this;
        }
    }
    /**********************************************************************************
     **********************************************************************************/

}
