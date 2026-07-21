package com.atlaskv.cli;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads and holds CLI configuration from ~/.atlaskv/config.yml.
 */
public final class CliConfig {

    private static final String CONFIG_DIR = ".atlaskv";
    private static final String CONFIG_FILE = "config.yml";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_TIMEOUT = 5;

    private String host;
    private int port;
    private int timeoutSeconds;
    private String authType;
    private String authToken;
    private String authUsername;
    private String authPassword;

    /**
     * Private constructor, use static factory.
     */
    private CliConfig() {
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.timeoutSeconds = DEFAULT_TIMEOUT;
    }

    /**
     * Loads configuration from the default config file path.
     *
     * @return loaded configuration
     */
    public static CliConfig load() {
        Path configPath = Path.of(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE);
        return load(configPath);
    }

    /**
     * Loads configuration from a specific path.
     *
     * @param configPath path to config file
     * @return loaded configuration
     */
    public static CliConfig load(Path configPath) {
        CliConfig config = new CliConfig();

        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(in);
                if (data != null) {
                    config.applyMap(data);
                }
            } catch (IOException e) {
                OutputFormatter.printWarning("Failed to read config file: " + e.getMessage());
            }
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private void applyMap(Map<String, Object> data) {
        if (data.containsKey("host")) {
            this.host = String.valueOf(data.get("host"));
        }
        if (data.containsKey("port")) {
            this.port = ((Number) data.get("port")).intValue();
        }
        if (data.containsKey("timeout")) {
            this.timeoutSeconds = ((Number) data.get("timeout")).intValue();
        }

        Object authObj = data.get("authentication");
        if (authObj instanceof Map) {
            Map<String, Object> auth = (Map<String, Object>) authObj;
            this.authType = (String) auth.get("type");
            this.authToken = (String) auth.get("token");
            this.authUsername = (String) auth.get("username");
            this.authPassword = (String) auth.get("password");
        }
    }

    /**
     * Returns the current config file path.
     *
     * @return config path
     */
    public static Path configPath() {
        return Path.of(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getAuthType() {
        return authType;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }
}
