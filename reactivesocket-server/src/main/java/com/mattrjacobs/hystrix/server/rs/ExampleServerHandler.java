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

package com.mattrjacobs.hystrix.server.rs;

import io.reactivesocket.Frame;
import io.reactivesocket.Payload;
import io.reactivesocket.RequestHandler;
import org.reactivestreams.Publisher;
import rx.Observable;
import rx.RxReactiveStreams;
import uk.co.real_logic.agrona.BitUtil;

import java.nio.ByteBuffer;


public class ExampleServerHandler extends RequestHandler {
    @Override
    public Publisher<Payload> handleRequestResponse(Payload payload) {
        ByteBuffer data = payload.getData();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.remaining(); i += BitUtil.SIZE_OF_CHAR) {
            char c = data.getChar(i);
            sb.append(c);
        }
        String responseString = "Hello " + sb.toString();

        ByteBuffer responseData = ByteBuffer.allocate(responseString.length() * BitUtil.SIZE_OF_CHAR);
        for (char c: responseString.toCharArray()) {
            responseData.putChar(c);
        }

        Payload p = new Payload() {
            @Override
            public ByteBuffer getData() {
                responseData.rewind();
                return responseData;
            }

            @Override
            public ByteBuffer getMetadata() {
                return Frame.NULL_BYTEBUFFER;
            }
        };

        return RxReactiveStreams.toPublisher(Observable.just(p).doOnError(Throwable::printStackTrace));
    }

    @Override
    public Publisher<Payload> handleRequestStream(Payload payload) {
        return null;
    }

    @Override
    public Publisher<Payload> handleSubscription(Payload payload) {
        return null;
    }

    @Override
    public Publisher<Void> handleFireAndForget(Payload payload) {
        return null;
    }

    @Override
    public Publisher<Payload> handleChannel(Payload initialPayload, Publisher<Payload> inputs) {
        return null;
    }

    @Override
    public Publisher<Void> handleMetadataPush(Payload payload) {
        return null;
    }
}
