package com.mattrjacobs.hystrix.client.grpc;

import com.mattrjacobs.hystrix.Service;
import com.mattrjacobs.hystrix.grpc.HelloReply;
import io.grpc.stub.StreamObserver;
import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StartClient {
    public static void main(String[] args) {
        ExampleClient client = new ExampleClient("localhost", 11111);


        Service<Void, String> rawService = request -> Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                final StreamObserver<HelloReply> responseObserver = new StreamObserver<HelloReply>() {
                    @Override
                    public void onNext(HelloReply helloReply) {
                        subscriber.onNext(helloReply.getMessage());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }
                };

                client.async(responseObserver);
            }
        });

        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> outerObservable = rawService.invoke(null);

        outerObservable.subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                System.out.println("Outer OnCompleted");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Outer OnError : " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onNext(String s) {
                System.out.println("Outer OnNext : " + s);
            }
        });

        try {
            boolean await = latch.await(1000, TimeUnit.MILLISECONDS);
            System.out.println("Received value = " + await);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted Exception : " + ex);
        }
    }
}
