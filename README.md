# BungeeServerSync
Плагин для синхронизации игровых серверов между несколькими экземплярами BungeeCord при помощи RedisBungee.
Хранение игровых серверов осуществляется при помощи базы данных.

# Поддерживаемые базы данных
* MySQL

# Description
Плагин для синхронизации игровых серверов между множеством экземпляров прокси-серверов.

# Commands

| Command    | Usage                                                                         | Description                                                                                                                                       | Permission          | Alias |
|------------|-------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|-------|
| /bsync     | /bsync \<bungeeId/all>                                                        | Синхронизирует список игровых серверов на прокси-сервере(-ах). При отсутствии аргументов обновляет сервера на текущем прокси-сервере              | bungeesync.update   | none  |
| /badd      | /badd \<name> \<host> \<motd> \<restricted> \<type> ?\<kick> ?\<requireEmpty> | Добавляет игровой сервер в базу данных и на текущий прокси-сервер. type - тип сервера                                                             | bungeesync.add      | none  |
| /bremove   | /brm \<name> ?\<kick> ?\<requireEmpty>                                        | Удаляет игровой сервер из базы данных и текущего прокси-сервера                                                                                   | bungeesync.remove   | brm   |
| /bfallback | /bfallback \<bungeeId/all>                                                    | Обновляет список fallback серверов на прокси-сервере(-ах). При отсутствии аргументов обновляет список fallback серверов на текущем прокси-сервере | bungeesync.fallback | none  |
| /breload   | /breload                                                                      | Перезагружает файлы конфигурации плагина                                                                                                          | bungeesync.reload   | none  |

# Server types

| Server Type | Description                                                |
|-------------|------------------------------------------------------------|
| HUB         | Хаб-сервер. Является приоритетным сервером для подключения |
| GAME        | Игровой сервер. Также может являться и сервером лобби      |                                          |


# Requirements
* Waterfall proxy
* MySQL - хранение списка игровых серверов
* Redis server - используется в RedisBungee
* RedisBungee - плагин-инструмент для синхронизации данных между прокси-серверами

# Installation

* Устанавливаем [RedisBungee](https://github.com/ProxioDev/RedisBungee/wiki/Installation)
* Подготавливаем MySQL сервер
* Кидаем BungeeServerSync в папку плагинов и запускаем сервер
* При необходимости останавливаем сервер и перенастраиваем конфигурацию базы данных

# Build
```
mvn clean package
```

# p.s.
I'll translate this plugin if it gets published on Spigot/Bukkit.
At the moment this is my little personal development.
I'll be glad, if you contribute to this development.
