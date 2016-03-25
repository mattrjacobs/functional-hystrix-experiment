package com.mattrjacobs.hystrix.client.grpc;

import com.mattrjacobs.hystrix.grpc.HelloReply;
import com.mattrjacobs.hystrix.grpc.HelloRequest;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

public class ExampleClient {
    private final ManagedChannel channel;

    public ExampleClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
    }

    public ClientCall<HelloRequest, HelloReply> async() {
        MethodDescriptor<HelloRequest, HelloReply> descriptor = io.grpc.MethodDescriptor.create(
                MethodDescriptor.MethodType.SERVER_STREAMING,
                MethodDescriptor.generateFullMethodName(
                        "helloworld.Greeter", "SayHello"),
                ProtoUtils.marshaller(com.mattrjacobs.hystrix.grpc.HelloRequest.getDefaultInstance()),
                ProtoUtils.marshaller(com.mattrjacobs.hystrix.grpc.HelloReply.getDefaultInstance()));
        CallOptions callOptions = CallOptions.DEFAULT;
        return channel.newCall(descriptor, callOptions);
    }
}
