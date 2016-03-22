package com.mattrjacobs.hystrix.client.grpc;

import com.mattrjacobs.hystrix.grpc.HelloReply;

public class StartClient {
    public static void main(String[] args) {
        ExampleClient client = new ExampleClient("localhost", 11111);

        HelloReply reply = client.sync();
        System.out.println("REPLY : " + reply);
    }
}
