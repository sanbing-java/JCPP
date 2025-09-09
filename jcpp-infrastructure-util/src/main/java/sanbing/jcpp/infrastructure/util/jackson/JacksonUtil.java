/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.jackson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * @author 九筒
 */
public class JacksonUtil {

    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS, false)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .defaultTimeZone(TimeZone.getTimeZone("GMT+8"))
            .build()
            .registerModules(DataTypeModule.INSTANCE);

    public static final ObjectMapper PRETTY_SORTED_JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .serializationInclusion(Include.NON_NULL)
            .defaultTimeZone(TimeZone.getTimeZone("GMT+8"))
            .build()
            .registerModules(DataTypeModule.INSTANCE);


    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueType) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("给定的对象值: "
                    + fromValue + " 无法转换为 " + toValueType, e);
        }
    }

    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("给定的对象值: "
                    + fromValue + " 无法转换为 " + toValueTypeRef, e);
        }
    }

    public static <T> T fromString(String string, Class<T> clazz) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字符串值: "
                    + string + " 无法转换为Json对象", e);
        }
    }

    public static <T> T fromString(String string, TypeReference<T> valueTypeRef) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字符串值: "
                    + string + " 无法转换为Json对象", e);
        }
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            return bytes != null ? OBJECT_MAPPER.readValue(bytes, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字节数组: "
                    + Arrays.toString(bytes) + " 无法转换为Json对象", e);
        }
    }
    public static <T> T fromReader(Reader reader, Class<T> clazz) {
        try {
            return reader != null ? OBJECT_MAPPER.readValue(reader, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的Reader无法转换为Json对象", e);
        }
    }

    public static JsonNode fromBytes(byte[] bytes) {
        try {
            return OBJECT_MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字节数组: "
                    + Arrays.toString(bytes) + " 无法转换为Json对象", e);
        }
    }

    public static String toString(Object value) {
        try {
            return value != null ? OBJECT_MAPPER.writeValueAsString(value) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("给定的对象值无法转换为字符串: " + value, e);
        }
    }

    public static String toPrettyString(Object o) {
        try {
            return PRETTY_SORTED_JSON_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode toJsonNode(Object value) {
        try {
            return OBJECT_MAPPER.valueToTree(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("给定的对象值无法转换为JSON节点: " + value, e);
        }
    }

    public static <T> T toPojo(String content, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(content, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字符串值无法转换为指定类型: " + content, e);
        }
    }

    public static <T> T toPojo(String content, TypeReference<T> type) {
        try {
            return OBJECT_MAPPER.readValue(content, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字符串值无法转换为指定类型: " + content, e);
        }
    }

    public static JsonNode toJson(String content) {
        try {
            return OBJECT_MAPPER.readTree(content);
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字符串值无法转换为JSON: " + content, e);
        }
    }

    public static JsonNode toJson(byte[] content) {
        try {
            return OBJECT_MAPPER.readTree(content);
        } catch (IOException e) {
            throw new IllegalArgumentException("给定的字节数组无法转换为JSON: " + Arrays.toString(content), e);
        }
    }

    public static <T> T fromJson(JsonNode json, Class<T> type) {
        try {
            return OBJECT_MAPPER.treeToValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("给定的JSON对象无法转换为指定类型: " + json, e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return fromJson(toJson(json), type);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JSON转换失败", e);
        }
    }

    public static <T> T treeToValue(JsonNode node, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("无法转换值: " + node.toString(), e);
        }
    }

    public static ObjectNode newObjectNode() {
        return newObjectNode(OBJECT_MAPPER);
    }

    public static ObjectNode newObjectNode(ObjectMapper mapper) {
        return mapper.createObjectNode();
    }

    public static ArrayNode newArrayNode() {
        return newArrayNode(OBJECT_MAPPER);
    }

    public static ArrayNode newArrayNode(ObjectMapper mapper) {
        return mapper.createArrayNode();
    }

    public static <T> T clone(T value) {
        @SuppressWarnings("unchecked")
        Class<T> valueClass = (Class<T>) value.getClass();
        return fromString(toString(value), valueClass);
    }

    public static <T> JsonNode valueToTree(T value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    public static <T> byte[] writeValueAsBytes(T value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("给定的对象值无法转换为字节数组: " + value, e);
        }
    }

    public static <T> void writeValue(Writer writer, T value) {
        try {
            OBJECT_MAPPER.writeValue(writer, value);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given writer value: "
                    + writer + "cannot be wrote", e);
        }
    }

    public static JsonNode getSafely(JsonNode node, String... path) {
        if (node == null) {
            return null;
        }
        for (String p : path) {
            if (!node.has(p)) {
                return null;
            } else {
                node = node.get(p);
            }
        }
        return node;
    }

}
