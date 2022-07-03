package net.quazar.mg.database;

import lombok.Data;

@Data
public class DatabaseConfiguration {

    private final String host;
    private final int port;
    private final String databaseName;
    private final String user;
    private final String password;

}
