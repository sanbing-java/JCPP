/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.kafka;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.util.property.PropertyUtils;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class KafkaTopicConfigs {
    public static final String NUM_PARTITIONS_SETTING = "partitions";

    @Value("${queue.kafka.topic-properties.app:}")
    private String appProperties;

    @Getter
    private Map<String, String> appConfigs;

    @PostConstruct
    private void init() {
        this.appConfigs = PropertyUtils.getProps(appProperties);
    }

}
