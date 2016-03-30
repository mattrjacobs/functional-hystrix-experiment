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
package com.mattrjacobs.hystrix.client.rs;

import io.netty.handler.logging.LogLevel;
import io.reactivesocket.ConnectionSetupPayload;
import io.reactivesocket.Frame;
import io.reactivesocket.Payload;
import io.reactivesocket.ReactiveSocket;
import io.reactivesocket.websocket.rxnetty.WebSocketDuplexConnection;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import org.reactivestreams.Publisher;
import rx.Observable;
import rx.RxReactiveStreams;
import uk.co.real_logic.agrona.BitUtil;

import java.nio.ByteBuffer;

public class ExampleClient {
    private final ReactiveSocket reactiveSocket;

    public ExampleClient(String host, int port) {
        Observable<WebSocketConnection> wsConnection = HttpClient.newClient(host, port)
                //.enableWireLogging(LogLevel.INFO)
                .createGet("/rs")
                .requestWebSocketUpgrade()
                .flatMap(WebSocketResponse::getWebSocketConnection);

        Publisher<WebSocketDuplexConnection> connectionPublisher =
                WebSocketDuplexConnection.create(RxReactiveStreams.toPublisher(wsConnection));

        reactiveSocket = RxReactiveStreams
                .toObservable(connectionPublisher)
                .map(w -> ReactiveSocket.fromClientConnection(w, ConnectionSetupPayload.create("UTF-8", "UTF-8")))
                .toBlocking()
                .single();

        reactiveSocket.startAndWait();
    }

    public Observable<String> makeCall() {
        String request = "Matt";
        ByteBuffer b = ByteBuffer.allocate(request.length() * BitUtil.SIZE_OF_CHAR);
        for (char c: request.toCharArray()) {
            b.putChar(c);
        }

        Payload p = new Payload() {
            @Override
            public ByteBuffer getData() {
                b.rewind();
                return b;
            }

            @Override
            public ByteBuffer getMetadata() {
                return Frame.NULL_BYTEBUFFER;
            }
        };

        Observable<Payload> payloadObservable = RxReactiveStreams.toObservable(reactiveSocket.requestResponse(p));
        return payloadObservable
                .map(response -> {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < response.getData().remaining(); i += BitUtil.SIZE_OF_CHAR) {
                        char c = response.getData().getChar(i);
                        sb.append(c);
                    }
                    return sb.toString();
                })
                .doOnNext(r -> System.out.println("Got from server => " + r))
                .doOnError(Throwable::printStackTrace);
    }
}
