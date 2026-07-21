package com.atlaskv.sdk.util;

import com.atlaskv.sdk.exceptions.SerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared utility for handling Jackson JSON serialization and deserialization.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {
        // Prevent instantiation
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param obj object to serialize
     * @return JSON string representation
     * @throws SerializationException if serialization fails
     */
    public static String writeValueAsString(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string into a target class.
     *
     * @param json      JSON string
     * @param valueType target class
     * @param <T>       target type
     * @return deserialized object
     * @throws SerializationException if deserialization fails
     */
    public static <T> T readValue(String json, Class<T> valueType) {
        try {
            return MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to deserialize JSON to " + valueType.getSimpleName(), e);
        }
    }

    /**
     * Deserializes a JSON string into a target type reference (for generic collections).
     *
     * @param json         JSON string
     * @param valueTypeRef type reference
     * @param <T>          target type
     * @return deserialized object
     * @throws SerializationException if deserialization fails
     */
    public static <T> T readValue(String json, TypeReference<T> valueTypeRef) {
        try {
            return MAPPER.readValue(json, valueTypeRef);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to deserialize JSON to generic type", e);
        }
    }
}
