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

public class TestFallbackFilter {

    Observable<Integer> success;
    Observable<Integer> failure;
    Observable<Integer> fallbackResume;

    Service<Boolean, Integer> service;

    @Before
    public void init() {
        success = Observable.defer(() -> Observable.just(1, 2, 3, 4));
        failure = Observable.defer(() -> Observable.concat(
                Observable.just(1, 2, 3),
                Observable.error(new RuntimeException("runtime exception"))));

        service = shouldFail -> (shouldFail ? failure : success).subscribeOn(Schedulers.computation());

        fallbackResume = Observable.defer(() -> Observable.just(4));
    }

    @Test
    public void testFallbackAppliedToSuccess() {
        FallbackFilter<Boolean, Integer> fallbackFilter = new FallbackFilter<>(fallbackResume);
        Observable<Integer> response =
                fallbackFilter.apply(service).invoke(false);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
        sub.assertCompleted();
    }

    @Test
    public void testFallbackAppliedToFailure() {
        FallbackFilter<Boolean, Integer> fallbackFilter = new FallbackFilter<>(fallbackResume);
        Observable<Integer> response =
                fallbackFilter.apply(service).invoke(true);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
        sub.assertCompleted();
    }

    @Test
    public void testNoFallbackAppliedToSuccess() {
        Observable<Integer> response = service.invoke(false);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
        sub.assertCompleted();
    }

    @Test
    public void testNoFallbackAppliedToFailure() {
        Observable<Integer> response = service.invoke(true);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3);
        sub.assertError(RuntimeException.class);
        sub.assertNotCompleted();
    }
}
