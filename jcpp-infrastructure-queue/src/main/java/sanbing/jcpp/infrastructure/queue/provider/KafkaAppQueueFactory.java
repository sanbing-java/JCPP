/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.provider;

import com.google.protobuf.util.JsonFormat;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.queue.ProtoQueueMsg;
import sanbing.jcpp.infrastructure.queue.QueueAdmin;
import sanbing.jcpp.infrastructure.queue.QueueConsumer;
import sanbing.jcpp.infrastructure.queue.QueueProducer;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.infrastructure.queue.kafka.*;
import sanbing.jcpp.infrastructure.queue.settings.QueueAppSettings;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaAppQueueFactory implements AppQueueFactory {

    private final KafkaSettings kafkaSettings;
    private final QueueAppSettings appSettings;
    private final KafkaConsumerStatsService consumerStatsService;
    private final ServiceInfoProvider serviceInfoProvider;

    private final QueueAdmin appAdmin;

    public KafkaAppQueueFactory(KafkaSettings kafkaSettings,
                                ServiceInfoProvider serviceInfoProvider,
                                QueueAppSettings appSettings,
                                KafkaConsumerStatsService consumerStatsService,
                                KafkaTopicConfigs kafkaTopicConfigs) {
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.appSettings = appSettings;
        this.consumerStatsService = consumerStatsService;

        this.appAdmin = new KafkaAdmin(kafkaSettings, kafkaTopicConfigs.getAppConfigs());
    }

    @Override
    public QueueConsumer<ProtoQueueMsg<UplinkQueueMessage>> createProtocolUplinkMsgConsumer() {
        KafkaConsumerTemplate.KafkaConsumerTemplateBuilder<ProtoQueueMsg<UplinkQueueMessage>> consumerBuilder = KafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(appSettings.getTopic());
        consumerBuilder.clientId("protocol-uplink-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("protocol-uplink-consumer");
        if (appSettings.getDecoder() == QueueAppSettings.DecoderType.protobuf) {
            consumerBuilder.decoder(msg -> new ProtoQueueMsg<>(msg.getKey(), UplinkQueueMessage.parseFrom(msg.getData()), msg.getHeaders()));
        } else {
            consumerBuilder.decoder(msg -> {
                UplinkQueueMessage.Builder builder = UplinkQueueMessage.newBuilder();
                JsonFormat.parser().merge(new String(msg.getData()), builder);
                return new ProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
            });
        }
        consumerBuilder.admin(appAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public QueueProducer<ProtoQueueMsg<UplinkQueueMessage>> createProtocolUplinkMsgProducer(String topic) {
        KafkaProducerTemplate.KafkaProducerTemplateBuilder<ProtoQueueMsg<UplinkQueueMessage>> requestBuilder = KafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("protocol-to-app-" + serviceInfoProvider.getServiceId());
        requestBuilder.topic(topic);
        requestBuilder.admin(appAdmin);
        return requestBuilder.build();
    }


    @PreDestroy
    private void destroy() {
        if (appAdmin != null) {
            appAdmin.destroy();
        }
    }
}
