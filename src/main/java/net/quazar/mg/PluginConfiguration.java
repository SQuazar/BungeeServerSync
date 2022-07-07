package net.quazar.mg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class PluginConfiguration {

    private final Plugin plugin;
    @Getter
    private Configuration configuration;

    public void makeConfig() throws IOException {
        if (!plugin.getDataFolder().exists())
            plugin.getLogger().info("Created config folder: " + plugin.getDataFolder().mkdir());
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(in);
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, configFile);
            }
        } else configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

}
