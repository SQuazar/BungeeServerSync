package net.quazar.bsync.database;

import lombok.Data;
import net.md_5.bungee.config.Configuration;
import net.quazar.bsync.config.PluginConfiguration;

@Data
public final class DatabaseConfiguration {

    private final String host;
    private final int port;
    private final String databaseName;
    private final String user;
    private final String password;

    public DatabaseConfiguration(PluginConfiguration configuration) {
        Configuration section = configuration.getConfiguration().getSection("database");
        this.host = section.getString("host");
        this.port = section.getInt("port");
        this.databaseName = section.getString("db");
        this.user = section.getString("user");
        this.password = section.getString("password");
    }

}
