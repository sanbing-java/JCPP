/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.service.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import sanbing.jcpp.infrastructure.queue.common.QueueConfig;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;

import java.util.Set;

@Getter
@ToString
@AllArgsConstructor
public class QueueConsumerManagerTask {

    private final QueueEvent event;
    private QueueConfig config;
    private Set<TopicPartitionInfo> partitions;
    private boolean drainQueue;

    public static QueueConsumerManagerTask delete(boolean drainQueue) {
        return new QueueConsumerManagerTask(QueueEvent.DELETE, null, null, drainQueue);
    }

    public static QueueConsumerManagerTask configUpdate(QueueConfig config) {
        return new QueueConsumerManagerTask(QueueEvent.CONFIG_UPDATE, config, null, false);
    }

    public static QueueConsumerManagerTask partitionChange(Set<TopicPartitionInfo> partitions) {
        return new QueueConsumerManagerTask(QueueEvent.PARTITION_CHANGE, null, partitions, false);
    }

}
