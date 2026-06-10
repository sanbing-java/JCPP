/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class KafkaConsumerStatisticConfig {

    @Value("${queue.kafka.consumer-stats.enabled:true}")
    private Boolean enabled;

    @Value("${queue.kafka.consumer-stats.print-interval-ms:60000}")
    private Long printIntervalMs;

    @Value("${queue.kafka.consumer-stats.kafka-response-timeout-ms:1000}")
    private Long kafkaResponseTimeoutMs;
}
