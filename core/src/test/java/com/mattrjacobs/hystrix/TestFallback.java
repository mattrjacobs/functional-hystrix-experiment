package com.mattrjacobs.hystrix;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class TestFallback {

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
    }

    @Test
    public void testNoFallbackAppliedToSuccess() {
        Observable<Integer> response = service.invoke(false);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3, 4);
        sub.assertNoErrors();
    }

    @Test
    public void testNoFallbackAppliedToFailure() {
        Observable<Integer> response = service.invoke(true);

        TestSubscriber<Integer> sub = new TestSubscriber<>();
        response.subscribe(sub);
        sub.awaitTerminalEvent();
        sub.assertValues(1, 2, 3);
        sub.assertError(RuntimeException.class);
    }
}
