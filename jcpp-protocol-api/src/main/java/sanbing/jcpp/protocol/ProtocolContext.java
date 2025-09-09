/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol;

import io.netty.util.ResourceLeakDetector;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.queue.discovery.PartitionProvider;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.infrastructure.queue.provider.AppQueueFactory;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.config.ShardingThreadPool;
import sanbing.jcpp.protocol.provider.ProtocolSessionRegistryProvider;
import sanbing.jcpp.protocol.provider.ProtocolsConfigProvider;

/**
 * @author 九筒
 */
@Component
@Getter
@Slf4j
public class ProtocolContext {

    private final StatsFactory statsFactory;

    private final ProtocolsConfigProvider protocolsConfigProvider;

    private final ProtocolSessionRegistryProvider protocolSessionRegistryProvider;

    private final ServiceInfoProvider serviceInfoProvider;

    private final PartitionProvider partitionProvider;

    private final AppQueueFactory appQueueFactory;

    private final ShardingThreadPool shardingThreadPool;

    public ProtocolContext(StatsFactory statsFactory,
                           ProtocolsConfigProvider protocolsConfigProvider,
                           ProtocolSessionRegistryProvider protocolSessionRegistryProvider,
                           ServiceInfoProvider serviceInfoProvider,
                           @Autowired(required = false) PartitionProvider partitionProvider,
                           @Autowired(required = false) AppQueueFactory appQueueFactory,
                           ShardingThreadPool shardingThreadPool) {
        this.statsFactory = statsFactory;
        this.protocolsConfigProvider = protocolsConfigProvider;
        this.protocolSessionRegistryProvider = protocolSessionRegistryProvider;
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionProvider = partitionProvider;
        this.appQueueFactory = appQueueFactory;
        this.shardingThreadPool = shardingThreadPool;
    }

    @PostConstruct
    public void init() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        log.info("Setting resource leak detector level to {}", ResourceLeakDetector.Level.DISABLED);
    }
}