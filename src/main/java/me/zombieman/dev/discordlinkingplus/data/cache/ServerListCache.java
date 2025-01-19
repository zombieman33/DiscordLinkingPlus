package me.zombieman.dev.discordlinkingplus.data.cache;


import me.zombieman.dev.discordlinkingplus.database.mysql.ServerDatabase;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.ServerData;

import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerListCache {

    private final ServerDatabase dataSource;
    private ConcurrentHashMap<UUID, ServerData> servers = new ConcurrentHashMap<>();

    public ServerListCache(ServerDatabase dataSource) {
        this.dataSource = dataSource;
    }

    public Collection<ServerData> fetchServers() {
        try {
            this.servers.clear();
            this.dataSource.getData().forEach(serverData -> servers.put(UUID.fromString(serverData.getUuid()), serverData));
            return this.servers.values();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Collection<ServerData> getServers() {
        return servers.values();
    }
}
