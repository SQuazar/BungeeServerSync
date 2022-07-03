package net.quazar.mg.database;

public enum Sql {
    CREATE_TABLE("CREATE TABLE IF NOT EXISTS `server_info`(\n" +
            "    `name` VARCHAR(32) NOT NULL PRIMARY KEY,\n" +
            "    `address` VARCHAR(100) NOT NULL,\n" +
            "    `motd` VARCHAR(255) NOT NULL,\n" +
            "    `restricted` BOOLEAN NOT NULL DEFAULT false\n" +
            ");"),
    SELECT_BY_NAME("SELECT * FROM `server_info` where `name`=?"),
    SELECT_ALL("SELECT * FROM `server_info`"),
    INSERT("INSERT INTO `server_info`(`name`, `address`, `motd`, `restricted`) VALUES (?, ?, ?, ?)"),
    DELETE("DELETE FROM `server_info` WHERE `name`=?");

    private final String query;

    Sql(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
