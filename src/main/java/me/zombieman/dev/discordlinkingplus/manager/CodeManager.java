package me.zombieman.dev.discordlinkingplus.manager;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.CodeData;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CodeManager {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNPQRSTUVXYZ123456789";
    private static final Random random = new Random();

    private static Dao<CodeData, String> codeDao;
    private final ConnectionSource connectionSource;

    public CodeManager(String jdbcUrl, String username, String password) throws SQLException {
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, CodeData.class);
        codeDao = DaoManager.createDao(connectionSource, CodeData.class);
        System.out.println("Database connection established for CodeManager.");
    }

    public void close() {
        try {
            if (connectionSource != null && connectionSource.isOpen("default")) {
                connectionSource.close();
                System.out.println("Database connection closed.");
            }
        } catch (Exception e) {
            System.out.println("Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String createCode(UUID uuid, int expirationTimeMinutes) throws SQLException {
        String code;
        do {
            code = generateRandomCode();
        } while (codeDao.queryForId(code) != null);

        CodeData codeData = new CodeData();
        codeData.setCode(code);
        codeData.setUuid(uuid.toString());
        codeData.setExpiration(System.currentTimeMillis() + (expirationTimeMinutes * 60 * 1000L));

        codeDao.create(codeData);
        return code;
    }

    public static boolean checkCode(String code) throws SQLException {
        CodeData codeData = codeDao.queryForId(code);
        if (codeData != null && codeData.getExpiration() > System.currentTimeMillis()) {
            return true;
        } else if (codeData != null) {
            codeDao.deleteById(code);
        }
        return false;
    }

    public static void removeCode(String code) throws SQLException {
        codeDao.deleteById(code);
    }

    public static UUID getPlayerWithCode(String code) throws SQLException {
        CodeData codeData = codeDao.queryForId(code);
        if (codeData != null) {
            return UUID.fromString(codeData.getUuid());
        }
        return null;
    }

    private static String generateRandomCode() {
        StringBuilder codeBuilder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(CHARACTERS.length());
            codeBuilder.append(CHARACTERS.charAt(index));
        }
        return codeBuilder.toString();
    }

    public void startExpiredCodeCleanup(DiscordLinkingPlus plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                long currentTime = System.currentTimeMillis();
                List<CodeData> allCodes = codeDao.queryForAll();

                for (CodeData codeData : allCodes) {
                    if (codeData.getExpiration() <= currentTime) {
                        codeDao.delete(codeData);
                        System.out.println("Removed expired code: " + codeData.getCode());
                    }
                }
            } catch (SQLException e) {
                System.err.println("Failed to clean up expired codes: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0L, TimeUnit.MINUTES.toSeconds(20) * 20L);
    }
}