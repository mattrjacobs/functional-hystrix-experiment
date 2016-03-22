package com.mattrjacobs.hystrix.server.grpc;

public class StartServer {

    public static void main(String[] args) {
        final int PORT = 11111;
        ExampleServer server = new ExampleServer(PORT);
        System.out.println("Starting server on port : " + PORT);
        server.start();
        try {
            server.blockUntilShutdown();
        } catch (InterruptedException ex) {
            System.out.println("Main thread got Interrupted Ex : " + ex);
        }
    }
}
