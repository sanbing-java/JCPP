/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.infrastructure.queue.discovery.QueueKey;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceType;

import java.io.Serial;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static sanbing.jcpp.infrastructure.queue.discovery.QueueKey.MAIN_QUEUE_NAME;

@ToString(callSuper = true)
public class PartitionChangeEvent extends JCPPApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap;

    public PartitionChangeEvent(Object source, ServiceType serviceType, Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap) {
        super(source);
        this.partitionsMap = partitionsMap;
    }

    public Set<TopicPartitionInfo> getAppPartitions() {
        return getPartitionsByServiceTypeAndQueueName(ServiceType.APP, MAIN_QUEUE_NAME);
    }

    private Set<TopicPartitionInfo> getPartitionsByServiceTypeAndQueueName(ServiceType serviceType, String queueName) {
        return partitionsMap.entrySet()
                .stream()
                .filter(entry -> serviceType.equals(entry.getKey().getType()) && queueName.equals(entry.getKey().getQueueName()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }
}
