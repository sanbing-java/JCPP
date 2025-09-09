/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.discovery;

import com.google.common.hash.HashFunction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.infrastructure.queue.discovery.event.PartitionChangeEvent;
import sanbing.jcpp.proto.gen.ClusterProto.ServiceInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static sanbing.jcpp.infrastructure.util.JCPPHashUtil.forName;
import static sanbing.jcpp.infrastructure.util.JCPPHashUtil.hash;

/**
 * @author 九筒
 */
@Component
@Slf4j
@ConfigurationProperties("queue.partitions")
public class HashPartitionProvider implements PartitionProvider {

    @Value("${queue.app.topic}")
    private String appTopic;
    @Value("${queue.app.partitions:10}")
    private Integer appPartitions;
    @Value("${queue.partitions.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    // 存储分区主题的映射
    private final Map<QueueKey, String> partitionTopicsMap = new ConcurrentHashMap<>();
    // 存储分区大小的映射
    private final Map<QueueKey, Integer> partitionSizesMap = new ConcurrentHashMap<>();

    // 哈希函数，用于确定消息的分区
    private HashFunction hashFunction;

    // 存储我的分区列表的映射
    protected Map<QueueKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();

    // 应用事件发布器，用于发布应用事件
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    // 初始化方法，用于初始化哈希函数和分区信息
    @PostConstruct
    public void init() {
        // 根据名称初始化哈希函数
        this.hashFunction = forName(hashFunctionName);

        // 创建一个队列键，表示应用类型的服务
        QueueKey appKey = new QueueKey(ServiceType.APP);
        // 将应用类型的队列键与相应的主题关联
        partitionTopicsMap.put(appKey, appTopic);
        // 将应用类型的队列键与相应的分区数关联
        partitionSizesMap.put(appKey, appPartitions);
    }


    private TopicPartitionInfo resolve(QueueKey queueKey, int hash) {
        Integer partitionSize = partitionSizesMap.get(queueKey);
        if (partitionSize == null) {
            throw new IllegalStateException("队列 " + queueKey + " 的分区信息缺失");
        }

        int partition = Math.abs(hash % partitionSize);

        return buildTopicPartitionInfo(queueKey, partition);
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, UUID entityId) {
        QueueKey queueKey = getQueueKey(serviceType, queueName);
        return resolve(queueKey, hash(hashFunction, entityId));
    }

    /**
     * 根据服务类型、队列名称和堆码解析对应的主题分区信息
     *
     * @param serviceType 服务类型，用于区分不同的服务
     * @param queueName   队列名称，用于指定具体的队列
     * @param pileCode    堆码，用于进一步确定队列的分区
     * @return 返回解析到的主题分区信息对象
     */
    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, String pileCode) {
        // 根据服务类型和队列名称生成队列键
        QueueKey queueKey = getQueueKey(serviceType, queueName);
        // 使用哈希函数对堆码进行两次哈希处理，并根据队列键解析对应的主题分区信息
        return resolve(queueKey, hash(hashFunction, pileCode));
    }


    /**
     * 根据服务类型和队列名称获取队列键
     * 此方法用于构造一个队列键对象，并检查该队列键是否存在于分区大小映射中
     * 如果不存在，则构造一个新的仅包含服务类型的队列键
     * 这主要用于支持服务类型的队列管理，确保即使特定队列名称的键不存在，也能基于服务类型找到通用的配置
     *
     * @param serviceType 服务类型，用于区分不同的服务
     * @param queueName   队列名称，具体标识一个队列
     * @return 返回一个队列键对象，如果特定的队列键不存在，则返回基于服务类型的通用队列键
     */
    private QueueKey getQueueKey(ServiceType serviceType, String queueName) {
        // 构造一个包含服务类型和队列名称的队列键
        QueueKey queueKey = new QueueKey(serviceType, queueName);
        // 如果分区大小映射中不包含当前队列键，则构造一个仅包含服务类型的队列键
        if (!partitionSizesMap.containsKey(queueKey)) {
            queueKey = new QueueKey(serviceType);
        }
        // 返回最终确定的队列键
        return queueKey;
    }


    /**
     * 重新计算分区分布
     * 此方法用于根据当前服务信息和其他服务信息来重新计算分区分布，
     * 它确保每个分区都被正确地分配给集群中的服务节点，并处理分区分配的变更
     *
     * @param currentService 当前服务的信息
     * @param otherServices  集群中其他服务的信息列表
     */
    @Override
    public synchronized void recalculatePartitions(ServiceInfo currentService, List<ServiceInfo> otherServices) {
        // 记录重新计算分区的日志
        log.info("Recalculating partitions");
        // 记录当前服务的信息
        logServiceInfo(currentService);
        // 记录其他服务的信息
        otherServices.forEach(this::logServiceInfo);

        // 创建一个Map，用于存储每个队列键对应的服务器列表
        Map<QueueKey, List<ServiceInfo>> queueServicesMap = new HashMap<>();
        // 将当前服务添加到Map中
        addNode(currentService, queueServicesMap);
        // 将其他服务添加到Map中
        for (ServiceInfo other : otherServices) {
            addNode(other, queueServicesMap);
        }
        // 对每个队列键对应的服务器列表进行排序
        queueServicesMap.values().forEach(list -> list.sort(Comparator.comparing(ServiceInfo::getServiceId)));

        // 创建一个新的Map，用于存储新的分区分布
        final ConcurrentMap<QueueKey, List<Integer>> newPartitions = new ConcurrentHashMap<>();
        // 遍历每个分区大小，计算新的分区分布
        partitionSizesMap.forEach((queueKey, size) -> {
            for (int i = 0; i < size; i++) {
                try {
                    // 获取负责当前队列键的服务器列表
                    List<ServiceInfo> servers = queueServicesMap.get(queueKey);
                    // 计算负责当前分区的服务器
                    ServiceInfo serviceInfo = servers == null || servers.isEmpty() ? null : servers.get(i % servers.size());
                    // 记录负责当前分区的服务器信息
                    log.info("Server responsible for {}[{}] - {}", queueKey, i, serviceInfo != null ? serviceInfo.getServiceId() : "none");
                    // 如果当前服务是负责当前分区的服务器，则将其添加到新的分区分布中
                    if (currentService.equals(serviceInfo)) {
                        newPartitions.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(i);
                    }
                } catch (Exception e) {
                    // 如果计算失败，记录警告日志
                    log.warn("Failed to resolve server responsible for {}[{}]", queueKey, i, e);
                }
            }
        });

        // 保存旧的分区分布
        final Map<QueueKey, List<Integer>> oldPartitions = myPartitions;
        // 更新当前的分区分布为新计算的分区分布
        myPartitions = newPartitions;

        // 记录当前服务负责的分区信息
        log.info("Current Server responsible partitions: {}", myPartitions);

        // 创建一个Map，用于存储分区变更的信息
        Map<QueueKey, Set<TopicPartitionInfo>> changedPartitionsMap = new HashMap<>();

        // 检查是否有分区被移除
        Set<QueueKey> removed = new HashSet<>();
        oldPartitions.forEach((queueKey, partitions) -> {
            if (!newPartitions.containsKey(queueKey)) {
                removed.add(queueKey);
            }
        });

        // 对于被移除的分区，将其添加到变更信息中
        removed.forEach(queueKey -> {
            changedPartitionsMap.put(queueKey, Collections.emptySet());
        });

        // 检查是否有分区的分布发生变化，并将变更信息添加到Map中
        myPartitions.forEach((queueKey, partitions) -> {
            if (!partitions.equals(oldPartitions.get(queueKey))) {
                Set<TopicPartitionInfo> tpiList = partitions.stream()
                        .map(partition -> buildTopicPartitionInfo(queueKey, partition))
                        .collect(Collectors.toSet());
                changedPartitionsMap.put(queueKey, tpiList);
            }
        });

        // 如果有分区变更，根据服务类型对变更进行分组，并发布分区变更事件
        if (!changedPartitionsMap.isEmpty()) {
            Map<ServiceType, Map<QueueKey, Set<TopicPartitionInfo>>> partitionsByServiceType = new HashMap<>();
            changedPartitionsMap.forEach((queueKey, partitions) -> {
                partitionsByServiceType.computeIfAbsent(queueKey.getType(), serviceType -> new HashMap<>())
                        .put(queueKey, partitions);
            });
            partitionsByServiceType.forEach(this::publishPartitionChangeEvent);
        }
    }


    /**
     * 发布分区变更事件
     * 当分区发生变化时，调用此方法来发布分区变更事件
     *
     * @param serviceType   服务类型，表明分区变更所属的服务
     * @param partitionsMap 包含分区信息的映射，键为队列键，值为一组分区信息
     */
    private void publishPartitionChangeEvent(ServiceType serviceType, Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap) {
        // 构建并记录分区变更的日志信息
        log.info("Partitions changed: {}", System.lineSeparator() + partitionsMap.entrySet().stream()
                .map(entry -> "[" + entry.getKey() + "] - [" + entry.getValue().stream()
                        .map(tpi -> tpi.getPartition().orElse(-1).toString()).sorted()
                        .collect(Collectors.joining(", ")) + "]")
                .collect(Collectors.joining(System.lineSeparator())));

        // 创建分区变更事件对象
        PartitionChangeEvent event = new PartitionChangeEvent(this, serviceType, partitionsMap);

        // 发布分区变更事件
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            // 如果发布事件失败，记录错误日志
            log.error("Failed to publish partition change event {}", event, e);
        }
    }


    private void logServiceInfo(ServiceInfo server) {
        log.info("Found server: {}", server.getServiceId());
    }

    /**
     * 向队列服务列表中添加服务实例节点
     * 此方法遍历服务实例的服务类型，并根据服务类型将其添加到相应的队列中
     * 仅当服务类型为APP时，才会将实例添加到队列中
     *
     * @param instance         服务实例信息，包含服务类型等信息
     * @param queueServiceList 存储队列键与服务实例列表映射的字典
     */
    private void addNode(ServiceInfo instance, Map<QueueKey, List<ServiceInfo>> queueServiceList) {
        // 遍历服务实例的所有服务类型
        for (String serviceTypeStr : instance.getServiceTypesList()) {
            // 将服务类型字符串转换为ServiceType枚举
            ServiceType serviceType = ServiceType.of(serviceTypeStr);
            // 检查服务类型是否为APP，因为只有APP类型的服务才会被添加到队列中
            if (ServiceType.APP.equals(serviceType)) {
                // 使用computeIfAbsent方法，如果指定的队列键不存在，则创建一个新的ArrayList，并与该键关联，然后添加服务实例
                queueServiceList.computeIfAbsent(new QueueKey(serviceType), key -> new ArrayList<>()).add(instance);
            }
        }
    }


    /**
     * 构建主题分区信息
     *
     * @param queueKey  队列键，用于标识特定的消息队列
     * @param partition 分区编号，指定需要构建分区信息的分区号
     * @return 返回一个TopicPartitionInfo对象，包含主题分区的相关信息
     */
    private TopicPartitionInfo buildTopicPartitionInfo(QueueKey queueKey, int partition) {
        // 获取与队列键相关联的分区列表
        List<Integer> partitions = myPartitions.get(queueKey);

        // 判断当前分区是否在列表中，并构建主题分区信息
        // 如果partitions不为空且包含partition，则表示该分区是有效的
        return buildTopicPartitionInfo(queueKey, partition, partitions != null && partitions.contains(partition));
    }


    /**
     * 构建主题分区信息对象
     * <p>
     * 此方法用于根据给定的队列键和分区号，以及是否为我的分区标志，来构建一个主题分区信息对象
     * 它主要从partitionTopicsMap中获取与队列键对应的主题，并结合其他参数，使用构建者模式创建并返回该对象
     *
     * @param queueKey    队列的唯一标识符，用于在partitionTopicsMap中查找对应的主题
     * @param partition   分区号，表示该主题下的具体分区
     * @param myPartition 标志位，表示当前分区是否属于“我的分区”
     * @return 返回构建好的TopicPartitionInfo对象
     */
    private TopicPartitionInfo buildTopicPartitionInfo(QueueKey queueKey, int partition, boolean myPartition) {
        return TopicPartitionInfo.builder()
                .topic(partitionTopicsMap.get(queueKey))
                .partition(partition)
                .myPartition(myPartition)
                .build();
    }


}