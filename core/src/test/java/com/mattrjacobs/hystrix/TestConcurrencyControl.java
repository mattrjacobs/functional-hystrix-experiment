package com.mattrjacobs.hystrix;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.RejectedExecutionException;

public class TestConcurrencyControl {

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
        sub2.assertValues(1, 2, 3, 4);
        sub2.assertNoErrors();
        sub3.assertValues(1, 2, 3, 4);
        sub3.assertNoErrors();
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
        sub2.assertValues(1, 2, 3, 4);
        sub2.assertNoErrors();
        sub3.assertValues(1, 2, 3, 4);
        sub3.assertNoErrors();
        sub4.assertError(RejectedExecutionException.class);
        sub4.assertNoValues();
    }
}
