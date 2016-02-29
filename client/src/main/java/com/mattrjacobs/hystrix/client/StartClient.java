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

import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StartClient {
    public static void main(String[] args) {
        ExampleClient client = new ExampleClient("localhost", 11111);
        Observable<String> singleResp = client.makeCall();

        CountDownLatch latch = new CountDownLatch(1);

        singleResp.subscribe(new Subscriber<String>() {
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

        System.out.println("Starting the HTTP Client await...");
        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {

        }
        System.out.println("Done waiting for HTTP Client");
    }
}
