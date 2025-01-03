package me.zombieman.dev.discordlinkingplus.database.mysql.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

@DatabaseTable(tableName = "discord_data")
public class DiscordData {

    @DatabaseField(id = true)
    private String discordID;
    @DatabaseField
    private Timestamp timeoutEnd;

    public DiscordData() {}

    public void setTimeoutEnd(OffsetDateTime timeoutEnd) {
        this.timeoutEnd = (timeoutEnd != null) ? Timestamp.from(timeoutEnd.toInstant()) : null;
    }

    public OffsetDateTime getTimeoutEnd() {
        return (timeoutEnd != null) ? OffsetDateTime.ofInstant(timeoutEnd.toInstant(), java.time.ZoneOffset.UTC) : null;
    }

    public String getDiscordID() {
        return discordID;
    }

    public void setDiscordID(String discordID) {
        this.discordID = discordID;
    }
}
