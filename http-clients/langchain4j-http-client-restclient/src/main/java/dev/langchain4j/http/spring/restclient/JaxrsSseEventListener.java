/*
 * Copyright 2024 Emmanuel Hugonnet (c) 2024 Red Hat, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.langchain4j.http.spring.restclient;

import dev.langchain4j.http.ServerSentEvent;
import dev.langchain4j.http.ServerSentEventListener;
import jakarta.ws.rs.sse.InboundSseEvent;
import java.util.function.Consumer;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class JaxrsSseEventListener implements Consumer<InboundSseEvent>, Runnable {
    private final ServerSentEventListener listener;

    public JaxrsSseEventListener(ServerSentEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void accept(InboundSseEvent event) {
        listener.onEvent(convert(event));
    }

    public void onError(Throwable error) {
        listener.onError(error);
    }

    @Override
    public void run() {
        listener.onFinish();
    }

    private static ServerSentEvent convert(InboundSseEvent inbound) {
        return ServerSentEvent.builder()
                .type(inbound.getName())
                .data(inbound.readData())
                .build();
    }
}
