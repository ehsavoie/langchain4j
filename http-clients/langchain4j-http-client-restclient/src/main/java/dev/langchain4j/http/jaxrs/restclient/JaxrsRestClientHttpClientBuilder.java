package dev.langchain4j.http.jaxrs.restclient;

import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import jakarta.ws.rs.client.ClientBuilder;
import java.time.Duration;

public class JaxrsRestClientHttpClientBuilder implements HttpClientBuilder {

    private ClientBuilder restClientBuilder;
    private Duration connectTimeout;
    private Duration readTimeout;
    private boolean logRequests;
    private boolean logResponses;

    public ClientBuilder restClientBuilder() {
        return restClientBuilder;
    }

    public HttpClientBuilder restClientBuilder(ClientBuilder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
        return this;
    }

    @Override
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public HttpClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public HttpClient build() {
        return new JaxrsRestClientHttpClient(this);
    }
}
