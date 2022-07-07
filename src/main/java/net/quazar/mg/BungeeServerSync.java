package net.quazar.mg;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.quazar.mg.command.ServerSyncCommand;
import net.quazar.mg.database.Database;
import net.quazar.mg.database.DatabaseConfiguration;
import net.quazar.mg.database.MySQL;
import net.quazar.mg.mapper.impl.ServerInfoMapper;
import net.quazar.mg.repository.GameServerRepository;
import net.quazar.mg.repository.impl.GameServerRepositoryImpl;
import net.quazar.mg.service.ServerInfoService;
import net.quazar.mg.service.impl.ServerInfoServiceImpl;

import java.io.IOException;
import java.sql.SQLException;

@Getter
public final class BungeeServerSync extends Plugin {

    private Database database;
    private ServerInfoService serverInfoService;
    private ServerInfoMapper serverInfoMapper;
    private PluginConfiguration configuration;

    @Override
    public void onEnable() {
        configuration = new PluginConfiguration(this);
        try {
            configuration.makeConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            database = new MySQL(new DatabaseConfiguration(configuration));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        GameServerRepository gameServerRepository = new GameServerRepositoryImpl(database);
        serverInfoMapper = new ServerInfoMapper();
        serverInfoService = new ServerInfoServiceImpl(gameServerRepository, serverInfoMapper, this);
        getLogger().info("Synchronizing servers from database...");
        serverInfoService.findAll().forEach(gameServer -> serverInfoService.updateOnProxy(gameServer, true, false));
        getLogger().info("Servers is updated!");
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.SyncServers(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.AddServer(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.RemoveServer(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.FallbackCommand(serverInfoService));
    }

    @Override
    public void onDisable() {
        getLogger().info("Closing database connection...");
        database.close();
        getLogger().info("Database connection is closed");
    }
}
