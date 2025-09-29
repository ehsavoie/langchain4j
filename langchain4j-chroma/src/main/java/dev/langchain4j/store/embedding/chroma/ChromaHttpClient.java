package dev.langchain4j.store.embedding.chroma;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChromaHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ChromaHttpClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final boolean logRequests;
    private final boolean logResponses;

    public ChromaHttpClient(String baseUrl, Duration timeout, boolean logRequests, boolean logResponses) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.logRequests = logRequests;
        this.logResponses = logResponses;
        dev.langchain4j.http.client.HttpClientBuilder httpClientBuilder =
                dev.langchain4j.http.client.HttpClientBuilderLoader.loadHttpClientBuilder();
        // Configure HTTP/1.1 for Chroma compatibility if using JDK HTTP client
        if ("dev.langchain4j.http.client.jdk.JdkHttpClientBuilder"
                .equals(httpClientBuilder.getClass().getCanonicalName())) {
            try {
                Method method = httpClientBuilder
                        .getClass()
                        .getMethod("httpClientBuilder", java.net.http.HttpClient.Builder.class);
                method.invoke(
                        httpClientBuilder,
                        java.net.http.HttpClient.newBuilder()
                                .connectTimeout(timeout)
                                .version(java.net.http.HttpClient.Version.HTTP_1_1));
            } catch (NoSuchMethodException ex) {
                java.util.logging.Logger.getLogger(ChromaHttpClient.class.getName())
                        .log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                java.util.logging.Logger.getLogger(ChromaHttpClient.class.getName())
                        .log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(ChromaHttpClient.class.getName())
                        .log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                java.util.logging.Logger.getLogger(ChromaHttpClient.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }

        this.httpClient = httpClientBuilder.build();

        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T get(String path, Class<T> responseType) throws IOException {
        return get(path, responseType, null);
    }

    public <T> T get(String path, Class<T> responseType, Map<String, String> pathParams) throws IOException {
        String url = buildUrl(path, pathParams);

        HttpRequest request = HttpRequest.builder()
                .method(HttpMethod.GET)
                .url(url)
                .addHeader("Content-Type", "application/json")
                .build();

        return executeRequest(request, responseType);
    }

    public <T> T post(String path, Object requestBody, Class<T> responseType) throws IOException {
        return post(path, requestBody, responseType, null);
    }

    public <T> T post(String path, Object requestBody, Class<T> responseType, Map<String, String> pathParams)
            throws IOException {
        String url = buildUrl(path, pathParams);
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.builder()
                .method(HttpMethod.POST)
                .url(url)
                .addHeader("Content-Type", "application/json")
                .body(jsonBody)
                .build();

        return executeRequest(request, responseType);
    }

    public void delete(String path) throws IOException {
        delete(path, null);
    }

    public void delete(String path, Map<String, String> pathParams) throws IOException {
        String url = buildUrl(path, pathParams);

        HttpRequest request = HttpRequest.builder()
                .method(HttpMethod.DELETE)
                .url(url)
                .addHeader("Content-Type", "application/json")
                .build();

        executeRequest(request, Void.class);
    }

    private <T> T executeRequest(HttpRequest request, Class<T> responseType) throws IOException {
        if (logRequests) {
            logRequest(request);
        }

        try {
            SuccessfulHttpResponse response = httpClient.execute(request);

            if (logResponses) {
                logResponse(response);
            }

            if (responseType == Void.class || response.body().isEmpty()) {
                return null;
            }
            try {
                return objectMapper.readValue(response.body(), responseType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse response: " + response.body(), e);
            }
        } catch (HttpException e) {
            throw new RuntimeException("HTTP error: " + e.getMessage(), e);
        }
    }

    private String buildUrl(String path, Map<String, String> pathParams) {
        String url = baseUrl + path;

        if (pathParams != null) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return url;
    }

    private void logRequest(HttpRequest request) {
        try {
            log.debug(
                    "Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}",
                    request.method(),
                    request.url(),
                    request.headers(),
                    request.body());
        } catch (Exception e) {
            log.warn("Error while logging request: {}", e.getMessage());
        }
    }

    private void logResponse(SuccessfulHttpResponse response) {
        try {
            log.debug(
                    "Response:\n- status code: {}\n- headers: {}\n- body: {}",
                    response.statusCode(),
                    response.headers(),
                    response.body());
        } catch (Exception e) {
            log.warn("Error while logging response: {}", e.getMessage());
        }
    }
}
