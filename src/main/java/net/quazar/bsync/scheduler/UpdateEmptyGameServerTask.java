package net.quazar.bsync.scheduler;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.quazar.bsync.BungeeServerSync;
import net.quazar.bsync.model.GameServer;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class UpdateEmptyGameServerTask implements Runnable {

    private final BungeeServerSync plugin;
    private final ServerInfo serverInfo;
    private final GameServer gameServer;
    private ScheduledTask scheduledTask;

    @Override
    public void run() {
        scheduledTask = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            if (serverInfo.getPlayers().isEmpty()) {
//                ProxyServer.getInstance().getServers().put(gameServer.getName(), gameServer.getServerInfo());
                ProxyServer.getInstance().getConfig().addServer(gameServer.getServerInfo());
                plugin.getLogger().info(String.format("Empty server %s is synchronized on proxy by scheduled task", gameServer.getName()));
                scheduledTask.cancel();
            }
        }, 0L, 3L, TimeUnit.SECONDS);
    }
}
