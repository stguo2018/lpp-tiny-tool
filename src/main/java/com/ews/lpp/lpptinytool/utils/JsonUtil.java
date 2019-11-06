package com.ews.lpp.lpptinytool.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@Slf4j
public class JsonUtil {

    private JsonUtil() {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Object to json failure.", e);
        }
        return null;
    }

    public static <T> T fromJsonByTypeReference(String json, TypeReference<T> reference) {
        try {
            return objectMapper.readValue(json, reference);
        } catch (JsonProcessingException e) {
            log.error("Object to json failure.", e);
        }
        return null;
    }

}
