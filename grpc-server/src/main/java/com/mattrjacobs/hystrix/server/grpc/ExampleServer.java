package com.mattrjacobs.hystrix.server.grpc;

import com.mattrjacobs.hystrix.grpc.GreeterGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ExampleServer {
    private final Server server;
    private final int port;

    public ExampleServer(int port) {
        server = ServerBuilder
                .forPort(port)
                .addService(GreeterGrpc.bindService(new GreeterImpl()))
                .build();
        this.port = port;
    }

    void start() {
        try {
            server.start();
        } catch (IOException ioe) {
            System.out.println("Server did not start!");
            ioe.printStackTrace();
        }

        System.out.println("Server started up on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ExampleServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        server.shutdown();
    }

    void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
