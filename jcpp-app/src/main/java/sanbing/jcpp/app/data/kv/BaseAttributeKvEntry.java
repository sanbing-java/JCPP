/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.data.kv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.util.Optional;

@Slf4j
@Data
public class BaseAttributeKvEntry implements AttributeKvEntry {

    private static final long serialVersionUID = -6460767583563159407L;

    private final long lastUpdateTs;
    @Valid
    private final KvEntry kv;

    private final Integer version;

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
        this.version = null;
    }

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs, Integer version) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
        this.version = version;
    }

    public BaseAttributeKvEntry(long lastUpdateTs, KvEntry kv) {
        this(kv, lastUpdateTs);
    }

    @Override
    public String getKey() {
        return kv.getKey();
    }

    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    @Override
    public Optional<String> getJsonValue() {
        return kv.getJsonValue();
    }

    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    @Override
    public Object getValue() {
        return kv.getValue();
    }

    /**
     * 将当前对象转换为JSON字节数组
     * 避免Jackson序列化Optional类型的问题
     */
    @JsonIgnore
    public byte[] toJsonBytes() {
        try {
            ObjectNode json = JacksonUtil.newObjectNode();
            json.put("lastUpdateTs", lastUpdateTs);
            if (version != null) {
                json.put("version", version);
            }
            
            // 处理KvEntry序列化
            ObjectNode kvJson = JacksonUtil.newObjectNode();
            kvJson.put("key", kv.getKey());
            kvJson.put("dataType", kv.getDataType().name());
            
            // 根据数据类型序列化值，避免Optional问题
            switch (kv.getDataType()) {
                case STRING:
                    kv.getStrValue().ifPresent(value -> kvJson.put("value", value));
                    break;
                case LONG:
                    kv.getLongValue().ifPresent(value -> kvJson.put("value", value));
                    break;
                case BOOLEAN:
                    kv.getBooleanValue().ifPresent(value -> kvJson.put("value", value));
                    break;
                case DOUBLE:
                    kv.getDoubleValue().ifPresent(value -> kvJson.put("value", value));
                    break;
                case JSON:
                    kv.getJsonValue().ifPresent(value -> kvJson.put("value", value));
                    break;
                default:
                    // 如果没有匹配的类型，将值作为字符串处理
                    kvJson.put("value", kv.getValueAsString());
                    break;
            }
            
            json.set("kv", kvJson);
            return JacksonUtil.writeValueAsBytes(json);
        } catch (Exception e) {
            log.error("Failed to serialize BaseAttributeKvEntry to JSON bytes", e);
            throw new RuntimeException("Failed to serialize BaseAttributeKvEntry", e);
        }
    }

    /**
     * 从JSON字节数组反序列化为BaseAttributeKvEntry对象
     * 避免Jackson反序列化Optional类型的问题
     */
    public static BaseAttributeKvEntry fromJsonBytes(byte[] jsonBytes) {
        try {
            JsonNode json = JacksonUtil.fromBytes(jsonBytes);
            
            long lastUpdateTs = json.get("lastUpdateTs").asLong();
            Integer version = json.has("version") ? json.get("version").asInt() : null;
            
            // 解析KvEntry
            JsonNode kvJson = json.get("kv");
            String key = kvJson.get("key").asText();
            DataType dataType = DataType.valueOf(kvJson.get("dataType").asText());
            
            KvEntry kvEntry;
            switch (dataType) {
                case STRING:
                    String strValue = kvJson.has("value") ? kvJson.get("value").asText() : null;
                    kvEntry = new StringDataEntry(key, strValue);
                    break;
                case LONG:
                    Long longValue = kvJson.has("value") ? kvJson.get("value").asLong() : null;
                    kvEntry = new LongDataEntry(key, longValue);
                    break;
                case BOOLEAN:
                    Boolean boolValue = kvJson.has("value") ? kvJson.get("value").asBoolean() : null;
                    kvEntry = new BooleanDataEntry(key, boolValue);
                    break;
                case DOUBLE:
                    Double doubleValue = kvJson.has("value") ? kvJson.get("value").asDouble() : null;
                    kvEntry = new DoubleDataEntry(key, doubleValue);
                    break;
                case JSON:
                    String jsonValue = kvJson.has("value") ? kvJson.get("value").asText() : null;
                    kvEntry = new JsonDataEntry(key, jsonValue);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + dataType);
            }
            
            return new BaseAttributeKvEntry(kvEntry, lastUpdateTs, version);
        } catch (Exception e) {
            log.error("Failed to deserialize BaseAttributeKvEntry from JSON bytes", e);
            throw new RuntimeException("Failed to deserialize BaseAttributeKvEntry", e);
        }
    }

}
