package net.quazar.mg.command;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeCommandSender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.quazar.mg.BungeeServerSync;
import net.quazar.mg.exception.GameServerNotFoundException;
import net.quazar.mg.model.GameServer;
import net.quazar.mg.model.ServerType;
import net.quazar.mg.service.ServerInfoService;

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
                    redisBungeeAPI.getAllServers().forEach(proxy ->
                            redisBungeeAPI.sendProxyCommand(proxy, getName()));
                } else {
                    String serverId = args[0];
                    if (redisBungeeAPI.getAllServers().contains(serverId)) {
                        redisBungeeAPI.sendProxyCommand(serverId, getName());
                        return;
                    }
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            String.format("&cServer with id %s not found in redis.", serverId))));
                }
                return;
            }
            ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                Set<String> serverIds = ProxyServer.getInstance().getServersCopy().keySet();
                List<GameServer> gameServers = serverInfoService.findAll();
                List<String> delete = serverIds.stream().
                        filter(id -> gameServers.stream().noneMatch(gameServer -> gameServer.getName().equals(id)))
                        .collect(Collectors.toList());
                delete.forEach(id -> serverInfoService.deleteOnProxy(id, false));
                List<String> synchronizedServers = new ArrayList<>();
                gameServers.forEach(gameServer -> {
                    ServerInfo si;
                    if ((si = ProxyServer.getInstance().getServerInfo(gameServer.getName())) != null) {
                        if (gameServer.equals(si) && !delete.contains(gameServer.getName()))
                            return;
                    }
                    serverInfoService.updateOnProxy(gameServer, false, false);
                    synchronizedServers.add(gameServer.getName());
                });
                CommandSender s = sender;
                if (sender instanceof RedisBungeeCommandSender)
                    s = ProxyServer.getInstance().getConsole();
                if (!synchronizedServers.isEmpty())
                    s.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            String.format("&aServers &e[%s] &ais synchronized",
                                    String.join(", ", synchronizedServers)))));
                else sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                        "&aAll servers are already synchronized!")));
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
            if (args.length == 5) {
                String name = args[0];
                String[] addr = args[1].split(":");
                if (addr.length != 2) {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Bad host!"));
                    return;
                }
                InetSocketAddress socketAddress;
                try {
                    socketAddress = InetSocketAddress.createUnresolved(addr[0], Integer.parseInt(addr[1]));
                } catch (NumberFormatException exception) {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Bad port in host argument!"));
                    return;
                }
                String motd = args[2];
                boolean restricted = Boolean.parseBoolean(args[3]);
                ServerType serverType;
                try {
                    serverType = ServerType.valueOf(args[4].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            String.format("&cBad server type! Server types is: &e[%s]",
                                    Arrays.stream(ServerType.values())
                                            .map(type -> type.name().toLowerCase())
                                            .collect(Collectors.joining(", "))))));
                    return;
                }
                GameServer server = new GameServer(name, socketAddress, motd, restricted, serverType);
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    serverInfoService.save(server);
                    serverInfoService.updateOnProxy(server, false, true);
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            String.format("&aServer &e%s&a successful added!", server.getName()))));
                });
                return;
            }
            sender.sendMessage(new TextComponent(ChatColor.RED + "Bad command args! Usage /badd <name> <host> <motd> <restricted> <serverType>"));
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
                    kick.set(true);
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    serverInfoService.delete(server);
                    serverInfoService.deleteOnProxy(server.getName(), kick.get());
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            String.format("&aServer &e%s&a successful removed!", server.getName()))));
                });
                return;
            }
            sender.sendMessage(new TextComponent(ChatColor.RED + "Bad command args! Usage /bremove <name> ?<kick>"));
        }
    }

    public static final class FallbackCommand extends Command {

        private final ServerInfoService serverInfoService;
        private final RedisBungeeAPI redisBungeeAPI = RedisBungeeAPI.getRedisBungeeApi();

        public FallbackCommand(ServerInfoService serverInfoService) {
            super("bfallback", "bungeesync.fallback");
            this.serverInfoService = serverInfoService;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof RedisBungeeCommandSender)
                sender = ProxyServer.getInstance().getConsole();
            if (args.length > 0) {
                if ("all".equalsIgnoreCase(args[0])) {
                    redisBungeeAPI.getAllServers().forEach(proxy ->
                            redisBungeeAPI.sendProxyCommand(proxy, getName()));
                } else {
                    String serverId = args[0];
                    if (redisBungeeAPI.getAllServers().contains(serverId)) {
                        redisBungeeAPI.sendProxyCommand(serverId, getName());
                        return;
                    }
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            String.format("&cServer with id %s not found in redis.", serverId))));
                }
                return;
            }
            serverInfoService.updateFallbackServers();
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                    "&aList of fallback servers is updated!")));
        }
    }
}