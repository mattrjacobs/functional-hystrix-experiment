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

import com.mattrjacobs.hystrix.CircuitBreaker;
import com.mattrjacobs.hystrix.Service;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class TestCircuitBreakerFilter {

    Observable<Integer> success;
    Observable<Integer> failure;
    Service<Boolean, Integer> service;

    @Mock CircuitBreaker mockCircuitBreaker;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        success = Observable.defer(() -> Observable.just(1, 2, 3, 4));
        failure = Observable.defer(() -> Observable.concat(
                Observable.just(1, 2, 3),
                Observable.error(new RuntimeException("runtime exception"))));

        service = shouldFail -> (shouldFail ? failure : success).subscribeOn(Schedulers.computation());
    }

    @Test
    public void testCircuitBreakerOpen() {
        Mockito.when(mockCircuitBreaker.shouldAllow()).thenReturn(false);

        CircuitBreakerFilter<Boolean, Integer> circuitBreakerFilter = new CircuitBreakerFilter<>(mockCircuitBreaker);
        Observable<Integer> response = circuitBreakerFilter.apply(service).invoke(false);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertNoValues();
        sub.assertError(CircuitBreakerFilter.CircuitOpenException.class);

        Mockito.verify(mockCircuitBreaker, Mockito.times(1)).shouldAllow();
        Mockito.verifyNoMoreInteractions(mockCircuitBreaker);
    }

    @Test
    public void testCircuitBreakerClosedAndSucceeds() {
        Mockito.when(mockCircuitBreaker.shouldAllow()).thenReturn(true);

        CircuitBreakerFilter<Boolean, Integer> circuitBreakerFilter = new CircuitBreakerFilter<>(mockCircuitBreaker);
        Observable<Integer> response = circuitBreakerFilter.apply(service).invoke(false);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
        sub.assertCompleted();

        Mockito.verify(mockCircuitBreaker, Mockito.times(1)).shouldAllow();
        Mockito.verify(mockCircuitBreaker, Mockito.times(1)).markSuccess();
        Mockito.verifyNoMoreInteractions(mockCircuitBreaker);
    }

    @Test
    public void testCircuitBreakerClosedAndFails() {
        Mockito.when(mockCircuitBreaker.shouldAllow()).thenReturn(true);

        CircuitBreakerFilter<Boolean, Integer> circuitBreakerFilter = new CircuitBreakerFilter<>(mockCircuitBreaker);
        Observable<Integer> response = circuitBreakerFilter.apply(service).invoke(true); //induces failure

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3);
        sub.assertError(RuntimeException.class);

        Mockito.verify(mockCircuitBreaker, Mockito.times(1)).shouldAllow();
        Mockito.verify(mockCircuitBreaker, Mockito.times(0)).markSuccess();
        Mockito.verifyNoMoreInteractions(mockCircuitBreaker);
    }
}
