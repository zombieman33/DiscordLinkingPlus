package me.zombieman.dev.discordlinkingplus.database.mysql.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "server_data")
public class ServerData {

    @DatabaseField(id = true)
    private String uuid;

    @DatabaseField
    private String serverID;

    @DatabaseField
    private long lastUpdated;

    public ServerData() {}

    // Getters and Setters
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}
