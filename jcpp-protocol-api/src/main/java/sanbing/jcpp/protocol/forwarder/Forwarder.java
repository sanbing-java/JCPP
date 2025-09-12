/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.forwarder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import sanbing.jcpp.infrastructure.queue.*;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.infrastructure.queue.discovery.PartitionProvider;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceType;
import sanbing.jcpp.infrastructure.stats.MessagesStats;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.codec.ByteUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.Tracer;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static sanbing.jcpp.infrastructure.queue.common.QueueConstants.*;

/**
 * @author 九筒
 */
@Slf4j
public abstract class Forwarder {
    protected static final String ERROR = "error";

    final AtomicBoolean healthy = new AtomicBoolean(true);

    @Getter
    private final String protocolName;

    protected final MessagesStats forwarderMessagesStats;


    protected final PartitionProvider partitionProvider;
    protected final ServiceInfoProvider serviceInfoProvider;

    protected final boolean isMonolith;
    protected QueueProducer<ProtoQueueMsg<UplinkQueueMessage>> producer;

    protected Forwarder(String protocolName, StatsFactory statsFactory, PartitionProvider partitionProvider, ServiceInfoProvider serviceInfoProvider) {
        this.protocolName = protocolName;
        this.partitionProvider = partitionProvider;
        this.serviceInfoProvider = serviceInfoProvider;

        this.forwarderMessagesStats = statsFactory.createMessagesStats("forwarderMessages", "protocol", protocolName);

        this.isMonolith = serviceInfoProvider.isMonolith();
    }

    public abstract Health health();

    public abstract void destroy();

    protected void jcppForward(String topic, String key, UplinkQueueMessage msg, BiConsumer<Boolean, ObjectNode> consumer) {
        forwarderMessagesStats.incrementTotal();
        QueueMsgHeaders headers = new DefaultQueueMsgHeaders();

        Tracer currentTracer = TracerContextUtil.getCurrentTracer();
        headers.put(MSG_MD_TRACER_ID, ByteUtil.stringToBytes(currentTracer.getTraceId()));
        headers.put(MSG_MD_TRACER_ORIGIN, ByteUtil.stringToBytes(currentTracer.getOrigin()));
        headers.put(MSG_MD_TRACER_TS, ByteUtil.longToBytes(currentTracer.getTracerTs()));

        TopicPartitionInfo tpi = partitionProvider.resolve(ServiceType.APP, topic, key);
        producer.send(tpi, new ProtoQueueMsg<>(key, msg, headers), new QueueCallback() {
            @Override
            public void onSuccess(QueueMsgMetadata metadata) {

                TracerContextUtil.newTracer(currentTracer.getTraceId(), currentTracer.getOrigin(), currentTracer.getTracerTs());
                MDCUtils.recordTracer();

                log.trace("单体消息转发成功 key:{}", key);

                if (consumer != null) {
                    consumer.accept(true, JacksonUtil.newObjectNode());
                }
                forwarderMessagesStats.incrementSuccessful();
            }

            @Override
            public void onFailure(Throwable t) {

                TracerContextUtil.newTracer(currentTracer.getTraceId(), currentTracer.getOrigin(), currentTracer.getTracerTs());
                MDCUtils.recordTracer();

                log.warn("单体消息转发异常", t);

                if (consumer != null) {
                    ObjectNode objectNode = JacksonUtil.newObjectNode();
                    objectNode.put(ERROR, t.getClass() + ": " + t.getMessage());
                    consumer.accept(true, objectNode);
                }
                forwarderMessagesStats.incrementFailed();
            }
        });
    }

    public abstract void sendMessage(UplinkQueueMessage msg, BiConsumer<Boolean, ObjectNode> consumer);

    public abstract void sendMessage(UplinkQueueMessage msg);

}