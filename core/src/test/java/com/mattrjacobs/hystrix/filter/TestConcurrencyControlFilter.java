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

import com.mattrjacobs.hystrix.Service;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.RejectedExecutionException;

public class TestConcurrencyControlFilter {

    Observable<Integer> success;
    Service<Integer, Integer> service;

    @Before
    public void init() {
        success = Observable.defer(() -> Observable.just(1, 2, 3, 4));

        service = latencyToAdd -> Observable.defer(() -> {
            try {
                Thread.sleep(latencyToAdd);
                return success;
            } catch (InterruptedException ex) {
                return Observable.error(ex);
            }
        }).subscribeOn(Schedulers.io());
    }

    @Test
    public void testSingleSuccessWithConcurrencyOfThree() {
        ConcurrencyControlFilter<Integer, Integer> concurrencyControlFilter = new ConcurrencyControlFilter<>(3);
        Observable<Integer> response = concurrencyControlFilter.apply(service).invoke(100);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
        sub.assertCompleted();
    }

    @Test
    public void testThreeSuccessesWithConcurrencyOfThree() {
        ConcurrencyControlFilter<Integer, Integer> concurrencyControlFilter = new ConcurrencyControlFilter<>(3);
        Observable<Integer> response1 = concurrencyControlFilter.apply(service).invoke(100);
        Observable<Integer> response2 = concurrencyControlFilter.apply(service).invoke(100);
        Observable<Integer> response3 = concurrencyControlFilter.apply(service).invoke(100);

        TestSubscriber<Integer> sub1 = new TestSubscriber<>();
        response1.subscribe(sub1);

        TestSubscriber<Integer> sub2 = new TestSubscriber<>();
        response2.subscribe(sub2);

        TestSubscriber<Integer> sub3 = new TestSubscriber<>();
        response3.subscribe(sub3);

        sub1.awaitTerminalEvent();
        sub2.awaitTerminalEvent();
        sub3.awaitTerminalEvent();
        sub1.assertValues(1, 2, 3, 4);
        sub1.assertNoErrors();
        sub1.assertCompleted();
        sub2.assertValues(1, 2, 3, 4);
        sub2.assertNoErrors();
        sub2.assertCompleted();
        sub3.assertValues(1, 2, 3, 4);
        sub3.assertNoErrors();
        sub3.assertCompleted();
    }

    @Test
    public void testFourRequestsWithConcurrencyOfThree() {
        ConcurrencyControlFilter<Integer, Integer> concurrencyControlFilter = new ConcurrencyControlFilter<>(3);
        Observable<Integer> response1 = concurrencyControlFilter.apply(service).invoke(100);
        Observable<Integer> response2 = concurrencyControlFilter.apply(service).invoke(100);
        Observable<Integer> response3 = concurrencyControlFilter.apply(service).invoke(100);
        Observable<Integer> response4 = concurrencyControlFilter.apply(service).invoke(100);

        TestSubscriber<Integer> sub1 = new TestSubscriber<>();
        response1.subscribe(sub1);

        TestSubscriber<Integer> sub2 = new TestSubscriber<>();
        response2.subscribe(sub2);

        TestSubscriber<Integer> sub3 = new TestSubscriber<>();
        response3.subscribe(sub3);

        TestSubscriber<Integer> sub4 = new TestSubscriber<>();
        response4.subscribe(sub4);

        sub1.awaitTerminalEvent();
        sub2.awaitTerminalEvent();
        sub3.awaitTerminalEvent();
        sub4.awaitTerminalEvent();
        sub1.assertValues(1, 2, 3, 4);
        sub1.assertNoErrors();
        sub1.assertCompleted();
        sub2.assertValues(1, 2, 3, 4);
        sub2.assertNoErrors();
        sub2.assertCompleted();
        sub3.assertValues(1, 2, 3, 4);
        sub3.assertNoErrors();
        sub3.assertCompleted();
        sub4.assertError(RejectedExecutionException.class);
        sub4.assertNoValues();
        sub4.assertNotCompleted();
    }
}
