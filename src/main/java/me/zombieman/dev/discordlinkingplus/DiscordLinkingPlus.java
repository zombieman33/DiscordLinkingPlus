package me.zombieman.dev.discordlinkingplus;

import me.zombieman.dev.discordlinkingplus.api.API;
import me.zombieman.dev.discordlinkingplus.commands.AdminCmd;
import me.zombieman.dev.discordlinkingplus.commands.ClaimRewardsCmd;
import me.zombieman.dev.discordlinkingplus.commands.LinkCmd;
import me.zombieman.dev.discordlinkingplus.commands.UnlinkCmd;
import me.zombieman.dev.discordlinkingplus.data.PlayerData;
import me.zombieman.dev.discordlinkingplus.data.cache.ServerListCache;
import me.zombieman.dev.discordlinkingplus.database.mysql.DiscordDatabase;
import me.zombieman.dev.discordlinkingplus.database.mysql.PlayerDatabase;
import me.zombieman.dev.discordlinkingplus.database.mysql.ServerDatabase;
import me.zombieman.dev.discordlinkingplus.database.mysql.statistics.LinkStatisticsDatabase;
import me.zombieman.dev.discordlinkingplus.database.redis.RedisSubscriber;
import me.zombieman.dev.discordlinkingplus.discord.*;
import me.zombieman.dev.discordlinkingplus.listeners.PlayerJoinListener;
import me.zombieman.dev.discordlinkingplus.manager.*;
import me.zombieman.dev.discordlinkingplus.placeholders.LinkPlaceholders;
import me.zombieman.dev.discordlinkingplus.utils.ServerNameUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class DiscordLinkingPlus extends JavaPlugin {
    private RankManager rankManager;
    private API api;
    private PlayerDatabase playerDatabase;
    private LinkStatisticsDatabase linkStatisticsDatabase;
    private DiscordDatabase discordDatabase;
    private CodeManager codeManager;
    private StatisticsManager statisticsManager;
    private JedisPool jedisPool;
    private Thread redisSubscriberThread;
    private JDA jda;
    private volatile boolean running = true;

    private ServerListCache serverListCache;
    private ServerDatabase serverDatabase;

    public final UUID sessionID = UUID.randomUUID();
    private LinkPlaceholders linkPlaceholders;
    private static DiscordLinkingPlus instance;
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        instance = this;

        PlayerData.initDataFolder(this);

        // Check for PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("-----------------------------------------");
            getLogger().warning("WARNING");
            getLogger().warning("PlaceholderAPI plugin is not installed!");
            getLogger().warning(this.getPluginMeta().getName() + " is now being disabled!");
            getLogger().warning("-----------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // Check for Luckperms
        if (getServer().getPluginManager().getPlugin("Luckperms") == null) {
            getLogger().warning("-----------------------------------------");
            getLogger().warning("WARNING");
            getLogger().warning("Luckperms plugin is not installed!");
            getLogger().warning(this.getPluginMeta().getName() + " is now being disabled!");
            getLogger().warning("-----------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize MySQL connection
        try {
            String url = getConfig().getString("database.mysql.url");
            String username = getConfig().getString("database.mysql.username");
            String password = getConfig().getString("database.mysql.password");
            playerDatabase = new PlayerDatabase(url, username, password);
            codeManager = new CodeManager(url, username, password);
            discordDatabase = new DiscordDatabase(url, username, password);
            linkStatisticsDatabase = new LinkStatisticsDatabase(this, url, username, password);
            serverDatabase = new ServerDatabase(this, url, username, password);
            serverListCache = new ServerListCache(serverDatabase);
            getLogger().info("Connected to MySQL database");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to MySQL database! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Redis connection
        String redisHost = getConfig().getString("database.redis.host");
        int redisPort = getConfig().getInt("database.redis.port");
        String redisPassword = getConfig().getString("database.redis.password", null);
        String redisUsername = getConfig().getString("database.redis.username", null);

        JedisPoolConfig poolConfig = new JedisPoolConfig();

        if (redisUsername != null && redisPassword != null) {
            getLogger().info("Using Redis username and password authentication.");
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisUsername, redisPassword);
        } else if (redisPassword != null) {
            getLogger().info("Using Redis password authentication.");
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);
        } else {
            getLogger().info("Connecting to Redis without password authentication.");
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
        }

        getLogger().info("Connected to Redis server");

        // Start Redis subscriber thread
        redisSubscriberThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                RedisSubscriber subscriber = new RedisSubscriber(this);
                jedis.subscribe(subscriber, "DISCORD_LINKING");
            } finally {
                running = false;
            }
        });
        redisSubscriberThread.start();

        // Initialize Discord Bot
        DiscordBot.getInstance(getConfig().getString("DiscordBotToken"), getConfig().getString("ServerName", "n/a"))
                .addEventListener(
                        new DiscordListener(this),
                        new DiscordBanListener(this),
                        new DiscordTimeoutListener(this),
                        new BoostListener(this),
                        new DiscordLeave(this));

        // Listeners
//        DiscordBot.getBot().addEventListener(new DiscordBanListener(this));
//        DiscordBot.getBot().addEventListener(new DiscordTimeoutListener(this));

        getCommand("claimrewards").setExecutor(new ClaimRewardsCmd(this));
        getCommand("link").setExecutor(new LinkCmd(this));
        getCommand("unlink").setExecutor(new UnlinkCmd(this));
        getCommand("discordadmin").setExecutor(new AdminCmd(this));

        rankManager = new RankManager(this, DiscordBot.getBot());
        statisticsManager = new StatisticsManager(this);

        new PlayerJoinListener(this);

        if (getGuild() == null) {
            getLogger().severe("-----------------------------------------");
            getLogger().severe("ERROR");
            getLogger().severe("The guild is null!");
            getLogger().severe(this.getPluginMeta().getName() + " is now being disabled!");
            getLogger().severe("-----------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        new LoggingManager(this);
        new ServerNameUtil();
        new LuckpermsManager(this);

        initializeAPI();

//        api = new API(this);

        // Placeholders
        linkPlaceholders = new LinkPlaceholders(this);
        linkPlaceholders.register();

        scheduleLinkReminder();

        codeManager.startExpiredCodeCleanup(this);
    }

    @Override
    public void onDisable() {

        if (linkPlaceholders != null) {
            linkPlaceholders.unregister();
            getLogger().info("Placeholders unregistered.");
        }

        running = false;
        if (redisSubscriberThread != null && redisSubscriberThread.isAlive()) {
            redisSubscriberThread.interrupt();
            try {
                redisSubscriberThread.join(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (jedisPool != null) {
            jedisPool.close();
            getLogger().info("Redis connection pool closed.");
        }

        if (playerDatabase != null) {
            playerDatabase.close();
            getLogger().info("MySQL database connection closed.");
        }
        if (codeManager != null) {
            codeManager.close();
            getLogger().info("MySQL code database connection closed.");
        }
        if (discordDatabase != null) {
            discordDatabase.close();
            getLogger().info("MySQL discord database connection closed.");
        }
        if (linkStatisticsDatabase != null) {
            linkStatisticsDatabase.close();
            getLogger().info("MySQL link discord statistics database connection closed.");
        }

        // Plugin shutdown logic
        if (DiscordBot.getBot() != null) {
            DiscordBot.shutdown();
        }
    }

    public void scheduleLinkReminder() {

        DiscordLinkingPlus plugin = this;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {

                    try {
                        if (getPlayerDatabase().getPlayerData(player.getUniqueId()).isLinked()) continue;

                        if (!player.hasPermission("discordlinkingplus.command.link")) continue;

                        sendReminder(player);

                    } catch (SQLException e) {
                        plugin.getLogger().severe("Database not responding: " + e.getMessage());
                        return;
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L * 60L * 40);
    }

    public void sendReminder(Player player) {

        player.sendMessage(MiniMessage.miniMessage().deserialize("""
                            <green><strikethrough>                                           </strikethrough>
                            <green>Remember to link your account!
                            <green>/link (CLICK ME)
                            <green><strikethrough>                                           </strikethrough>""")
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Click to link accounts")))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/link")));


        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    public PlayerDatabase getPlayerDatabase() {
        return playerDatabase;
    }
    public DiscordDatabase getDiscordDatabase() {
        return discordDatabase;
    }
    public LinkStatisticsDatabase getLinkStatisticsDatabase() {
        return linkStatisticsDatabase;
    }
    public Jedis getJedisResource() {
        return jedisPool.getResource();
    }
    public RankManager getRankManager() {
        return this.rankManager;
    }
    public StatisticsManager getStatisticsManager() {
        return this.statisticsManager;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean canUnLink() {
        return getConfig().getBoolean("UnLink.allowed");
    }
    public Guild getGuild() {
        String discordServerToken = getConfig().getString("DiscordServerToken");

        if (discordServerToken == null) return null;

        return DiscordBot.getBot().getGuildById(discordServerToken);
    }
    public String guildID() {
        return getConfig().getString("DiscordServerToken");
    }
    public boolean isMainServer() {
        return getConfig().getBoolean("MainServer", true);
    }
    public API getApi() {
        if (api == null) {
            System.err.println("API is not initialized!");
            return null;
        }
        return api;
    }
    private void initializeAPI() {
        if (api != null) {
            getLogger().warning("API is already initialized!");
            return;
        }
        api = new API(this);
        getLogger().info("API initialized successfully.");
    }
    public static DiscordLinkingPlus getInstance() {
        return instance;
    }

    public ServerDatabase getServerDatabase() {
        return serverDatabase;
    }

    public ServerListCache getServerListCache() {
        return serverListCache;
    }
}
