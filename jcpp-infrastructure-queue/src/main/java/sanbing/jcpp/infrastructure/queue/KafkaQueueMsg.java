/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaQueueMsg implements QueueMsg {
    private final String key;
    private final QueueMsgHeaders headers;
    private final byte[] data;

    public KafkaQueueMsg(ConsumerRecord<String, byte[]> record) {
        this.key = record.key();
        QueueMsgHeaders headers = new DefaultQueueMsgHeaders();
        record.headers().forEach(header -> {
            headers.put(header.key(), header.value());
        });
        this.headers = headers;
        this.data = record.value();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public QueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
