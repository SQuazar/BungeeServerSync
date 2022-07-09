package net.quazar.bsync.service.impl;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.quazar.bsync.BungeeServerSync;
import net.quazar.bsync.exception.GameServerNotFoundException;
import net.quazar.bsync.exception.ServerInfoException;
import net.quazar.bsync.model.GameServer;
import net.quazar.bsync.model.ServerType;
import net.quazar.bsync.repository.GameServerRepository;
import net.quazar.bsync.scheduler.DeleteEmptyGameServerTask;
import net.quazar.bsync.scheduler.UpdateEmptyGameServerTask;
import net.quazar.bsync.service.ServerInfoService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ServerInfoServiceImpl implements ServerInfoService {

    private final GameServerRepository repository;
    private final BungeeServerSync plugin;
    private List<ServerInfo> fallback;

    @Override
    public @NotNull GameServer get(@NotNull String name) throws GameServerNotFoundException {
        return repository.findByKey(name).orElseThrow(() -> new GameServerNotFoundException(String.format(
                plugin.getMessages().getString("server-not-found"),
                name
        )));
    }

    @Override
    public @NotNull GameServer save(@NotNull GameServer model) {
        GameServer server = repository.save(model);
        plugin.getLogger().info(String.format("Server %s is saved in database", model));
        return server;
    }

    @Override
    public List<GameServer> findAll() {
        return repository.findAll();
    }

    @Override
    public void delete(@NotNull GameServer model) {
        repository.delete(model);
        plugin.getLogger().info(String.format("Server %s is deleted from database", model));
    }

    @Override
    public void deleteByName(@NotNull String name) {
        repository.deleteByKey(name);
    }

    @Override
    public void deleteOnProxy(@NotNull String name, boolean kick, boolean requireEmpty) throws GameServerNotFoundException {
        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(name);
        if (serverInfo == null)
            throw new GameServerNotFoundException(String.format(
                    plugin.getMessages().getString("server-not-found"),
                    name));
        if (kick)
            fallback(serverInfo.getPlayers(), String.format(
                    plugin.getMessages().getString("server-deleted"),
                    serverInfo.getName()));
        if (requireEmpty)
            new DeleteEmptyGameServerTask(plugin, serverInfo);
        else {
//            ProxyServer.getInstance().getServers().remove(serverInfo.getName());
            ProxyServer.getInstance().getConfig().removeServer(serverInfo);
            if (!kick)
                serverInfo.getPlayers().forEach(proxiedPlayer ->
                        proxiedPlayer.sendMessage(new TextComponent(plugin.getMessages().getString("notify-server-updated"))));
        }
        plugin.getLogger().info(String.format("Server %s is deleted from proxy", name));
    }

    @Override
    public void updateOnProxy(@NotNull GameServer gameServer, boolean kick, boolean requireEmptyServer) {
        ServerInfo si;
        if ((si = ProxyServer.getInstance().getServerInfo(gameServer.getName())) != null) {
            Collection<ProxiedPlayer> players = si.getPlayers();
            if (kick)
                fallback(players, String.format(plugin.getMessages().getString("reason-server-updated"), si.getName()));
            if (requireEmptyServer)
                new UpdateEmptyGameServerTask(plugin, si, gameServer).run();
            else {
//                ProxyServer.getInstance().getServers().put(gameServer.getName(), gameServer.getServerInfo());
                ProxyServer.getInstance().getConfig().addServer(gameServer.getServerInfo());
                plugin.getLogger().info(String.format("Server %s is synchronized on proxy", gameServer.getName()));
                if (!kick)
                    players.forEach(proxiedPlayer ->
                            proxiedPlayer.sendMessage(new TextComponent(plugin.getMessages().getString("notify-server-updated"))));
                else
                    reconnect(gameServer.getServerInfo(), players, plugin.getMessages().getString("reconnect-message"));
            }
            return;
        }
//        ProxyServer.getInstance().getServers().put(gameServer.getName(), gameServer.getServerInfo());
        ProxyServer.getInstance().getConfig().addServer(gameServer.getServerInfo());
        plugin.getLogger().info(String.format("Server %s is synchronized on proxy", gameServer.getName()));
    }

    @Override
    public void updateFallbackServers() {
        resolveFallbackServers(true);
    }

    @Override
    public List<String> getFallbackIds() {
        return resolveFallbackServers(false).stream()
                .map(ServerInfo::getName)
                .collect(Collectors.toList());
    }

    private synchronized void fallback(Collection<ProxiedPlayer> players, String reason) {
        players.forEach(proxiedPlayer -> {
            proxiedPlayer.connect(resolveFallbackServer(resolveFallbackServers(false)));
            proxiedPlayer.sendMessage(new TextComponent(reason));
        });
    }

    private synchronized void reconnect(ServerInfo server, Collection<ProxiedPlayer> players, String reason) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> players.forEach(proxiedPlayer -> {
            if (server.canAccess(proxiedPlayer)) {
                proxiedPlayer.connect(server, ServerConnectEvent.Reason.PLUGIN);
                if (reason != null)
                    proxiedPlayer.sendMessage(new TextComponent(reason));
            }
        }), 1L, TimeUnit.SECONDS);
    }

    private List<ServerInfo> resolveFallbackServers(boolean update) {
        if (this.fallback != null && !update) return this.fallback;
        try {
            return this.fallback = Collections.singletonList(ProxyServer.getInstance().getServerInfo(get("fallback").getName()));
        } catch (ServerInfoException e) {
            List<ServerInfo> fallback = findAll().stream()
                    .filter(gameServer -> !gameServer.isRestricted()
                            && gameServer.getServerType() == ServerType.HUB)
                    .map(gameServer -> ProxyServer.getInstance().getServerInfo(gameServer.getName()))
                    .collect(Collectors.toList());
            if (fallback.isEmpty()) {
                for (ListenerInfo listener : ProxyServer.getInstance().getConfigurationAdapter().getListeners()) {
                    for (String server : listener.getServerPriority())
                        fallback.add(ProxyServer.getInstance().getServerInfo(server));
                    break;
                }
                if (fallback.isEmpty())
                    throw new NullPointerException("Fallback server not found!");
            }
            return this.fallback = Collections.unmodifiableList(fallback);
        }
    }

    private ServerInfo resolveFallbackServer(List<ServerInfo> fallbackServers) {
        ServerInfo serverInfo = fallbackServers.get(0);
        for (ServerInfo info : fallbackServers)
            if (info.getPlayers().size() < serverInfo.getPlayers().size())
                serverInfo = info;
        return serverInfo;
    }
}
