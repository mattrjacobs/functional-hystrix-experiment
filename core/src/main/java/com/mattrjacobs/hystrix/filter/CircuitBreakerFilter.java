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
import rx.Observable;

public class CircuitBreakerFilter<Req, Resp> implements Filter<Req, Resp> {
    private final CircuitBreaker circuitBreaker;

    private static final RuntimeException CIRCUIT_BREAKER_OPEN_EXCEPTION =
            new CircuitOpenException("Circuit-breaker is OPEN and denying requests");

    public CircuitBreakerFilter(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Service<Req, Resp> apply(Service<Req, Resp> serviceToWrap) {
        return request -> {
            if (circuitBreaker.shouldAllow()) {
                return serviceToWrap.
                        invoke(request).
                        doOnCompleted(circuitBreaker::markSuccess);

            } else {
                return Observable.error(CIRCUIT_BREAKER_OPEN_EXCEPTION);
            }
        };
    }

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }
}
