package net.quazar.mg.service.impl;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.quazar.mg.BungeeServerSync;
import net.quazar.mg.exception.GameServerNotFoundException;
import net.quazar.mg.exception.ServerInfoException;
import net.quazar.mg.mapper.ObjectMapper;
import net.quazar.mg.model.GameServer;
import net.quazar.mg.model.ServerType;
import net.quazar.mg.repository.GameServerRepository;
import net.quazar.mg.scheduler.EmptyServerInfoTask;
import net.quazar.mg.service.ServerInfoService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ServerInfoServiceImpl implements ServerInfoService {

    private final GameServerRepository repository;
    private final ObjectMapper<ServerInfo, GameServer> serverMapper;
    private final BungeeServerSync plugin;
    private List<ServerInfo> fallback;

    @Override
    public @NotNull GameServer get(@NotNull String name) throws GameServerNotFoundException {
        return repository.findByKey(name).orElseThrow(() -> new GameServerNotFoundException(String.format(
                ChatColor.RED + "Server with name %s not found!",
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
    public void deleteOnProxy(@NotNull String name, boolean kick) {
        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(name);
        if (serverInfo == null)
            throw new GameServerNotFoundException(String.format(ChatColor.RED + "Server with name %s not found!",
                    name));
        if (kick)
            fallback(serverInfo.getPlayers(), String.format("&cСервер %s был удалён.", serverInfo.getName()));
        else {
            ProxyServer.getInstance().getServers().remove(serverInfo.getName());
            serverInfo.getPlayers().forEach(proxiedPlayer ->
                    proxiedPlayer.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            "&cДанный сервер был обновлён на прокси-сервере. " +
                                    "Пожалуйста,&e перезайдите&c, чтобы избежать потери данных!"))));
        }
        plugin.getLogger().info(String.format("Server %s is deleted from proxy", name));
    }

    @Override
    public void updateOnProxy(@NotNull GameServer gameServer, boolean kick, boolean requireEmptyServer) {
        ServerInfo si;
        if ((si = ProxyServer.getInstance().getServerInfo(gameServer.getName())) != null) {
            Collection<ProxiedPlayer> players = si.getPlayers();
            if (kick)
                fallback(players, String.format("&cСервер %s был обновлён на прокси-сервере.", si.getName()));
            if (requireEmptyServer)
                new EmptyServerInfoTask(plugin, si, gameServer).run();
            else {
                ProxyServer.getInstance().getServers().put(gameServer.getName(), gameServer.getServerInfo());
                plugin.getLogger().info(String.format("Server %s is synchronized on proxy", gameServer.getName()));
                if (!kick)
                    players.forEach(proxiedPlayer ->
                            proxiedPlayer.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                                    "&cДанный сервер был обновлён на прокси-сервере. " +
                                            "Пожалуйста,&e перезайдите&c, чтобы избежать потери данных!"))));
                else reconnect(gameServer.getServerInfo(), players, null);
            }
            return;
        }
        ProxyServer.getInstance().getServers().put(gameServer.getName(), gameServer.getServerInfo());
        plugin.getLogger().info(String.format("Server %s is synchronized on proxy", gameServer.getName()));
    }

    @Override
    public void updateFallbackServers() {
        resolveFallbackServers(true);
    }

    private synchronized void fallback(Collection<ProxiedPlayer> players, String reason) {
        players.forEach(proxiedPlayer -> {
            proxiedPlayer.connect(resolveFallbackServer(resolveFallbackServers(false)));
            proxiedPlayer.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', reason)));
        });
    }

    private synchronized void reconnect(ServerInfo server, Collection<ProxiedPlayer> players, String reason) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> players.forEach(proxiedPlayer -> {
            if (server.canAccess(proxiedPlayer)) {
                proxiedPlayer.connect(server, ServerConnectEvent.Reason.PLUGIN);
                if (reason != null)
                    proxiedPlayer.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', reason)));
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
                            && gameServer.getServerType() == ServerType.FALLBACK)
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
