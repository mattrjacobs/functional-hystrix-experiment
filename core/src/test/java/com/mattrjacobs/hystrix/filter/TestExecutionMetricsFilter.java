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
package com.mattrjacobs.hystrix.filter;

import com.mattrjacobs.hystrix.ExecutionMetrics;
import com.mattrjacobs.hystrix.Service;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.RejectedExecutionException;

public class TestExecutionMetricsFilter {

    class Request {
        final Throwable exceptionToFail;
        final int latencyToAdd;

        public Request(Throwable exceptionToFail, int latencyToAdd) {
            this.exceptionToFail = exceptionToFail;
            this.latencyToAdd = latencyToAdd;
        }
    }

    @Mock
    ExecutionMetrics mockExecutionMetrics;

    Observable<Integer> success;

    Service<Request, Integer> service;

    private Observable<Integer> failure(Throwable ex) {
        return Observable.defer(() -> Observable.concat(
                Observable.just(1, 2, 3),
                Observable.error(ex)));
    }

    private final static BaseMatcher<Long> latencyMatcher = new BaseMatcher<Long>() {
        @Override
        public boolean matches(Object o) {
            Long l = (Long) o;
            System.out.println("Latency : " + l);
            return l > 100;
        }

        @Override
        public void describeTo(Description description) {

        }
    };

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        success = Observable.defer(() -> Observable.just(1, 2, 3, 4));

        service = request -> Observable.defer(() -> {
            try {
                Thread.sleep(request.latencyToAdd);
                if (request.exceptionToFail != null) {
                    return failure(request.exceptionToFail);
                } else {
                    return success;
                }
            } catch (InterruptedException ex) {
                return Observable.error(ex);
            }

        }).subscribeOn(Schedulers.io());
    }

    @Test
    public void testSuccess() {
        ExecutionMetricsFilter<Request, Integer> executionMetricsFilter =
                new ExecutionMetricsFilter<>(mockExecutionMetrics);
        Observable<Integer> response =
                executionMetricsFilter.apply(service).invoke(new Request(null, 100));

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
        sub.assertCompleted();

        Mockito.verify(mockExecutionMetrics, Mockito.times(1)).markSuccess(Mockito.longThat(latencyMatcher));
        Mockito.verifyNoMoreInteractions(mockExecutionMetrics);
    }

    @Test
    public void testShortCircuited() {
        ExecutionMetricsFilter<Request, Integer> executionMetricsFilter =
                new ExecutionMetricsFilter<>(mockExecutionMetrics);
        Observable<Integer> response =
                executionMetricsFilter.apply(service).invoke(new Request(new CircuitBreakerFilter.CircuitOpenException("unit test"), 100));

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3);
        sub.assertError(RuntimeException.class);
        sub.assertNotCompleted();

        Mockito.verify(mockExecutionMetrics, Mockito.times(1)).markShortCircuited(Mockito.longThat(latencyMatcher));
        Mockito.verifyNoMoreInteractions(mockExecutionMetrics);
    }

    @Test
    public void testConcurrencyBoundExceeded() {
        ExecutionMetricsFilter<Request, Integer> executionMetricsFilter =
                new ExecutionMetricsFilter<>(mockExecutionMetrics);
        Observable<Integer> response =
                executionMetricsFilter.apply(service).invoke(new Request(new RejectedExecutionException("unit test"), 100));

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3);
        sub.assertError(RuntimeException.class);
        sub.assertNotCompleted();

        Mockito.verify(mockExecutionMetrics, Mockito.times(1)).markConcurrencyBoundExceeded(Mockito.longThat(latencyMatcher));
        Mockito.verifyNoMoreInteractions(mockExecutionMetrics);
    }

    @Test
    public void testFailure() {
        ExecutionMetricsFilter<Request, Integer> executionMetricsFilter =
                new ExecutionMetricsFilter<>(mockExecutionMetrics);
        Observable<Integer> response =
                executionMetricsFilter.apply(service).invoke(new Request(new RuntimeException("unit test"), 100));

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3);
        sub.assertError(RuntimeException.class);
        sub.assertNotCompleted();

        Mockito.verify(mockExecutionMetrics, Mockito.times(1)).markFailure(Mockito.longThat(latencyMatcher));
        Mockito.verifyNoMoreInteractions(mockExecutionMetrics);
    }

}
