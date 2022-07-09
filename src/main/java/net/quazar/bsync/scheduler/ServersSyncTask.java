package net.quazar.bsync.scheduler;

import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.quazar.bsync.BungeeServerSync;
import net.quazar.bsync.model.GameServer;
import net.quazar.bsync.service.ServerInfoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ServersSyncTask implements Runnable {

    private final BungeeServerSync plugin;
    private final ServerInfoService serverInfoService;
    @Setter
    private long period;
    private ScheduledTask task;

    public ServersSyncTask(BungeeServerSync plugin,
                           ServerInfoService serverInfoService,
                           long period) {
        this.plugin = plugin;
        this.serverInfoService = serverInfoService;
        this.period = period;
    }

    @Override
    public void run() {
        task = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            plugin.getLogger().info("Scheduled server synchronization");
            ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                Set<String> serverIds = ProxyServer.getInstance().getServersCopy().keySet();
                List<GameServer> gameServers = serverInfoService.findAll();
                List<String> delete = serverIds.stream().
                        filter(id -> gameServers.stream().noneMatch(gameServer -> gameServer.getName().equals(id)))
                        .collect(Collectors.toList());
                delete.forEach(id -> serverInfoService.deleteOnProxy(
                        id,
                        plugin.getConfiguration().getConfiguration().getBoolean("update-service.kick-on-delete-update"),
                        plugin.getConfiguration().getConfiguration().getBoolean("update-service.require-empty-for-delete-update")));
                List<String> synchronizedServers = new ArrayList<>();
                gameServers.forEach(gameServer -> {
                    ServerInfo si;
                    if ((si = ProxyServer.getInstance().getServerInfo(gameServer.getName())) != null) {
                        if (gameServer.equals(si) && !delete.contains(gameServer.getName()))
                            return;
                    }
                    serverInfoService.updateOnProxy(
                            gameServer,
                            plugin.getConfiguration().getConfiguration().getBoolean("update-service.kick-on-update"),
                            plugin.getConfiguration().getConfiguration().getBoolean("update-service.require-empty-for-update"));
                    synchronizedServers.add(gameServer.getName());
                });
                CommandSender sender = ProxyServer.getInstance().getConsole();
                if (!synchronizedServers.isEmpty())
                    sender.sendMessage(new TextComponent(String.format(plugin.getMessages().getString("synchronized"),
                            String.join(", ", synchronizedServers))));
                else sender.sendMessage(new TextComponent(plugin.getMessages().getString("already-synchronized")));
            });
        }, period, period, TimeUnit.SECONDS);
    }

    public void cancel() {
        task.cancel();
        task = null;
    }
}
