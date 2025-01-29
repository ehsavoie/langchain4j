package dev.langchain4j.http.jaxrs.restclient;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.sse.InboundSseEvent;
import java.util.function.Consumer;

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
        if (error instanceof ClientErrorException) {
            ClientErrorException ex = (ClientErrorException) error;
            listener.onError(new HttpException(
                    ex.getResponse().getStatus(), ex.getResponse().readEntity(String.class)));
        } else {
            listener.onError(error);
        }
    }

    @Override
    public void run() {
        listener.onClose();
    }

    private static ServerSentEvent convert(InboundSseEvent inbound) {
        return new ServerSentEvent(inbound.getName(), inbound.readData());
    }
}
