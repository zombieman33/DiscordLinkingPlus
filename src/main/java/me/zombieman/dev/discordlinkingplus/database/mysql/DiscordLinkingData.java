package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "discord_linking")
public class DiscordLinkingData {

    @DatabaseField(id = true)
    private String uuid;

    @DatabaseField(canBeNull = false)
    private String username;

    @DatabaseField
    private String discordTag;

    @DatabaseField
    private String serverClaimedOn;

    @DatabaseField
    private boolean isLinked;

    @DatabaseField
    private boolean hasLinked;

    public DiscordLinkingData() {}

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDiscordTag() { return discordTag; }
    public void setDiscordTag(String discordTag) { this.discordTag = discordTag; }

    public String getServerClaimedOn() { return serverClaimedOn; }
    public void setServerClaimedOn(String serverClaimedOn) { this.serverClaimedOn = serverClaimedOn; }

    public boolean isLinked() { return isLinked; }
    public void setLinked(boolean linked) { isLinked = linked; }

    public boolean hasLinked() { return hasLinked; }
    public void setHasLinked(boolean hasLinked) { this.hasLinked = hasLinked; }
}
