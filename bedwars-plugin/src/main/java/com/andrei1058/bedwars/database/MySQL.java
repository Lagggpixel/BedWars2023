package com.andrei1058.bedwars.database;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.stats.PlayerStats;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.andrei1058.bedwars.BedWars.config;

@SuppressWarnings("WeakerAccess")
public class MySQL implements Database {

    private HikariDataSource dataSource;
    private String host, database, user, pass;
    private int port;
    private boolean ssl;
    private boolean certificateVerification;
    private int poolSize;
    private int maxLifetime;

    /**
     * Create new MySQL connection.
     */
    public MySQL() {
        this.host = config.getYml().getString("database.host");
        this.database = config.getYml().getString("database.database");
        this.user = config.getYml().getString("database.user");
        this.pass = config.getYml().getString("database.pass");
        this.port = config.getYml().getInt("database.port");
        this.ssl = config.getYml().getBoolean("database.ssl");
        this.certificateVerification = config.getYml().getBoolean("database.verify-certificate", true);
        this.poolSize = config.getYml().getInt("database.pool-size", 10);
        this.maxLifetime = config.getYml().getInt("database.max-lifetime", 1800);
        createPool();
    }

    /**
     * Creates the SQL connection pool.
     */
    public void createPool() {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setPoolName("BedWars1058MySQLPool");

        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setMaxLifetime(maxLifetime * 1000);

        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);

        hikariConfig.setUsername(user);
        hikariConfig.setPassword(pass);

        hikariConfig.addDataSourceProperty("useSSL", String.valueOf(ssl));
        if (!certificateVerification) {
            hikariConfig.addDataSourceProperty("verifyServerCertificate", String.valueOf(false));
        }

        hikariConfig.addDataSourceProperty("characterEncoding", "utf8");
        hikariConfig.addDataSourceProperty("encoding", "UTF-8");
        hikariConfig.addDataSourceProperty("useUnicode", "true");

        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("jdbcCompliantTruncation", "false");

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "275");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Recover if connection gets interrupted
        hikariConfig.addDataSourceProperty("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));

        dataSource = new HikariDataSource(hikariConfig);
    }

    @Override
    public boolean hasStats(UUID uuid) {
        String sql = "SELECT id FROM global_stats WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void init() {
        try(Connection connection = dataSource.getConnection()) {
            String sql = "CREATE TABLE IF NOT EXISTS global_stats (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(200), uuid VARCHAR(200), first_play TIMESTAMP NULL DEFAULT NULL, " +
                    "last_play TIMESTAMP NULL DEFAULT NULL, wins INT(200), kills INT(200), " +
                    "final_kills INT(200), looses INT(200), deaths INT(200), final_deaths INT(200), beds_destroyed INT(200), games_played INT(200));";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            sql = "CREATE TABLE IF NOT EXISTS quick_buy (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(200), " +
                    "slot_19 VARCHAR(200), slot_20 VARCHAR(200), slot_21 VARCHAR(200), slot_22 VARCHAR(200), slot_23 VARCHAR(200), slot_24 VARCHAR(200), slot_25 VARCHAR(200)," +
                    "slot_28 VARCHAR(200), slot_29 VARCHAR(200), slot_30 VARCHAR(200), slot_31 VARCHAR(200), slot_32 VARCHAR(200), slot_33 VARCHAR(200), slot_34 VARCHAR(200)," +
                    "slot_37 VARCHAR(200), slot_38 VARCHAR(200), slot_39 VARCHAR(200), slot_40 VARCHAR(200), slot_41 VARCHAR(200), slot_42 VARCHAR(200), slot_43 VARCHAR(200));";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }


            sql = "CREATE TABLE IF NOT EXISTS player_levels (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(200), " +
                    "level INT(200), xp INT(200), name VARCHAR(200) CHARACTER SET utf8, next_cost INT(200));";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            sql = "CREATE TABLE IF NOT EXISTS player_language (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(200), iso VARCHAR(200));";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveStats(PlayerStats stats) {
        String sql;
        if (hasStats(stats.getUuid())) {
            sql = "UPDATE global_stats SET first_play=?, last_play=?, wins=?, kills=?, final_kills=?, looses=?, deaths=?, final_deaths=?, beds_destroyed=?, games_played=?, name=? WHERE uuid = ?;";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, stats.getFirstPlay() != null ? Timestamp.from(stats.getFirstPlay()) : null);
                statement.setTimestamp(2, stats.getLastPlay() != null ? Timestamp.from(stats.getLastPlay()) : null);
                statement.setInt(3, stats.getWins());
                statement.setInt(4, stats.getKills());
                statement.setInt(5, stats.getFinalKills());
                statement.setInt(6, stats.getLosses());
                statement.setInt(7, stats.getDeaths());
                statement.setInt(8, stats.getFinalDeaths());
                statement.setInt(9, stats.getBedsDestroyed());
                statement.setInt(10, stats.getGamesPlayed());
                statement.setString(11, stats.getName());
                statement.setString(12, stats.getUuid().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            sql = "INSERT INTO global_stats VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, 0);
                statement.setString(2, stats.getName());
                statement.setString(3, stats.getUuid().toString());
                statement.setTimestamp(4, Timestamp.from(stats.getFirstPlay()));
                statement.setTimestamp(5, Timestamp.from(stats.getLastPlay()));
                statement.setInt(6, stats.getWins());
                statement.setInt(7, stats.getKills());
                statement.setInt(8, stats.getFinalKills());
                statement.setInt(9, stats.getLosses());
                statement.setInt(10, stats.getDeaths());
                statement.setInt(11, stats.getFinalDeaths());
                statement.setInt(12, stats.getBedsDestroyed());
                statement.setInt(13, stats.getGamesPlayed());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PlayerStats fetchStats(UUID uuid) {
        PlayerStats stats = new PlayerStats(uuid);

        String sql = "SELECT first_play, last_play, wins, kills, final_kills, looses, deaths, final_deaths, looses," +
                "deaths, final_deaths, beds_destroyed, games_played FROM global_stats WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    Timestamp firstPlay = result.getTimestamp(1);
                    Timestamp lastPlay = result.getTimestamp(2);
                    stats.setFirstPlay(firstPlay != null ? firstPlay.toInstant() : null);
                    stats.setLastPlay(lastPlay != null ? lastPlay.toInstant() : null);
                    stats.setWins(result.getInt(3));
                    stats.setKills(result.getInt(4));
                    stats.setFinalKills(result.getInt(5));
                    stats.setLosses(result.getInt(6));
                    stats.setDeaths(result.getInt(7));
                    stats.setFinalDeaths(result.getInt(8));
                    stats.setBedsDestroyed(result.getInt(9));
                    stats.setGamesPlayed(result.getInt(10));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stats;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    @Override
    public void setQuickBuySlot(UUID uuid, String shopPath, int slot) {
        String sql = "SELECT id FROM quick_buy WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    sql = "INSERT INTO quick_buy VALUES(0, ?, ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ');";
                    try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
                        statement2.setString(1, uuid.toString());
                        statement2.executeUpdate();
                    }
                }
                BedWars.debug("UPDATE SET SLOT " + slot + " identifier " + shopPath);
                sql = "UPDATE quick_buy SET slot_" + slot + " = ? WHERE uuid = ?;";
                try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
                    statement2.setString(1, shopPath);
                    statement2.setString(2, uuid.toString());
                    statement2.executeUpdate();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public String getQuickBuySlots(UUID uuid, int slot) {
        String sql = "SELECT slot_" + slot + " FROM quick_buy WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString(1);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean hasQuickBuy(UUID uuid) {
        String sql = "SELECT id FROM quick_buy WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Object[] getLevelData(UUID uuid) {
        String sql = "SELECT level, xp, name, next_cost FROM player_levels WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Object[]{
                            result.getInt(1),
                            result.getInt(2),
                            result.getString(3),
                            result.getInt(4)
                    };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Object[]{1, 0, "", 0};
    }

    @Override
    public void setLevelData(UUID uuid, int level, int xp, String displayName, int nextCost) {
        String sql = "SELECT id from player_levels WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    sql = "INSERT INTO player_levels VALUES (?, ?, ?, ?, ?, ?);";
                    try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
                        statement2.setInt(1, 0);
                        statement2.setString(2, uuid.toString());
                        statement2.setInt(3, level);
                        statement2.setInt(4, xp);
                        statement2.setString(5, displayName);
                        statement2.setInt(6, nextCost);
                        statement2.executeUpdate();
                    }
                } else {
                    if (displayName == null) {
                        sql = "UPDATE player_levels SET level=?, xp=? WHERE uuid = ?;";
                    } else {
                        sql = "UPDATE player_levels SET level=?, xp=?, name=?, next_cost=? WHERE uuid = ?;";
                    }
                    try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
                        statement2.setInt(1, level);
                        statement2.setInt(2, xp);
                        if (displayName != null) {
                            statement2.setString(3, displayName);
                            statement2.setInt(4, nextCost);
                            statement2.setString(5, uuid.toString());
                        } else {
                            statement2.setString(3, uuid.toString());
                        }
                        statement2.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLanguage(UUID uuid, String iso) {
        String sql = "SELECT iso FROM player_language WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    sql = "UPDATE player_language SET iso = ? WHERE uuid = ?;";
                    try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
                        statement2.setString(1, iso);
                        statement2.setString(2, uuid.toString());
                        statement2.executeUpdate();
                    }
                } else {
                    sql = "INSERT INTO player_language VALUES (0, ?, ?);";
                    try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
                        statement2.setString(1, uuid.toString());
                        statement2.setString(2, iso);
                        statement2.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getLanguage(UUID uuid) {
        String sql = "SELECT iso FROM player_language WHERE uuid = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Language.getDefaultLanguage().getIso();
    }
}
