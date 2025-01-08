/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import static org.isa.ipc.JamnServer.HttpHeader.FieldValue.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;

import org.isa.ipc.JamnWebServiceProvider.WebService;

/**
 * 
 */
public class DefaultWebServices {

    protected SystemInterface sysIFace;

    /**
     */
    public DefaultWebServices(SystemInterface pComp) {
        sysIFace = pComp;
    }

    /**
     */
    @WebService(methods = { "POST" }, path = ShellRequest.Path, contentType = ShellRequest.ContentType)
    public ShellResponse runShellCommand(ShellRequest pRequest) {
        ShellResponse lResponse = new ShellResponse();
        String[] lCommand = pRequest.command.toArray(new String[0]);

        List<String> lResult = sysIFace.functions().shellCmd(lCommand, pRequest.workingDir, false);

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

}
