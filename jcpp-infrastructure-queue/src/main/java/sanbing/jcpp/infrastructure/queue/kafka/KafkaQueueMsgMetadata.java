/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.clients.producer.RecordMetadata;
import sanbing.jcpp.infrastructure.queue.QueueMsgMetadata;

@Data
@AllArgsConstructor
public class KafkaQueueMsgMetadata implements QueueMsgMetadata {

    private RecordMetadata metadata;
}
