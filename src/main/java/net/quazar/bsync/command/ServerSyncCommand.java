package net.quazar.bsync.command;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeCommandSender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.quazar.bsync.BungeeServerSync;
import net.quazar.bsync.exception.GameServerNotFoundException;
import net.quazar.bsync.model.GameServer;
import net.quazar.bsync.model.ServerType;
import net.quazar.bsync.service.ServerInfoService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class ServerSyncCommand {

    public static final class SyncServers extends Command {

        private final ServerInfoService serverInfoService;
        private final BungeeServerSync plugin;
        private final RedisBungeeAPI redisBungeeAPI = RedisBungeeAPI.getRedisBungeeApi();

        public SyncServers(ServerInfoService serverInfoService,
                           BungeeServerSync plugin) {
            super("bsync", "bungeesync.update");
            this.serverInfoService = serverInfoService;
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                if ("all".equalsIgnoreCase(args[0])) {
                    redisBungeeAPI.sendProxyCommand(getName());
                } else {
                    String serverId = args[0];
                    if (redisBungeeAPI.getAllServers().contains(serverId)) {
                        redisBungeeAPI.sendProxyCommand(serverId, getName());
                        return;
                    }
                    sender.sendMessage(new TextComponent(String.format(plugin.getMessages().getString("not-found-in-redis"),
                            serverId)));
                }
                return;
            }
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
                CommandSender s = sender;
                if (sender instanceof RedisBungeeCommandSender)
                    s = ProxyServer.getInstance().getConsole();
                if (!synchronizedServers.isEmpty())
                    s.sendMessage(new TextComponent(String.format(plugin.getMessages().getString("synchronized"),
                            String.join(", ", synchronizedServers))));
                else s.sendMessage(new TextComponent(plugin.getMessages().getString("already-synchronized")));
            });
        }
    }

    public static final class AddServer extends Command {

        private final ServerInfoService serverInfoService;
        private final BungeeServerSync plugin;

        public AddServer(ServerInfoService serverInfoService,
                         BungeeServerSync plugin) {
            super("badd", "bungeesync.add");
            this.serverInfoService = serverInfoService;
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length >= 5) {
                String name = args[0];
                String[] addr = args[1].split(":");
                if (addr.length != 2) {
                    sender.sendMessage(new TextComponent(plugin.getMessages().getString("not-valid-host")));
                    return;
                }
                InetSocketAddress socketAddress;
                try {
                    socketAddress = InetSocketAddress.createUnresolved(addr[0], Integer.parseInt(addr[1]));
                } catch (NumberFormatException exception) {
                    sender.sendMessage(new TextComponent(plugin.getMessages().getString("not-valid-port")));
                    return;
                }
                String motd = args[2];
                boolean restricted = Boolean.parseBoolean(args[3]);
                ServerType serverType;
                try {
                    serverType = ServerType.valueOf(args[4].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponent(
                            String.format(plugin.getMessages().getString("not-valid-server-type"),
                                    Arrays.stream(ServerType.values())
                                            .map(type -> type.name().toLowerCase())
                                            .collect(Collectors.joining(", ")))));
                    return;
                }
                GameServer server = new GameServer(name, socketAddress, motd, restricted, serverType);
                AtomicBoolean kick = new AtomicBoolean(false);
                if (args.length >= 6)
                    kick.set(Boolean.parseBoolean(args[5]));
                AtomicBoolean requireEmpty = new AtomicBoolean(false);
                if (args.length >= 7)
                    requireEmpty.set(Boolean.parseBoolean(args[6]));
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    serverInfoService.save(server);
                    serverInfoService.updateOnProxy(server, kick.get(), requireEmpty.get());
                    sender.sendMessage(new TextComponent(String.format(
                            plugin.getMessages().getString("server-added"),
                            server.getName())));
                });
                return;
            }
            sender.sendMessage(new TextComponent(plugin.getMessages().getString("invalid-add-command")));
        }
    }

    public static final class RemoveServer extends Command {

        private final ServerInfoService serverInfoService;
        private final BungeeServerSync plugin;

        public RemoveServer(ServerInfoService serverInfoService,
                            BungeeServerSync plugin) {
            super("bremove", "bungeesync.remove", "brm");
            this.serverInfoService = serverInfoService;
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length >= 1) {
                String name = args[0];
                GameServer server;
                try {
                    server = serverInfoService.get(name);
                } catch (GameServerNotFoundException e) {
                    sender.sendMessage(new TextComponent(e.getMessage()));
                    return;
                }
                AtomicBoolean kick = new AtomicBoolean(false);
                if (args.length >= 2)
                    kick.set(Boolean.parseBoolean(args[1]));
                AtomicBoolean requireEmpty = new AtomicBoolean(false);
                if (args.length >= 3)
                    requireEmpty.set(Boolean.parseBoolean(args[2]));
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    serverInfoService.delete(server);
                    sender.sendMessage(new TextComponent(String.format(
                            plugin.getMessages().getString("server-removed"),
                            name)));
                    try {
                        serverInfoService.deleteOnProxy(server.getName(), kick.get(), requireEmpty.get());
                    } catch (GameServerNotFoundException e) {
                        sender.sendMessage(new TextComponent(String.format(
                                plugin.getMessages().getString("server-delete-exception"),
                                name,
                                e.getMessage())));
                        return;
                    }
                    sender.sendMessage(new TextComponent(String.format(
                            plugin.getMessages().getString("server-removed-on-proxy"),
                            server.getName())));
                });
                return;
            }
            sender.sendMessage(new TextComponent(plugin.getMessages().getString("invalid-remove-command")));
        }
    }

    public static final class FallbackCommand extends Command {

        private final ServerInfoService serverInfoService;
        private final BungeeServerSync plugin;
        private final RedisBungeeAPI redisBungeeAPI = RedisBungeeAPI.getRedisBungeeApi();

        public FallbackCommand(ServerInfoService serverInfoService,
                               BungeeServerSync plugin) {
            super("bfallback", "bungeesync.fallback");
            this.serverInfoService = serverInfoService;
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof RedisBungeeCommandSender)
                sender = ProxyServer.getInstance().getConsole();
            if (args.length > 0) {
                if ("all".equalsIgnoreCase(args[0])) redisBungeeAPI.sendProxyCommand(getName());
                else {
                    String serverId = args[0];
                    if (redisBungeeAPI.getAllServers().contains(serverId)) {
                        redisBungeeAPI.sendProxyCommand(serverId, getName());
                        return;
                    }
                    sender.sendMessage(new TextComponent(String.format(
                            plugin.getMessages().getString("not-fount-in-redis"),
                            serverId)));
                }
                return;
            }
            serverInfoService.updateFallbackServers();
            sender.sendMessage(new TextComponent(String.format(
                    plugin.getMessages().getString("fallback-updated"),
                    String.join(", ", serverInfoService.getFallbackIds()))));
        }
    }

    public static final class ReloadCommand extends Command {

        private final BungeeServerSync plugin;
        private final RedisBungeeAPI redisBungeeAPI = RedisBungeeAPI.getRedisBungeeApi();

        public ReloadCommand(BungeeServerSync plugin) {
            super("breload", "bungeesync.reload");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("all"))
                    redisBungeeAPI.sendProxyCommand("breload");
                else
                    if (redisBungeeAPI.getAllServers().contains(args[0]))
                        redisBungeeAPI.sendProxyCommand(args[0], "breload");
                    else
                        sender.sendMessage(new TextComponent(String.format(plugin.getMessages().getString("not-found-in-redis"),
                                args[0])));
            }
            plugin.reloadConfiguration();
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Plugin configuration successful reloaded!"));
        }
    }
}