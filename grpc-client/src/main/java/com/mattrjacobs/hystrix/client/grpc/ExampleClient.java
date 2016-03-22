package com.mattrjacobs.hystrix.client.grpc;

import com.mattrjacobs.hystrix.grpc.GreeterGrpc;
import com.mattrjacobs.hystrix.grpc.HelloReply;
import com.mattrjacobs.hystrix.grpc.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ExampleClient {
    private final GreeterGrpc.GreeterStub asyncStub;

    public ExampleClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        this.asyncStub = GreeterGrpc.newStub(channel);
    }

    public void async(StreamObserver<HelloReply> responseObserver) {
        HelloRequest req = HelloRequest.newBuilder().setName("Foo").build();
        asyncStub.sayHello(req, responseObserver);
    }
}
