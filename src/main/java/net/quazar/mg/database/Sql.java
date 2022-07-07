package net.quazar.mg.database;

import org.intellij.lang.annotations.Language;

public enum Sql {
    CREATE_TABLE("CREATE TABLE IF NOT EXISTS `server_info`(\n" +
            "    `name` VARCHAR(32) NOT NULL PRIMARY KEY,\n" +
            "    `address` VARCHAR(100) NOT NULL,\n" +
            "    `motd` VARCHAR(255) NOT NULL,\n" +
            "    `restricted` BOOLEAN NOT NULL DEFAULT false,\n" +
            "    `type` VARCHAR(32) DEFAULT 'game'\n" +
            ");"),
    SELECT_BY_NAME("SELECT * FROM `server_info` where `name`=?"),
    SELECT_ALL("SELECT * FROM `server_info`"),
    INSERT("INSERT INTO `server_info`(`name`, `address`, `motd`, `restricted`, `type`) VALUES (?, ?, ?, ?, ?)" +
            "ON DUPLICATE KEY UPDATE `address`=?, `motd`=?, `restricted`=?, `type`=?"),
    DELETE("DELETE FROM `server_info` WHERE `name`=?");

    private final String query;

    Sql(@Language("MySQL") String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
