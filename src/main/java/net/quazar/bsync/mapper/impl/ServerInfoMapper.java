package net.quazar.bsync.mapper.impl;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.quazar.bsync.mapper.ObjectMapper;
import net.quazar.bsync.model.GameServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class ServerInfoMapper implements ObjectMapper<ServerInfo, GameServer> {
    @Override
    public GameServer mapTo(ServerInfo source) {
        InetSocketAddress address = (InetSocketAddress) source.getSocketAddress();
        return new GameServer
                (
                        source.getName(),
                        address.getHostName() + ":" + address.getPort(),
                        source.getMotd(),
                        source.isRestricted(),
                        null,
                        source
                );
    }

    @Override
    public ServerInfo mapFrom(GameServer gameServer) {
        String[] addr = gameServer.getAddress().split(":");
        SocketAddress socketAddress = InetSocketAddress.createUnresolved(addr[0], Integer.parseInt(addr[1]));
        return ProxyServer.getInstance().constructServerInfo
                (
                        gameServer.getName(),
                        socketAddress,
                        gameServer.getMotd(),
                        gameServer.isRestricted()
                );
    }
}
