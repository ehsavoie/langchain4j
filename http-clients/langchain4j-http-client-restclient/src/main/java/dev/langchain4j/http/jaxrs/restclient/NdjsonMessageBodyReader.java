package dev.langchain4j.http.jaxrs.restclient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Consumes("application/x-ndjson")
public class NdjsonMessageBodyReader implements MessageBodyReader<List<JsonObject>> {
    public static final MediaType APPLICATION_ND_JSON = new MediaType("application", "x-ndjson");

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return (List.class.isAssignableFrom(type) && JsonObject.class.getName().equals(genericType.getTypeName()))
                && (APPLICATION_ND_JSON.isCompatible(mediaType)
                        || MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType));
    }

    @Override
    public List<JsonObject> readFrom(
            Class<List<JsonObject>> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
            throws IOException, WebApplicationException {
        final List<JsonObject> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try (JsonReader jsonReader = Json.createReader(new StringReader(line))) {
                    result.add(jsonReader.readObject());
                }
            }
        }
        return List.copyOf(result);
    }
}
