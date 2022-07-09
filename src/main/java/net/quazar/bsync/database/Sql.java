package net.quazar.bsync.database;

import net.quazar.bsync.model.ServerType;
import org.intellij.lang.annotations.Language;

public enum Sql {
    CREATE_TABLE("CREATE TABLE IF NOT EXISTS `server_info`(" +
            "`name` VARCHAR(32) NOT NULL PRIMARY KEY," +
            "`address` VARCHAR(100) NOT NULL," +
            "`motd` VARCHAR(255) NOT NULL," +
            "`restricted` BOOLEAN NOT NULL DEFAULT false," +
            "`type` VARCHAR(32) DEFAULT '" + ServerType.GAME.name().toLowerCase() + "');"),
    SELECT_BY_NAME("SELECT * FROM `server_info` where `name`=?"),
    SELECT_ALL("SELECT * FROM `server_info`"),
    INSERT("INSERT INTO `server_info`(`name`, `address`, `motd`, `restricted`, `type`) VALUES (?, ?, ?, ?, ?)" +
            "ON DUPLICATE KEY UPDATE `address`=?, `motd`=?, `restricted`=?, `type`=?"),
    DELETE("DELETE FROM `server_info` WHERE `name`=?");

    private final String query;

    Sql(@Language("sql") String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
