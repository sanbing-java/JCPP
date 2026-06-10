/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.discovery;

import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.proto.gen.ClusterProto.ServiceInfo;

import java.util.List;
import java.util.UUID;

public interface PartitionProvider {

    TopicPartitionInfo resolve(ServiceType serviceType,String queueName, UUID entityId);

    TopicPartitionInfo resolve(ServiceType serviceType,String queueName, String pileCode);

    void recalculatePartitions(ServiceInfo currentService, List<ServiceInfo> otherServices);

}
