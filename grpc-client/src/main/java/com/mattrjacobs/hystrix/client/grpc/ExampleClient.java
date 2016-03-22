package com.mattrjacobs.hystrix.client.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.mattrjacobs.hystrix.grpc.GreeterGrpc;
import com.mattrjacobs.hystrix.grpc.HelloReply;
import com.mattrjacobs.hystrix.grpc.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ExampleClient {
    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private final GreeterGrpc.GreeterFutureStub futureStub;

    public ExampleClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
        this.futureStub = GreeterGrpc.newFutureStub(channel);
    }

    public HelloReply sync() {
        HelloRequest req = HelloRequest.newBuilder().setName("Foo").build();
        return blockingStub.sayHello(req);
    }

    public ListenableFuture<HelloReply> async() {
        HelloRequest req = HelloRequest.newBuilder().setName("Foo").build();
        return futureStub.sayHello(req);
    }
}
