/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.cfg;

import lombok.Getter;
import lombok.Setter;
import sanbing.jcpp.infrastructure.util.property.PropertyUtils;

import java.util.Map;

@Getter
@Setter
public class KafkaCfg {

    private String topic;

    private boolean jcppPartition;

    private String bootstrapServers;

    private String acks;

    private EncoderType encoder;

    private int retries;

    private String compressionType; // none, gzip, snappy, lz4, zstd

    private int batchSize;

    private int lingerMs;

    private long bufferMemory;

    private Map<String, String> otherProperties; // Other inline properties if necessary

    private String topicProperties;

    public void setOtherProperties(String otherProperties) {
        this.otherProperties = PropertyUtils.getProps(otherProperties);
    }

    public enum EncoderType {
        protobuf,
        json
    }
}