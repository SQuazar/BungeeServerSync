package net.quazar.mg.scheduler;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.quazar.mg.BungeeServerSync;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class EmptyServerInfoTask implements Runnable {

    private final BungeeServerSync plugin;
    private final ServerInfo serverInfo;
    private ScheduledTask scheduledTask;

    @Override
    public void run() {
        scheduledTask = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            if (serverInfo.getPlayers().isEmpty()) {
                ProxyServer.getInstance().getServers().put(serverInfo.getName(), serverInfo);
                scheduledTask.cancel();
            }
        }, 1L, 3L, TimeUnit.SECONDS);
    }
}
