package me.zombieman.dev.discordlinkingplus.database.mysql.data.statistics;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "link_statistics")
public class LinkStatisticsData {

    @DatabaseField(id = true)
    private String uuid;

    @DatabaseField
    private long totalLinkedTime;

    @DatabaseField
    private int timesLinked;

    @DatabaseField
    private long lastLinkedTime;

    public LinkStatisticsData() {}

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getTotalLinkedTime() { return totalLinkedTime; }
    public void setTotalLinkedTime(long totalLinkedTime) { this.totalLinkedTime = totalLinkedTime; }

    public int getTimesLinked() { return timesLinked; }
    public void setTimesLinked(int timesLinked) { this.timesLinked = timesLinked; }

    public long getLastLinkedTime() { return lastLinkedTime; }
    public void setLastLinkedTime(long lastLinkedTime) { this.lastLinkedTime = lastLinkedTime; }
}
