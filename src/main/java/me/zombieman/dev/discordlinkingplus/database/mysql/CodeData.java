package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "codes")
public class CodeData {

    @DatabaseField(id = true)
    private String code;

    @DatabaseField(canBeNull = false)
    private String uuid;

    @DatabaseField(canBeNull = false)
    private long expiration;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
