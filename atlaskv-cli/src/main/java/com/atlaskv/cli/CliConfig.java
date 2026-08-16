package com.atlaskv.cli;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads, holds, and persists CLI configuration from ~/.atlaskv/config.yml.
 */
public final class CliConfig {

    private static final String CONFIG_DIR = ".atlaskv";
    private static final String CONFIG_FILE = "config.yml";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_TIMEOUT = 5;

    private String endpoint;
    private String host;
    private int port;
    private int timeoutSeconds;
    private String apiKey;
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
        if (data.containsKey("endpoint") && data.get("endpoint") != null) {
            this.endpoint = String.valueOf(data.get("endpoint"));
        }
        if (data.containsKey("host") && data.get("host") != null) {
            this.host = String.valueOf(data.get("host"));
        }
        if (data.containsKey("port") && data.get("port") != null) {
            this.port = ((Number) data.get("port")).intValue();
        }
        if (data.containsKey("timeout") && data.get("timeout") != null) {
            this.timeoutSeconds = ((Number) data.get("timeout")).intValue();
        }
        if (data.containsKey("api-key") && data.get("api-key") != null) {
            this.apiKey = String.valueOf(data.get("api-key"));
            this.authToken = this.apiKey;
            this.authType = "bearer";
        } else if (data.containsKey("apiKey") && data.get("apiKey") != null) {
            this.apiKey = String.valueOf(data.get("apiKey"));
            this.authToken = this.apiKey;
            this.authType = "bearer";
        }

        Object authObj = data.get("authentication");
        if (authObj instanceof Map) {
            Map<String, Object> auth = (Map<String, Object>) authObj;
            this.authType = (String) auth.get("type");
            this.authToken = (String) auth.get("token");
            this.apiKey = this.authToken;
            this.authUsername = (String) auth.get("username");
            this.authPassword = (String) auth.get("password");
        }
    }

    /**
     * Updates a configuration property in-memory.
     *
     * @param key   property key (e.g. endpoint, api-key, host, port, timeout)
     * @param value property value
     */
    public void set(String key, String value) {
        if (key == null) {
            return;
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT).trim().replace("_", "-");
        switch (normalizedKey) {
            case "endpoint" -> this.endpoint = (value != null && !value.isBlank()) ? value.trim() : null;
            case "api-key", "apikey", "token" -> {
                this.apiKey = (value != null && !value.isBlank()) ? value.trim() : null;
                this.authToken = this.apiKey;
                this.authType = (this.apiKey != null) ? "bearer" : null;
            }
            case "host" -> this.host = (value != null && !value.isBlank()) ? value.trim() : DEFAULT_HOST;
            case "port" -> this.port = (value != null && !value.isBlank()) ? Integer.parseInt(value.trim()) : DEFAULT_PORT;
            case "timeout" -> this.timeoutSeconds = (value != null && !value.isBlank()) ? Integer.parseInt(value.trim()) : DEFAULT_TIMEOUT;
            case "auth-type" -> this.authType = value;
            case "username" -> this.authUsername = value;
            case "password" -> this.authPassword = value;
            default -> throw new IllegalArgumentException("Unknown configuration key: " + key);
        }
    }

    /**
     * Persists the current configuration to the default config file path.
     *
     * @throws IOException on write error
     */
    public void save() throws IOException {
        save(configPath());
    }

    /**
     * Persists the current configuration to the specified config file path.
     *
     * @param path file path
     * @throws IOException on write error
     */
    public void save(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        if (endpoint != null && !endpoint.isBlank()) {
            data.put("endpoint", endpoint);
        }
        data.put("host", host);
        data.put("port", port);
        data.put("timeout", timeoutSeconds);

        if (apiKey != null && !apiKey.isBlank()) {
            data.put("api-key", apiKey);
        } else if (authType != null) {
            Map<String, Object> auth = new LinkedHashMap<>();
            auth.put("type", authType);
            if (authToken != null) {
                auth.put("token", authToken);
            }
            if (authUsername != null) {
                auth.put("username", authUsername);
            }
            if (authPassword != null) {
                auth.put("password", authPassword);
            }
            data.put("authentication", auth);
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        try (Writer writer = Files.newBufferedWriter(path)) {
            yaml.dump(data, writer);
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

    public String getEndpoint() {
        return endpoint;
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : authToken;
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
