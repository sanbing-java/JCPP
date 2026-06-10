/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import sanbing.jcpp.infrastructure.queue.QueueAdmin;
import sanbing.jcpp.infrastructure.queue.QueueCallback;
import sanbing.jcpp.infrastructure.queue.QueueMsg;
import sanbing.jcpp.infrastructure.queue.QueueProducer;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class KafkaProducerTemplate<T extends QueueMsg> implements QueueProducer<T> {

    private final KafkaProducer<String, byte[]> producer;

    @Getter
    private final String topic;

    @Getter
    private final KafkaSettings settings;

    private final QueueAdmin admin;

    private final Set<TopicPartitionInfo> topics;

    @Getter
    private final String clientId;

    @Builder
    private KafkaProducerTemplate(KafkaSettings settings, String topic, String clientId, QueueAdmin admin) {
        Properties props = settings.toProducerProps(topic);

        this.clientId = Objects.requireNonNull(clientId, "Kafka producer client.id is null");
        if (!StringUtils.isEmpty(clientId)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        }
        this.settings = settings;

        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
        this.admin = admin;
        topics = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void init() {
    }

    void addAnalyticHeaders(List<Header> headers) {
        headers.add(new RecordHeader("_producerId", getClientId().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("_threadName", Thread.currentThread().getName().getBytes(StandardCharsets.UTF_8)));
        if (log.isTraceEnabled()) {
            try {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                int maxLevel = Math.min(stackTrace.length, 20);
                for (int i = 2; i < maxLevel; i++) { // ignore two levels: getStackTrace and addAnalyticHeaders
                    headers.add(new RecordHeader("_stackTrace" + i, stackTrace[i].toString().getBytes(StandardCharsets.UTF_8)));
                }
            } catch (Throwable t) {
                log.trace("Failed to add stacktrace headers in Kafka producer {}", getClientId(), t);
            }
        }
    }

    @Override
    public void send(TopicPartitionInfo tpi, T msg, QueueCallback callback) {
        try {
            createTopicIfNotExist(tpi);
            String key = msg.getKey();
            byte[] data = msg.getData();
            ProducerRecord<String, byte[]> record;
            List<Header> headers = msg.getHeaders().getData().entrySet().stream().map(e -> new RecordHeader(e.getKey(), e.getValue())).collect(Collectors.toList());
            if (log.isDebugEnabled()) {
                addAnalyticHeaders(headers);
            }
            record = new ProducerRecord<>(tpi.getFullTopicName(), null, key, data, headers);
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    if (callback != null) {
                        callback.onSuccess(new KafkaQueueMsgMetadata(metadata));
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(exception);
                    } else {
                        log.warn("Producer template failure: {}", exception.getMessage(), exception);
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            } else {
                log.warn("Producer template failure (send method wrapper): {}", e.getMessage(), e);
            }
            throw e;
        }
    }

    private void createTopicIfNotExist(TopicPartitionInfo tpi) {
        if (topics.contains(tpi)) {
            return;
        }
        admin.createTopicIfNotExists(tpi.getFullTopicName());
        topics.add(tpi);
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer.close();
        }
    }
}
