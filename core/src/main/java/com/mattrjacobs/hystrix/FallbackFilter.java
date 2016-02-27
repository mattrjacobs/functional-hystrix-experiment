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

package com.mattrjacobs.hystrix;

import rx.Observable;

public class FallbackFilter<Req, Resp> implements Filter<Req, Resp> {
    private final Observable<Resp> resumeWith;

    public FallbackFilter(Observable<Resp> resumeWith) {
        this.resumeWith = resumeWith;
    }

    @Override
    public Service<Req, Resp> apply(Service<Req, Resp> serviceToWrap) {
        return new Service<Req, Resp>() {
            @Override
            public Observable<Resp> invoke(Req request) {
                return serviceToWrap.invoke(request).onErrorResumeNext(resumeWith);
            }
        };
    }
}
