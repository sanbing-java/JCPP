/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.forwarder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import sanbing.jcpp.infrastructure.queue.discovery.PartitionProvider;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.infrastructure.queue.provider.AppQueueFactory;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.cfg.ForwarderCfg;
import sanbing.jcpp.protocol.cfg.MemoryCfg;

import java.util.function.BiConsumer;

/**
 * @author 九筒
 */
@Slf4j
public class MemoryForwarder extends Forwarder {

    private final MemoryCfg memoryCfg;


    public MemoryForwarder(String protocolName,
                           ForwarderCfg forwarderCfg,
                           StatsFactory statsFactory,
                           AppQueueFactory appQueueFactory,
                           PartitionProvider partitionProvider,
                           ServiceInfoProvider serviceInfoProvider) {
        super(protocolName, statsFactory, partitionProvider, serviceInfoProvider);

        this.memoryCfg = forwarderCfg.getMemory();

        super.producer = appQueueFactory.createProtocolUplinkMsgProducer(memoryCfg.getTopic());
    }

    @Override
    public Health health() {
        if (healthy.get()) {
            return Health.up().withDetail("producer", "Memory producer is healthy").build();
        } else {
            return Health.down().withDetail("producer", "Memory producer is unhealthy").build();
        }
    }

    @Override
    public void destroy() {
        healthy.set(false);

        if (this.producer != null) {
            try {
                this.producer.stop();
            } catch (Exception e) {
                log.error("Failed to close producer during destroy()", e);
            }
        }
    }

    @Override
    public void sendMessage(UplinkQueueMessage msg, BiConsumer<Boolean, ObjectNode> consumer) {
        String topic = memoryCfg.getTopic();

        String key = msg.getMessageKey();

        try {

            jcppForward(topic, key, msg, consumer);

        } catch (Exception e) {

            log.warn("[{}] Failed to forward Memory message: {}", getProtocolName(), msg, e);
        }
    }

    @Override
    public void sendMessage(UplinkQueueMessage msg) {
        sendMessage(msg, null);
    }
}