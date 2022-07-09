package net.quazar.bsync;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.quazar.bsync.command.ServerSyncCommand;
import net.quazar.bsync.config.LocaledMessages;
import net.quazar.bsync.config.PluginConfiguration;
import net.quazar.bsync.database.Database;
import net.quazar.bsync.database.DatabaseConfiguration;
import net.quazar.bsync.database.MySQL;
import net.quazar.bsync.model.ServerType;
import net.quazar.bsync.repository.GameServerRepository;
import net.quazar.bsync.repository.impl.GameServerRepositoryImpl;
import net.quazar.bsync.scheduler.ServersSyncTask;
import net.quazar.bsync.service.ServerInfoService;
import net.quazar.bsync.service.impl.ServerInfoServiceImpl;

import java.io.IOException;
import java.sql.SQLException;

public final class BungeeServerSync extends Plugin {

    private Database database;
    private ServerInfoService serverInfoService;
    @Getter
    private PluginConfiguration configuration;
    @Getter
    private LocaledMessages messages;
    private ServersSyncTask serversSyncTask;

    @Override
    public void onEnable() {
        getLogger().info("Initializing configuration files");
        configuration = new PluginConfiguration(this);
        try {
            configuration.makeConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        messages = new LocaledMessages(this, configuration.getConfiguration().getString("messages-locale"));
        try {
            messages.makeMessages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getLogger().info("Configuration files is initialized!");

        getLogger().info("Initializing database");
        try {
            database = new MySQL(new DatabaseConfiguration(configuration));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        getLogger().info("Database is successful initialized!");

        GameServerRepository gameServerRepository = new GameServerRepositoryImpl(database);
        serverInfoService = new ServerInfoServiceImpl(gameServerRepository, this);

        getLogger().info("Synchronizing servers from database...");
        getProxy().getConfig().getListeners().forEach(listenerInfo -> listenerInfo.getServerPriority().clear());
        getProxy().getConfig().removeServers(getProxy().getConfig().getServersCopy().values());
        serverInfoService.findAll().forEach(gameServer -> {
            serverInfoService.updateOnProxy(gameServer, true, false);
            if (gameServer.getServerType() == ServerType.HUB)
                getProxy().getConfig().getListeners().forEach(listenerInfo ->
                        listenerInfo.getServerPriority().add(gameServer.getName()));
        });
        getLogger().info("Servers is updated!");

        startScheduledServersSyncTask();

        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.SyncServers(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.AddServer(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.RemoveServer(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.FallbackCommand(serverInfoService, this));
        getProxy().getPluginManager().registerCommand(this, new ServerSyncCommand.ReloadCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("Closing database connection...");
        database.close();
        getLogger().info("Database connection is closed");
        if (serversSyncTask != null)
            serversSyncTask.cancel();
    }

    private void startScheduledServersSyncTask() {
        if (serversSyncTask != null) serversSyncTask.cancel();
        long syncPeriod;
        if ((syncPeriod = configuration.getConfiguration().getLong("update-service.scheduled-update")) > -1) {
            getLogger().info(String.format("Starting scheduled servers sync task with period %d seconds",
                    syncPeriod));
            serversSyncTask = new ServersSyncTask(this, serverInfoService, syncPeriod);
            serversSyncTask.run();
            getLogger().info(String.format("Scheduled server sync task is stated! Next scheduled synchronization via %d seconds.",
                    syncPeriod));
        }
    }

    public void reloadConfiguration() {
        configuration.reload();
        messages.setLocale(configuration.getConfiguration().getString("messages-locale"));
        messages.reload();
        startScheduledServersSyncTask();
    }
}
