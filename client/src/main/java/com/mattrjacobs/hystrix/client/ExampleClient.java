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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;

import java.nio.charset.Charset;

public class ExampleClient {
    private final HttpClient<ByteBuf, ByteBuf> httpClient;

    public ExampleClient(String host, int port) {
        System.out.println("Creating HTTP Client on : " + host + " : " + port);
        httpClient = RxNetty.createHttpClient(host, port);
    }

    public Observable<String> makeCall() {
        HttpClientRequest<ByteBuf> req = HttpClientRequest.create(HttpVersion.HTTP_1_1, HttpMethod.GET, "/example");
        return httpClient.submit(req).
                flatMap(HttpClientResponse<ByteBuf>::getContent).
                map(bb -> bb.toString(Charset.defaultCharset()));
    }
}
