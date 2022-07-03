package net.quazar.mg.service;

import net.md_5.bungee.api.config.ServerInfo;
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
    @NotNull ServerInfo get(@NotNull String name);

    /**
     * Сохраняет экземпляр информации о сервере в репозиторий базы данных
     * @param model экземпляр информации о сервере
     * @return экземпляр информации о сервере
     */
    @NotNull ServerInfo save(@NotNull ServerInfo model);

    /**
     * Получает все экземпляры информации о серверах из репозитория базы данных
     * @return экземпляры информации о серверах
     */
    List<ServerInfo> findAll();

    /**
     * Удаляет информацию о сервере из репозитория базы данных
     * @param model экземпляр информации о сервере
     */
    void delete(@NotNull ServerInfo model);

    /**
     * Удаляет информацию о сервере из репозитория базы данных
     * @param name имя сервера
     */
    void deleteByName(@NotNull String name);

    /**
     * Обновляет информацию о сервере непосредственно на прокси-сервере
     * @param serverInfo информация о сервере
     * @param kick кикать ли игроков во время обновления
     * @param requireEmptyServer дожидаться ли "опустошения" сервера
     */
    void updateOnProxy(@NotNull ServerInfo serverInfo, boolean kick, boolean requireEmptyServer);

}
