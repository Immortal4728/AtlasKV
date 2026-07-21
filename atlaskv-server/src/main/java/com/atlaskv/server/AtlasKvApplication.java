package com.atlaskv.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * AtlasKV Spring Boot application entry point.
 * Boots the Raft consensus node with REST API and cluster management.
 */
@SpringBootApplication
public class AtlasKvApplication {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasKvApplication.class);

    /**
     * Main entry point for AtlasKV server.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AtlasKvApplication.class, args);
        Environment env = ctx.getEnvironment();

        String port = env.getProperty("server.port", "8081");
        String nodeId = env.getProperty("atlaskv.node.id", "node1");
        String grpcPort = env.getProperty("atlaskv.server.grpc-port", "50051");

        LOG.info("\n" +
                "=============================================================\n" +
                "    _  _   _           _  ____   __                         \n" +
                "   / \\| |_| | __ _ ___| |/ /\\ \\ / /                         \n" +
                "  / _ \\ __| |/ _` / __| ' /  \\ V /                          \n" +
                " / ___ \\ |_| | (_| \\__ \\ . \\   | |                           \n" +
                "/_/   \\_\\__|_|\\__,_|___/_|\\_\\  |_|  v1.0.0                  \n" +
                "=============================================================\n" +
                "  AtlasKV Raft Node Started Successfully!\n" +
                "  Node ID      : {}\n" +
                "  REST API     : http://0.0.0.0:{}\n" +
                "  gRPC Port    : {}\n" +
                "  Swagger Docs : http://0.0.0.0:{}/swagger-ui.html\n" +
                "  Actuator     : http://0.0.0.0:{}/actuator/health\n" +
                "=============================================================",
                nodeId, port, grpcPort, port, port);
    }
}
