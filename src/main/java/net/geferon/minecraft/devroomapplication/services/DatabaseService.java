package net.geferon.minecraft.devroomapplication.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.geferon.minecraft.devroomapplication.DevRoomApplicationPlugin;
import org.bukkit.configuration.Configuration;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

@Singleton
public class DatabaseService {
    private final DevRoomApplicationPlugin plugin;
    private final Configuration config;
    private final DataSource source;

    @Inject
    public DatabaseService(DevRoomApplicationPlugin plugin, Configuration config, DataSource source) {
        this.plugin = plugin;
        this.config = config;
        this.source = source;
    }

//    private Connection activeConnection;

    public Connection getConnection() throws SQLException {
//        if (activeConnection == null || activeConnection.isClosed()) {
//            connect();
//        }
//
//        return activeConnection;
        return source.getConnection();
    }

//    private void connect() throws SQLException {
//        activeConnection = DriverManager.getConnection(
//            "jdbc:mysql://%s:%d/%s".formatted(
//                    config.getString("database-host"),
//                    config.getInt("database-port"),
//                    config.getString("database-db")
//            ),
//            config.getString("database-username"),
//            config.getString("database-password")
//        );
//    }

    public void initialize() {
        try {
            var con = getConnection();

            var sql = new String(plugin.getResource("init.sql").readAllBytes(), "UTF8");
            var createTables = con.prepareStatement(sql);
            createTables.execute();
            con.close();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void dispose() {
//        try {
//            if (activeConnection != null && !activeConnection.isClosed()) activeConnection.close();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
        if (source instanceof HikariDataSource hik) {
            hik.close();
        }
    }

    public static DataSource getDataSource(Configuration config) {
        try {
            var dbDataSource = new MariaDbDataSource();
            dbDataSource.setUrl("jdbc:mariadb://%s:%d/%s".formatted(
                    config.getString("database-host"),
                    config.getInt("database-port"),
                    config.getString("database-db")
            ));
            dbDataSource.setUser(config.getString("database-user"));
            dbDataSource.setPassword(config.getString("database-password"));

            var tempConnector = dbDataSource.getConnection();
            var query = tempConnector.prepareStatement("SELECT 1 + 1");
            query.execute();
            tempConnector.close();

            var dbConfig = new HikariConfig();
            dbConfig.setDataSource(dbDataSource);
            var ds = new HikariDataSource(dbConfig);

            return ds;
        } catch (SQLException | HikariPool.PoolInitializationException e) {
            // Do nothing
        }
        return null;
    }
}
