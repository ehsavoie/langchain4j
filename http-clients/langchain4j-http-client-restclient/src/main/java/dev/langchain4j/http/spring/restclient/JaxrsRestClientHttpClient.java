package dev.langchain4j.http.spring.restclient;

import static dev.langchain4j.internal.Utils.getOrDefault;

import dev.langchain4j.http.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class JaxrsRestClientHttpClient extends AbstractHttpClient {

    private final Client restClient;
    private final boolean logRequests;
    private final boolean logResponses;

    public JaxrsRestClientHttpClient(JaxrsRestClientHttpClientBuilder builder) {
        ClientBuilder restClientBuilder = getOrDefault(builder.restClientBuilder(), ClientBuilder.newBuilder());
        this.restClient = restClientBuilder
                .connectTimeout(builder.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(builder.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
        this.logRequests = builder.logRequests();
        this.logResponses = builder.logResponses();
    }

    public static JaxrsRestClientHttpClientBuilder builder() { // TODO
        return new JaxrsRestClientHttpClientBuilder();
    }

    @Override
    protected HttpResponse doExecute(HttpRequest httpRequest) {
        Invocation.Builder builder = restClient.target(httpRequest.url()).request();
        for (Entry<String, String> header : httpRequest.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        Response response = builder.build(httpRequest.method().toString()).invoke();
        Map<String, String> headers = new HashMap<>(response.getHeaders().size());
        response.getHeaders()
                .entrySet()
                .forEach(entry -> headers.put(entry.getKey(), entry.getValue().toString()));
        return HttpResponse.builder()
                .statusCode(response.getStatus())
                .headers(headers)
                .body(response.readEntity(String.class))
                .build();
    }

    @Override
    protected void doExecute(HttpRequest httpRequest, ServerSentEventListener listener) {
        JaxrsSseEventListener sseEventListener = new JaxrsSseEventListener(listener);
        ClientRequestFilter filter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                for (Map.Entry<String, String> header : httpRequest.headers().entrySet()) {
                    requestContext.getHeaders().add(header.getKey(), header.getValue());
                }
            }
        };
        try (SseEventSource msgEventSource = SseEventSource.target(
                        restClient.target(httpRequest.url()).register(filter))
                .build()) {
            msgEventSource.register(sseEventListener, error -> sseEventListener.onError(error), sseEventListener);
        }
    }

    @Override
    protected boolean logRequests() {
        return logRequests;
    }

    @Override
    public boolean logResponses() {
        return logResponses;
    }
}
