/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mattrjacobs.hystrix.client.rxnetty;

import com.mattrjacobs.hystrix.CircuitBreaker;
import com.mattrjacobs.hystrix.ExecutionMetrics;
import com.mattrjacobs.hystrix.FallbackMetrics;
import com.mattrjacobs.hystrix.Service;
import com.mattrjacobs.hystrix.filter.CircuitBreakerFilter;
import com.mattrjacobs.hystrix.filter.ConcurrencyControlFilter;
import com.mattrjacobs.hystrix.filter.ExecutionMetricsFilter;
import com.mattrjacobs.hystrix.filter.FallbackFilter;
import com.mattrjacobs.hystrix.filter.FallbackMetricsFilter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StartClient {
    public static void main(String[] args) {
        ExampleClient client = new ExampleClient("localhost", 11111);

        ExecutionMetrics metrics = new ExecutionMetrics() {
            @Override
            public void markSuccess(long latency) {
                System.out.println("ExecutionSuccess[" + latency + "ms]");
            }

            @Override
            public void markFailure(long latency) {
                System.out.println("ExecutionFailure[" + latency + "ms]");
            }

            @Override
            public void markConcurrencyBoundExceeded(long latency) {
                System.out.println("ExecutionRejected[" + latency + "ms]");
            }

            @Override
            public void markShortCircuited(long latency) {
                System.out.println("ExecutionShortCircuited[" + latency + "ms]");
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
                //return (r.nextDouble() > 0.01);
            }

            @Override
            public void markSuccess() {

            }
        };

        FallbackMetricsFilter<Void, String> fallbackMetricsFilter = new FallbackMetricsFilter<>(fallbackMetrics);
        ConcurrencyControlFilter<Void, String> fallbackConcurrencyControlFilter = new ConcurrencyControlFilter<>(1);

        Service<Void, String> fallbackService = request -> Observable.defer(() -> Observable.just("FALLBACK"));
        Service<Void, String> hystrixFallbackService = fallbackMetricsFilter.apply(
                fallbackConcurrencyControlFilter.apply(fallbackService)
        );

        ExecutionMetricsFilter<Void, String> executionMetricsFilter = new ExecutionMetricsFilter<>(metrics);
        CircuitBreakerFilter<Void, String> circuitBreakerFilter = new CircuitBreakerFilter<>(circuitBreaker);
        FallbackFilter<Void, String> fallbackFilter = new FallbackFilter<>(hystrixFallbackService);
        ConcurrencyControlFilter<Void, String> concurrencyControlFilter = new ConcurrencyControlFilter<>(999);

        Service<Void, String> rawService = request -> client.makeCall();

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
                            Thread.sleep(1);
                        } catch (Throwable ex) {
                            return Observable.error(ex);
                        }
                        return  hystrixService.invoke(null).onErrorResumeNext(ex -> {
                            ex.printStackTrace();
                            return Observable.just("FALLBACK FAILED!!!!!");
                        });
                    }));

        }

        Subscription subscription = Observable.amb(responses).subscribe(new Subscriber<String>() {
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

        //System.out.println("Starting the HTTP Client await...");
        try {
            boolean await = latch.await(1000, TimeUnit.MILLISECONDS);
            System.out.println("amb received value + terminal : " + await);
        } catch (InterruptedException ex) {

        }
        //System.out.println("Done waiting for HTTP Client");
    }
}
