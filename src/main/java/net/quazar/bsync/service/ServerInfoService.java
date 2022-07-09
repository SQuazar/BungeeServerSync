package net.quazar.bsync.service;

import net.quazar.bsync.exception.GameServerNotFoundException;
import net.quazar.bsync.model.GameServer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Сервис для взаимодействия с объектами информации о серверах из репозитория базы данных и прокси-сервера
 */
public interface ServerInfoService {

    /**
     * Получает экземпляр информации о сервере из репозитория базы данных
     * @param name имя серевера
     * @return экземпляр информации о сервере
     */
    @NotNull GameServer get(@NotNull String name) throws GameServerNotFoundException;

    /**
     * Сохраняет экземпляр информации о сервере в репозиторий базы данных
     * @param model экземпляр информации о сервере
     * @return экземпляр информации о сервере
     */
    @NotNull GameServer save(@NotNull GameServer model);

    /**
     * Получает все экземпляры информации о серверах из репозитория базы данных
     * @return экземпляры информации о серверах
     */
    List<GameServer> findAll();

    /**
     * Удаляет информацию о сервере из репозитория базы данных
     * @param model экземпляр информации о сервере
     */
    void delete(@NotNull GameServer model);

    /**
     * Удаляет информацию о сервере из репозитория базы данных
     * @param name имя сервера
     */
    void deleteByName(@NotNull String name);

    /**
     * Удаляет сервер на прокси-сервере
     * @param name имя сервера
     * @param kick нужно ли выкидывать игроков с сервера
     * @param requireEmpty дожидаться ли "опустошения" сервера
     */
    void deleteOnProxy(@NotNull String name, boolean kick, boolean requireEmpty) throws GameServerNotFoundException;

    /**
     * Обновляет информацию о сервере непосредственно на прокси-сервере
     * @param gameServer информация о сервере
     * @param kick кикать ли игроков во время обновления
     * @param requireEmptyServer дожидаться ли "опустошения" сервера
     */
    void updateOnProxy(@NotNull GameServer gameServer, boolean kick, boolean requireEmptyServer);

    /**
     * Обновляет Fallback сервера на прокси-сервере
     */
    void updateFallbackServers();

    List<String> getFallbackIds();

}
