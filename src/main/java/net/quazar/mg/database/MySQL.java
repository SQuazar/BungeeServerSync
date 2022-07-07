package net.quazar.mg.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class MySQL implements Database {

    private final HikariDataSource dataSource;

    public MySQL(DatabaseConfiguration configuration) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                configuration.getHost(),
                configuration.getPort(),
                configuration.getDatabaseName()));
        config.setUsername(configuration.getUser());
        config.setPassword(configuration.getPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(Sql.CREATE_TABLE.getQuery())) {
            ps.executeUpdate();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
