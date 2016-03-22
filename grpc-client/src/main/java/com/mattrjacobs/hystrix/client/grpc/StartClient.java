package com.mattrjacobs.hystrix.client.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.mattrjacobs.hystrix.grpc.HelloReply;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StartClient {
    public static void main(String[] args) {
        ExampleClient client = new ExampleClient("localhost", 11111);

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<HelloReply> responseObserver = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply helloReply) {
                System.out.println("OnNext : " + helloReply.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("OnError : " + throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("OnCompleted");
                latch.countDown();
            }
        };

        StreamObserver<HelloReply> reply = client.async(responseObserver);

        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted Exception : " + ex);
        }
    }
}
