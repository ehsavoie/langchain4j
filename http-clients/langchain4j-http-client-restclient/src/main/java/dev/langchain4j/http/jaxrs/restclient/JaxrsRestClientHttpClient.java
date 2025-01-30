package dev.langchain4j.http.jaxrs.restclient;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static java.util.stream.Collectors.joining;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.DefaultServerSentEventParser;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JaxrsRestClientHttpClient implements HttpClient {

    private final Client restClient;

    public JaxrsRestClientHttpClient(JaxrsRestClientHttpClientBuilder builder) {
        ClientBuilder restClientBuilder = getOrDefault(builder.restClientBuilder(), ClientBuilder.newBuilder());
        if (builder.connectTimeout() != null) {
            restClientBuilder.connectTimeout(builder.connectTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (builder.readTimeout() != null) {
            restClientBuilder.readTimeout(builder.readTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        this.restClient = restClientBuilder.build();
    }

    public static JaxrsRestClientHttpClientBuilder builder() { // TODO
        return new JaxrsRestClientHttpClientBuilder();
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest httpRequest) throws HttpException, RuntimeException {
        Invocation.Builder builder = restClient.target(httpRequest.url()).request();
        for (Entry<String, List<String>> header : httpRequest.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        Response response;
        if (httpRequest.body() != null && !httpRequest.body().isBlank() && httpRequest.method() == HttpMethod.POST) {
            response = builder.build(httpRequest.method().toString(), Entity.json(httpRequest.body()))
                    .invoke();
        } else {
            response = builder.build(httpRequest.method().toString()).invoke();
        }
        if (!isSuccessful(response.getStatus())) {
            throw new HttpException(response.getStatus(), readBody(response.readEntity(InputStream.class)));
        }
        Map<String, List<String>> headers = new HashMap<>(response.getHeaders().size());
        for (Entry<String, List<Object>> entry : response.getHeaders().entrySet()) {
            List<String> values =
                    entry.getValue().stream().map(o -> o.toString()).collect(Collectors.toList());
            headers.put(entry.getKey(), values);
        }
        return SuccessfulHttpResponse.builder()
                .statusCode(response.getStatus())
                .headers(headers)
                .body(response.readEntity(String.class))
                .build();
    }

    private static boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventListener listener) {
        execute(request, new DefaultServerSentEventParser(), listener);
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        JaxrsSseEventListener sseEventListener = new JaxrsSseEventListener(listener);
        ClientRequestFilter headerRequestFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                for (Map.Entry<String, List<String>> header : request.headers().entrySet()) {
                    requestContext.getHeaders().add(header.getKey(), header.getValue());
                }
                if (request.body() != null && !request.body().isBlank() && request.method() == HttpMethod.POST) {
                    requestContext.setEntity(Entity.json(request.body()));
                    requestContext.setMethod(request.method().toString());
                }
            }
        };
        ClientResponseFilter responseFilter = new ClientResponseFilter() {
            @Override
            public void filter(ClientRequestContext request, ClientResponseContext response) throws IOException {
                if (!isSuccessful(response.getStatus())) {
                    listener.onError(new HttpException(response.getStatus(), readBody(response.getEntityStream())));
                    return;
                }
                listener.onOpen(SuccessfulHttpResponse.builder()
                        .statusCode(response.getStatus())
                        .headers(response.getHeaders())
                        .build());
            }
        };
        try (SseEventSource msgEventSource = SseEventSource.target(restClient
                        .target(request.url())
                        .register(headerRequestFilter)
                        //                        .register(responseFilter)
                        .register(new NdjsonMessageBodyReader()))
                .build()) {
            msgEventSource.register(sseEventListener, error -> sseEventListener.onError(error), sseEventListener);
            try {
                msgEventSource.open();
                listener.onOpen(SuccessfulHttpResponse.builder().statusCode(200).build());
            } catch (IllegalStateException ex) {
                listener.onError(ex);
            }
        }
    }

    private static String readBody(InputStream inputStream) {
        try (inputStream;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(joining(System.lineSeparator()));
        } catch (IOException e) {
            return "Cannot read error response body: " + e.getMessage();
        }
    }
}
