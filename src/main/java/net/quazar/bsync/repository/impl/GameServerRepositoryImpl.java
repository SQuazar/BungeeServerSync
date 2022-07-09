package net.quazar.bsync.repository.impl;

import lombok.AllArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.quazar.bsync.database.Database;
import net.quazar.bsync.database.Sql;
import net.quazar.bsync.model.GameServer;
import net.quazar.bsync.model.ServerType;
import net.quazar.bsync.repository.GameServerRepository;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Репозиторий, через который идёт управление объектами базы данных
 */
@AllArgsConstructor
public final class GameServerRepositoryImpl implements GameServerRepository {

    private final Database database;

    /**
     * Получает объект из базы данных. Не прокси сервера!!
     * @param key имя сервера
     * @return экземпляр информации о сервере
     */
    @Override
    public Optional<GameServer> findByKey(@NotNull String key) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(Sql.SELECT_BY_NAME.getQuery())) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return Optional.of(fromResultSet(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Получает объекты из базы данных. Не прокси сервера!!
     * @return список экземпляров информации о серверах
     */
    @Override
    public List<GameServer> findAll() {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(Sql.SELECT_ALL.getQuery())) {
            List<GameServer> serverInfos = new ArrayList<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                serverInfos.add(fromResultSet(rs));
            return serverInfos;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Сохраняет сервер в базу данных
     * @param value экземпляр информации сервера
     * @return экземпляр информации сервера
     */
    @Override
    public @NotNull GameServer save(@NotNull GameServer value) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(Sql.INSERT.getQuery())) {
            ps.setString(1, value.getName());
            String address = value.getAddress();
            ps.setString(2, address);
            ps.setString(3, value.getMotd());
            ps.setBoolean(4, value.isRestricted());
            ps.setString(5, value.getServerType().name().toLowerCase());
            ps.setString(6, address);
            ps.setString(7, value.getMotd());
            ps.setBoolean(8, value.isRestricted());
            ps.setString(9, value.getServerType().name().toLowerCase());
            ps.executeUpdate();
            return value;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Удаляет объект из базы данных
     * @param value экземпляр информации сервера
     */
    @Override
    public void delete(@NotNull GameServer value) {
        try (Connection connection = database.getConnection();
        PreparedStatement ps = connection.prepareStatement(Sql.DELETE.getQuery())) {
            ps.setString(1, value.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаляет объект из базы данных по ключу
     * @param key ключ объекта
     */
    @Override
    public void deleteByKey(@NotNull String key) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(Sql.DELETE.getQuery())) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Преобразует ResultSet в ServerInfo
     * @param rs result set
     * @return экземпляр информации сервера из ResultSet
     * @throws SQLException в случае, если ResultSet будет содержать неверные данные
     */
    private GameServer fromResultSet(ResultSet rs) throws SQLException {
        String name = rs.getString(1);
        String[] addr = rs.getString(2).split(":");
        SocketAddress address = InetSocketAddress.createUnresolved(addr[0], Integer.parseInt(addr[1]));
        String motd = rs.getString(3);
        boolean restricted = rs.getBoolean(4);
        ServerType serverType = ServerType.valueOf(rs.getString(5).toUpperCase());
        ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(name, address, motd, restricted);
        return new GameServer(name, addr[0] + ":" + addr[1], motd, restricted, serverType, serverInfo);
    }
}
