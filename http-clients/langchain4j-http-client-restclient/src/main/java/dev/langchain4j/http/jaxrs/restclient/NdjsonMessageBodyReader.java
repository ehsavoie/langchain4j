package dev.langchain4j.http.jaxrs.restclient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import org.jboss.resteasy.plugins.providers.sse.EventInput;
import org.jboss.resteasy.plugins.providers.sse.SseConstants;
import org.jboss.resteasy.plugins.providers.sse.SseEventInputImpl;

@Provider
@Consumes({"application/ndjson", "application/x-ndjson"})
public class NdjsonMessageBodyReader implements MessageBodyReader<SseEventInputImpl> {
    public static final MediaType APPLICATION_X_ND_JSON = new MediaType("application", "x-ndjson");
    public static final MediaType APPLICATION_ND_JSON = new MediaType("application", "ndjson");

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return (EventInput.class.isAssignableFrom(type))
                && (APPLICATION_X_ND_JSON.isCompatible(mediaType)
                        || APPLICATION_ND_JSON.isCompatible(mediaType)
                        || MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType));
    }

    @Override
    public SseEventInputImpl readFrom(
            Class<SseEventInputImpl> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
            throws IOException, WebApplicationException {
        MediaType streamType = mediaType;
        if (mediaType.getParameters() != null) {
            Map<String, String> map = mediaType.getParameters();
            String elementType = map.get(SseConstants.SSE_ELEMENT_MEDIA_TYPE);
            if (elementType != null) {
                mediaType = MediaType.valueOf(elementType);
            }
        }
        return new SseEventInputImpl(annotations, streamType, mediaType, httpHeaders, entityStream);
    }
}
