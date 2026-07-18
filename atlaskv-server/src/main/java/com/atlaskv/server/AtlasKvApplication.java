package com.atlaskv.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AtlasKV Spring Boot application entry point.
 * Boots the Raft consensus node with REST API and cluster management.
 */
@SpringBootApplication
public class AtlasKvApplication {

    /**
     * Main entry point for AtlasKV server.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AtlasKvApplication.class, args);
    }
}
