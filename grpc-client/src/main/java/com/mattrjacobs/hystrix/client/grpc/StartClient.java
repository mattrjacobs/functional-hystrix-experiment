package com.mattrjacobs.hystrix.client.grpc;

import com.mattrjacobs.hystrix.CircuitBreaker;
import com.mattrjacobs.hystrix.ExecutionMetrics;
import com.mattrjacobs.hystrix.FallbackMetrics;
import com.mattrjacobs.hystrix.Service;
import com.mattrjacobs.hystrix.filter.CircuitBreakerFilter;
import com.mattrjacobs.hystrix.filter.ConcurrencyControlFilter;
import com.mattrjacobs.hystrix.filter.ExecutionMetricsFilter;
import com.mattrjacobs.hystrix.filter.FallbackFilter;
import com.mattrjacobs.hystrix.filter.FallbackMetricsFilter;
import com.mattrjacobs.hystrix.grpc.HelloRequest;
import com.mattrjacobs.hystrix.grpc.HelloReply;
import io.grpc.ClientCall;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StartClient {
    public static void main(String[] args) {
        ExampleClient client = new ExampleClient("localhost", 11112);

        ExecutionMetrics metrics = new ExecutionMetrics() {
            @Override
            public void markSuccess(long latency) {
                System.out.println(Thread.currentThread().getName() + ": ExecutionSuccess[" + latency + "ms]");
            }

            @Override
            public void markFailure(long latency) {
                System.out.println(Thread.currentThread().getName() + ": ExecutionFailure[" + latency + "ms]");
            }

            @Override
            public void markConcurrencyBoundExceeded(long latency) {
                System.out.println(Thread.currentThread().getName() + ": ExecutionRejected[" + latency + "ms]");
            }

            @Override
            public void markShortCircuited(long latency) {
                System.out.println(Thread.currentThread().getName() + ": ExecutionShortCircuited[" + latency + "ms]");
            }
        };

        FallbackMetrics fallbackMetrics = new FallbackMetrics() {
            @Override
            public void markSuccess(long latency) {
                System.out.println("*FallbackSuccess[" + latency + "ms]");
            }

            @Override
            public void markFailure(long latency) {
                System.out.println("*FallbackFailure[" + latency + "ms]");
            }

            @Override
            public void markConcurrencyBoundExceeded(long latency) {
                System.out.println("*FallbackRejected[" + latency + "ms]");
            }
        };

        Random r = new Random();

        CircuitBreaker circuitBreaker = new CircuitBreaker() {
            @Override
            public boolean shouldAllow() {
                return true;
                //return (r.nextDouble() > 0.1);
            }

            @Override
            public void markSuccess() {

            }
        };

        FallbackMetricsFilter<Void, String> fallbackMetricsFilter = new FallbackMetricsFilter<>(fallbackMetrics);
        ConcurrencyControlFilter<Void, String> fallbackConcurrencyControlFilter = new ConcurrencyControlFilter<>(10);

        Service<Void, String> fallbackService = request -> Observable.defer(() -> Observable.just("FALLBACK"));
        Service<Void, String> hystrixFallbackService = fallbackMetricsFilter.apply(
                fallbackConcurrencyControlFilter.apply(fallbackService)
        );

        ExecutionMetricsFilter<Void, String> executionMetricsFilter = new ExecutionMetricsFilter<>(metrics);
        CircuitBreakerFilter<Void, String> circuitBreakerFilter = new CircuitBreakerFilter<>(circuitBreaker);
        FallbackFilter<Void, String> fallbackFilter = new FallbackFilter<>(hystrixFallbackService);
        ConcurrencyControlFilter<Void, String> concurrencyControlFilter = new ConcurrencyControlFilter<>(20);

        HelloRequest helloRequest = HelloRequest.newBuilder().setName("Matt").build();


        Service<Void, String> rawService = request -> Observable.defer(() -> Observable.create(subscriber -> {
            ClientCall<HelloRequest, HelloReply> clientCall = client.async();

            final StreamObserver<HelloReply> responseObserver = new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply helloReply) {
                    try {
                        //simulate processing time of each chunk of data
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println("Interrupted : " + ex);
                    }
                    subscriber.onNext(helloReply.getMessage());
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    subscriber.onError(throwable);
                }

                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }
            };

            ClientCalls.asyncServerStreamingCall(clientCall, helloRequest, responseObserver);

            //I think this should properly wire up backpressure
            //subscriber.setProducer(n -> clientCall.request((int) n));

            subscriber.add(new Subscription() {
                private AtomicBoolean isUnsubscribed = new AtomicBoolean(false);

                @Override
                public void unsubscribe() {
                    //System.out.println("Cancelling client call : " + clientCall + " at : " + System.currentTimeMillis());
                    clientCall.cancel();
                    isUnsubscribed.set(true);
                }

                @Override
                public boolean isUnsubscribed() {
                    return isUnsubscribed.get();
                }
            });

        }));

        Service<Void, String> hystrixService = fallbackFilter.apply(
                executionMetricsFilter.apply(
                        concurrencyControlFilter.apply(
                                circuitBreakerFilter.apply(rawService)
                        )
                )
        );

        CountDownLatch latch = new CountDownLatch(1);

        int NUM_CONCURRENT_CALLS = 20;
        List<Observable<String>> responses = new ArrayList<>();

        for (int i = 0; i < NUM_CONCURRENT_CALLS; i++) {
            responses.add(
                    Observable.defer(() -> {
                        try {
                            Thread.sleep(10);
                        } catch (Throwable ex) {
                            return Observable.error(ex);
                        }
                        return hystrixService.invoke(null).onErrorResumeNext(ex -> {
                            ex.printStackTrace();
                            return Observable.just("FALLBACK FAILED!!!!!");
                        });
                    }));

        }

        Observable.amb(responses).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                System.out.println("Http Client OnCompleted!");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Http Client OnError!");
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String s) {
                System.out.println("Http Client OnNext : " + s);
            }
        });

        try {
            boolean await = latch.await(1000, TimeUnit.MILLISECONDS);
            System.out.println("amb Received value and terminal = " + await);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted Exception : " + ex);
        }
    }
}
