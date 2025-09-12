/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.queue.ProtoQueueMsg;
import sanbing.jcpp.infrastructure.queue.QueueConsumer;
import sanbing.jcpp.infrastructure.queue.QueueProducer;
import sanbing.jcpp.infrastructure.queue.memory.InMemoryQueueConsumer;
import sanbing.jcpp.infrastructure.queue.memory.InMemoryQueueProducer;
import sanbing.jcpp.infrastructure.queue.memory.InMemoryStorage;
import sanbing.jcpp.infrastructure.queue.settings.QueueAppSettings;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

@Slf4j
@Component
@ConditionalOnExpression("'${queue.type:null}'=='memory' && '${service.type:null}'=='monolith'")
public class InMemoryAppQueueFactory implements AppQueueFactory {

    private final InMemoryStorage storage;
    private final QueueAppSettings appSettings;

    public InMemoryAppQueueFactory(InMemoryStorage storage, QueueAppSettings appSettings) {
        this.storage = storage;
        this.appSettings = appSettings;
    }

    @Override
    public QueueConsumer<ProtoQueueMsg<UplinkQueueMessage>> createProtocolUplinkMsgConsumer() {
        return new InMemoryQueueConsumer<>(storage, appSettings.getTopic());
    }

    @Override
    public QueueProducer<ProtoQueueMsg<UplinkQueueMessage>> createProtocolUplinkMsgProducer(String topic) {
        return new InMemoryQueueProducer<>(storage, topic);
    }

    @Scheduled(fixedRateString = "${queue.memory.stats.print-interval-ms:60000}")
    private void printInMemoryStats() {
        storage.printStats();
    }

}
