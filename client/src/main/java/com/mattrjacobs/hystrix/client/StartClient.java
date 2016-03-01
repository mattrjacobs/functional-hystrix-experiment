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
package com.mattrjacobs.hystrix.client;

import com.mattrjacobs.hystrix.CircuitBreaker;
import com.mattrjacobs.hystrix.ExecutionMetrics;
import com.mattrjacobs.hystrix.Service;
import com.mattrjacobs.hystrix.filter.CircuitBreakerFilter;
import com.mattrjacobs.hystrix.filter.ConcurrencyControlFilter;
import com.mattrjacobs.hystrix.filter.ExecutionMetricsFilter;
import com.mattrjacobs.hystrix.filter.FallbackFilter;
import rx.Observable;
import rx.Subscriber;

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
                System.out.println(Thread.currentThread().getName() + " : SUCCESS[" + latency + "ms]");
            }

            @Override
            public void markFailure(long latency) {
                System.out.println(Thread.currentThread().getName() + " : FAILURE[" + latency + "ms]");
            }

            @Override
            public void markConcurrencyBoundExceeded(long latency) {
                System.out.println(Thread.currentThread().getName() + " : CONCURRENCY-REJECTED[" + latency + "ms]");
            }

            @Override
            public void markShortCircuited(long latency) {
                System.out.println(Thread.currentThread().getName() + " : SHORT-CIRCUITED[" + latency + "ms]");
            }
        };

        Random r = new Random();

        CircuitBreaker circuitBreaker = new CircuitBreaker() {
            @Override
            public boolean shouldAllow() {
                return r.nextBoolean();
            }

            @Override
            public void markSuccess() {

            }
        };

        ExecutionMetricsFilter<Void, String> executionMetricsFilter = new ExecutionMetricsFilter<>(metrics);
        CircuitBreakerFilter<Void, String> circuitBreakerFilter = new CircuitBreakerFilter<>(circuitBreaker);
        FallbackFilter<Void, String> fallbackFilter = new FallbackFilter<>(Observable.just("FALLBACK"));
        ConcurrencyControlFilter<Void, String> concurrencyControlFilter = new ConcurrencyControlFilter<>(4);

        Service<Void, String> rawService = request -> client.makeCall();

        Service<Void, String> hystrixService = fallbackFilter.apply(
                executionMetricsFilter.apply(
                        concurrencyControlFilter.apply(
                                circuitBreakerFilter.apply(rawService)
                        )
                )
        );

        CountDownLatch latch = new CountDownLatch(1);

        int NUM_CONCURRENT_CALLS = 10;
        List<Observable<String>> responses = new ArrayList<>();

        for (int i = 0; i < NUM_CONCURRENT_CALLS; i++) {
            responses.add(hystrixService.invoke(null));
        }

        Observable.merge(responses).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                //System.out.println("Http Client OnCompleted!");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                //System.out.println("Http Client OnError!");
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
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {

        }
        //System.out.println("Done waiting for HTTP Client");
    }
}
