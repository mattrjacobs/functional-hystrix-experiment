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

import java.util.concurrent.RejectedExecutionException;

public class ExecutionMetricsFilter<Req, Resp> implements Filter<Req, Resp> {
    private final ExecutionMetrics metrics;

    public ExecutionMetricsFilter(ExecutionMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public Service<Req, Resp> apply(Service<Req, Resp> serviceToWrap) {
        return request -> {
            Long startTime = System.currentTimeMillis();
            return serviceToWrap.
                    invoke(request).
                    doOnCompleted(() -> metrics.markSuccess(System.currentTimeMillis() - startTime)).
                    doOnError(ex -> {
                        if (ex instanceof RejectedExecutionException) {
                            metrics.markConcurrencyBoundExceeded(System.currentTimeMillis() - startTime);
                        } else if (ex instanceof CircuitBreakerFilter.CircuitOpenException) {
                            metrics.markShortCircuited(System.currentTimeMillis() - startTime);
                        } else {
                            metrics.markFailure(System.currentTimeMillis() - startTime);
                        }
                    });

        };
    }
}
