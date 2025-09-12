/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.forwarder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.actuate.health.Health;
import sanbing.jcpp.infrastructure.queue.discovery.PartitionProvider;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.infrastructure.queue.provider.AppQueueFactory;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.codec.ByteUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.Tracer;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.cfg.ForwarderCfg;
import sanbing.jcpp.protocol.cfg.KafkaCfg;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static sanbing.jcpp.infrastructure.queue.common.QueueConstants.*;

/**
 * @author 九筒
 */
@Slf4j
public class KafkaForwarder extends Forwarder {
    final AtomicBoolean healthy = new AtomicBoolean(true);

    private static final String OFFSET = "offset";
    private static final String PARTITION = "partition";
    private static final String TOPIC = "topic";

    private final KafkaCfg kafkaCfg;
    protected final boolean jcppPartition;

    private KafkaProducer<String, byte[]> kafkaProducer;

    public KafkaForwarder(String protocolName,
                          ForwarderCfg forwarderCfg,
                          StatsFactory statsFactory,
                          AppQueueFactory appQueueFactory,
                          PartitionProvider partitionProvider,
                          ServiceInfoProvider serviceInfoProvider) {
        super(protocolName, statsFactory, partitionProvider, serviceInfoProvider);

        this.kafkaCfg = forwarderCfg.getKafka();
        this.jcppPartition = kafkaCfg.isJcppPartition();

        if (this.isMonolith || jcppPartition) {

            this.producer = appQueueFactory.createProtocolUplinkMsgProducer(kafkaCfg.getTopic());

        } else {
            Properties properties = new Properties();
            properties.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka-forwarder-" + getProtocolName() + "-" + serviceInfoProvider.getServiceId());
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaCfg.getBootstrapServers());
            properties.put(ProducerConfig.ACKS_CONFIG, kafkaCfg.getAcks());
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
            properties.put(ProducerConfig.RETRIES_CONFIG, kafkaCfg.getRetries());
            properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaCfg.getCompressionType());
            properties.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaCfg.getBatchSize());
            properties.put(ProducerConfig.LINGER_MS_CONFIG, kafkaCfg.getLingerMs());
            properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaCfg.getBufferMemory());
            if (this.kafkaCfg.getOtherProperties() != null) {
                this.kafkaCfg.getOtherProperties().forEach((k, v) -> {
                    if (SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG.equals(k)
                            || SslConfigs.SSL_KEYSTORE_KEY_CONFIG.equals(k)
                            || SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG.equals(k)) {
                        v = v.replace("\\n", "\n");
                    }
                    properties.put(k, v);
                });
            }

            this.kafkaProducer = new KafkaProducer<>(properties);
        }
    }


    @Override
    public Health health() {
        if (healthy.get()) {
            return Health.up().withDetail("producer", "Kafka producer is healthy").build();
        } else {
            return Health.down().withDetail("producer", "Kafka producer is unhealthy").build();
        }
    }

    @Override
    public void destroy() {
        healthy.set(false);

        if (this.kafkaProducer != null) {
            try {
                this.kafkaProducer.close();
            } catch (Exception e) {
                log.error("Failed to close producer during destroy()", e);
            }
        }
    }

    @Override
    public void sendMessage(UplinkQueueMessage msg, BiConsumer<Boolean, ObjectNode> consumer) {
        String topic = kafkaCfg.getTopic();

        try {

            String messageKey = msg.getMessageKey();

            if (isMonolith || jcppPartition) {

                jcppForward(topic, messageKey, msg, consumer);

            } else {

                kafkaForward(topic, messageKey, msg, consumer);
            }

        } catch (Exception e) {
            log.debug("[{}] Failed to forward Kafka message: {}", getProtocolName(), msg, e);
        }
    }

    @Override
    public void sendMessage(UplinkQueueMessage msg) {
        sendMessage(msg, null);
    }

    private void kafkaForward(String topic, String key, UplinkQueueMessage msg, BiConsumer<Boolean, ObjectNode> consumer) throws InvalidProtocolBufferException {
        forwarderMessagesStats.incrementTotal();
        Headers headers = new RecordHeaders();

        Tracer currentTracer = TracerContextUtil.getCurrentTracer();
        headers.add(new RecordHeader(MSG_MD_TRACER_ID, ByteUtil.stringToBytes(currentTracer.getTraceId())));
        headers.add(new RecordHeader(MSG_MD_TRACER_ORIGIN, ByteUtil.stringToBytes(currentTracer.getOrigin())));
        headers.add(new RecordHeader(MSG_MD_TRACER_TS, ByteUtil.longToBytes(currentTracer.getTracerTs())));

        if (kafkaCfg.getEncoder() == KafkaCfg.EncoderType.json) {

            String protoJson = JsonFormat.printer().print(msg);

            log.info("[{}] Kafka forwarder send json headers:{}, message:{}", getProtocolName(), headers, protoJson);

            kafkaProducer.send(new ProducerRecord<>(topic, null, null, key, ByteUtil.stringToBytes(protoJson), headers),
                    (metadata, e) -> logAndDoConsumer(consumer, metadata, e, currentTracer));
        } else {

            log.info("[{}] Kafka forwarder send protobuf headers:{}, message:{}", getProtocolName(), headers, msg);

            kafkaProducer.send(new ProducerRecord<>(topic, null, null, key, msg.toByteArray(), headers),
                    (metadata, e) -> logAndDoConsumer(consumer, metadata, e, currentTracer));
        }
    }

    private void logAndDoConsumer(BiConsumer<Boolean, ObjectNode> consumer, RecordMetadata metadata, Exception e, Tracer currentTracer) {
        TracerContextUtil.newTracer(currentTracer.getTraceId(), currentTracer.getOrigin(), currentTracer.getTracerTs());
        MDCUtils.recordTracer();

        log.debug("Kafka 消息转发完成, success:{}", e == null);

        if (e != null) {
            forwarderMessagesStats.incrementFailed();
        } else {
            forwarderMessagesStats.incrementSuccessful();
        }

        if (consumer != null) {
            ObjectNode objectNode = JacksonUtil.newObjectNode();
            if (e != null) {
                objectNode.put(ERROR, e.getClass() + ": " + e.getMessage());
            }
            objectNode.put(OFFSET, String.valueOf(metadata.offset()));
            objectNode.put(PARTITION, String.valueOf(metadata.partition()));
            objectNode.put(TOPIC, metadata.topic());
            consumer.accept(e == null, objectNode);
        }
    }

}