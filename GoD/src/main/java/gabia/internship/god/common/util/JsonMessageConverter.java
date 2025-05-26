package gabia.internship.god.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMessageConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T fromBytes(byte[] body, Class<T> clazz) {
        try {
            return objectMapper.readValue(body, clazz);
        } catch (Exception e) {
            throw new RuntimeException("역직렬화 실패", e);
        }
    }

    public static <T> T fromBytes(byte[] body, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(body, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("역직렬화 실패", e);
        }
    }

    public static byte[] toBytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("직렬화 실패", e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }
}
