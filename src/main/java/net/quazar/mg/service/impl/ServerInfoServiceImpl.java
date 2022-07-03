package net.quazar.mg.service.impl;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.quazar.mg.BungeeServerSync;
import net.quazar.mg.exception.ServerInfoException;
import net.quazar.mg.exception.ServerInfoNotFoundException;
import net.quazar.mg.repository.ServerInfoRepository;
import net.quazar.mg.scheduler.EmptyServerInfoTask;
import net.quazar.mg.service.ServerInfoService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class ServerInfoServiceImpl implements ServerInfoService {

    private final ServerInfoRepository repository;
    private final BungeeServerSync plugin;
    private ServerInfo fallback;

    @Override
    public @NotNull ServerInfo get(@NotNull String name) {
        return repository.findByKey(name).orElseThrow(() -> new ServerInfoNotFoundException(String.format(
                ChatColor.RED + "Server with name %s not found!",
                name
        )));
    }

    @Override
    public @NotNull ServerInfo save(@NotNull ServerInfo model) {
        return repository.save(model);
    }

    @Override
    public List<ServerInfo> findAll() {
        return repository.findAll();
    }

    @Override
    public void delete(@NotNull ServerInfo model) {
        repository.delete(model);
    }

    @Override
    public void deleteByName(@NotNull String name) {
        repository.deleteByKey(name);
    }

    @Override
    public void updateOnProxy(@NotNull ServerInfo serverInfo, boolean kick, boolean requireEmptyServer) {
        ServerInfo si;
        if ((si = ProxyServer.getInstance().getServerInfo(serverInfo.getName())) != null) {
            if (kick) {
                fallback(si.getPlayers(), "&cСервер %s был обновлён на прокси-сервере.");
            }
            if (requireEmptyServer)
                new EmptyServerInfoTask(plugin, si).run();
            else {
                ProxyServer.getInstance().getServers().put(serverInfo.getName(), serverInfo);
                si.getPlayers().forEach(proxiedPlayer ->
                        proxiedPlayer.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                                "&cДанный сервер был обновлён на прокси-сервере. " +
                                        "Пожалуйста,&e перезайдите&c, чтобы избежать потери данных!"))));
            }
            return;
        }
        ProxyServer.getInstance().getServers().put(serverInfo.getName(), serverInfo);
    }

    private void fallback(Collection<ProxiedPlayer> players, String reason) {
        players.forEach(proxiedPlayer -> {
            proxiedPlayer.connect(resolveFallbackServer());
            proxiedPlayer.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', reason)));
        });
    }

    private ServerInfo resolveFallbackServer() {
        if (fallback != null) return fallback;
        try {
            return fallback = get("fallback");
        } catch (ServerInfoException e) {
            for (ListenerInfo listener : ProxyServer.getInstance().getConfigurationAdapter().getListeners()) {
                for (String server : listener.getServerPriority())
                    return fallback = ProxyServer.getInstance().getServerInfo(server);
                break;
            }
        }
        throw new NullPointerException("Fallback server not found!");
    }
}
