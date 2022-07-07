package net.quazar.mg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.net.InetSocketAddress;

@Data
@AllArgsConstructor
public final class GameServer {

    private final String name;
    private String address;
    private String motd;
    private boolean restricted;
    private ServerType serverType;
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final ServerInfo serverInfo;

    public GameServer(String name, InetSocketAddress address, String motd, boolean restricted, ServerType serverType) {
        this.name = name;
        this.address = address.getHostName() + ":" + address.getPort();
        this.motd = motd;
        this.restricted = restricted;
        this.serverInfo = ProxyServer.getInstance().constructServerInfo(name, address, motd, restricted);
        this.serverType = serverType;
    }

    public boolean equals(ServerInfo serverInfo) {
        return name.equals(serverInfo.getName()) &&
                address.equals(getAddress(serverInfo)) &&
                motd.equals(serverInfo.getMotd()) &&
                restricted == serverInfo.isRestricted();
    }

    private String getAddress(ServerInfo serverInfo) {
        InetSocketAddress socketAddress = (InetSocketAddress) serverInfo.getSocketAddress();
        return socketAddress.getHostName() + ":" + socketAddress.getPort();
    }

}
