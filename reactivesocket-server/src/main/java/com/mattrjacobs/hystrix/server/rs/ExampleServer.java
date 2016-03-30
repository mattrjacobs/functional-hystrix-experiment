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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.reactivesocket.websocket.rxnetty.server.ReactiveSocketWebSocketServer;
import io.reactivex.netty.protocol.http.server.HttpServer;

public class ExampleServer {
    private final ReactiveSocketWebSocketServer rsServer;
    private final HttpServer<ByteBuf, ByteBuf> server;

    public ExampleServer(final int port) {
        rsServer = ReactiveSocketWebSocketServer.create(setupPayload -> new ExampleServerHandler());

        server = HttpServer.newServer(port)
                .clientChannelOption(ChannelOption.AUTO_READ, true)
                .enableWireLogging(LogLevel.INFO);
    }

    public void start() {
        server.start((req, resp) -> resp.acceptWebSocketUpgrade(rsServer::acceptWebsocket));
    }

    public void awaitShutdown() {
        server.awaitShutdown();
    }
}
