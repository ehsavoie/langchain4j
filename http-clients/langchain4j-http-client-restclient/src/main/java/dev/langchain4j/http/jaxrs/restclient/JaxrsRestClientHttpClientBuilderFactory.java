package dev.langchain4j.http.jaxrs.restclient;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpClientBuilderFactory;

public class JaxrsRestClientHttpClientBuilderFactory implements HttpClientBuilderFactory {

    @Override
    public HttpClientBuilder create() {
        return new JaxrsRestClientHttpClientBuilder();
    }
}
