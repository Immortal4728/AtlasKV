package com.atlaskv.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CliConfig.
 */
final class CliConfigTest {

    @Test
    void loadDefaults() {
        Path nonExistent = Path.of("non", "existent", "config.yml");
        CliConfig config = CliConfig.load(nonExistent);

        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getTimeoutSeconds()).isEqualTo(5);
        assertThat(config.getEndpoint()).isNull();
        assertThat(config.getApiKey()).isNull();
        assertThat(config.getAuthType()).isNull();
    }

    @Test
    void loadFromFile(@TempDir Path tmpDir) throws IOException {
        Path configFile = tmpDir.resolve("config.yml");
        String yaml = """
                host: 10.0.0.1
                port: 9090
                timeout: 15
                authentication:
                  type: bearer
                  token: test-token-123
                """;
        Files.writeString(configFile, yaml);

        CliConfig config = CliConfig.load(configFile);

        assertThat(config.getHost()).isEqualTo("10.0.0.1");
        assertThat(config.getPort()).isEqualTo(9090);
        assertThat(config.getTimeoutSeconds()).isEqualTo(15);
        assertThat(config.getAuthType()).isEqualTo("bearer");
        assertThat(config.getAuthToken()).isEqualTo("test-token-123");
        assertThat(config.getApiKey()).isEqualTo("test-token-123");
    }

    @Test
    void loadWithEndpointAndApiKey(@TempDir Path tmpDir) throws IOException {
        Path configFile = tmpDir.resolve("config.yml");
        String yaml = """
                endpoint: https://atlaskv.cloud.dev
                api-key: ak_prod_secret_9999
                timeout: 10
                """;
        Files.writeString(configFile, yaml);

        CliConfig config = CliConfig.load(configFile);

        assertThat(config.getEndpoint()).isEqualTo("https://atlaskv.cloud.dev");
        assertThat(config.getApiKey()).isEqualTo("ak_prod_secret_9999");
        assertThat(config.getAuthToken()).isEqualTo("ak_prod_secret_9999");
        assertThat(config.getAuthType()).isEqualTo("bearer");
        assertThat(config.getTimeoutSeconds()).isEqualTo(10);
    }

    @Test
    void setAndSaveConfig(@TempDir Path tmpDir) throws IOException {
        Path configFile = tmpDir.resolve("config.yml");
        CliConfig config = CliConfig.load(configFile);

        config.set("endpoint", "https://remote.atlaskv.io");
        config.set("api-key", "ak_saved_key_12345");
        config.set("timeout", "20");
        config.save(configFile);

        CliConfig reloaded = CliConfig.load(configFile);
        assertThat(reloaded.getEndpoint()).isEqualTo("https://remote.atlaskv.io");
        assertThat(reloaded.getApiKey()).isEqualTo("ak_saved_key_12345");
        assertThat(reloaded.getTimeoutSeconds()).isEqualTo(20);
    }

    @Test
    void loadBasicAuth(@TempDir Path tmpDir) throws IOException {
        Path configFile = tmpDir.resolve("config.yml");
        String yaml = """
                host: myhost
                port: 7070
                timeout: 10
                authentication:
                  type: basic
                  username: admin
                  password: secret
                """;
        Files.writeString(configFile, yaml);

        CliConfig config = CliConfig.load(configFile);

        assertThat(config.getAuthType()).isEqualTo("basic");
        assertThat(config.getAuthUsername()).isEqualTo("admin");
        assertThat(config.getAuthPassword()).isEqualTo("secret");
    }

    @Test
    void loadPartialConfig(@TempDir Path tmpDir) throws IOException {
        Path configFile = tmpDir.resolve("config.yml");
        String yaml = "port: 3000\n";
        Files.writeString(configFile, yaml);

        CliConfig config = CliConfig.load(configFile);

        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(3000);
        assertThat(config.getTimeoutSeconds()).isEqualTo(5);
    }

    @Test
    void configPathNotNull() {
        assertThat(CliConfig.configPath()).isNotNull();
        assertThat(CliConfig.configPath().toString()).contains(".atlaskv");
    }
}
