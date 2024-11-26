/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample.data;

/**
 * 
 */
public class ServerInfo {

    protected String name = "";
    protected String version = "";
    protected String description = "";

    public String getName() {
        return name;
    }

    public ServerInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public ServerInfo setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ServerInfo setDescription(String description) {
        this.description = description;
        return this;
    }
}
