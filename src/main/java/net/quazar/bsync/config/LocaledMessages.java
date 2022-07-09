package net.quazar.bsync.config;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.quazar.bsync.BungeeServerSync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class LocaledMessages {

    private final BungeeServerSync plugin;
    private String locale;
    @Getter
    private Configuration configuration;

    public LocaledMessages(BungeeServerSync plugin, String locale) {
        this.plugin = plugin;
        this.locale = locale;
    }

    public void makeMessages() throws IOException {
        if (!plugin.getDataFolder().exists())
            plugin.getLogger().info("Created config folder: " + plugin.getDataFolder().mkdir());
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + locale + ".yml");
        if (!messagesFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream(messagesFile.getName())) {
                if (in == null)
                    throw new FileNotFoundException(String.format(
                            "File %s not found in config directory! Change messages-locale or create messages file with selected localization",
                            messagesFile.getName()
                    ));
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(in);
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, messagesFile);
            }
        } else configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesFile);
    }

    public String getString(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                configuration.getString(path));
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void reload() {
        try {
            makeMessages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
