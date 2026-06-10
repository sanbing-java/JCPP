/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import sanbing.jcpp.infrastructure.util.annotation.AfterStartUp;
import sanbing.jcpp.proto.gen.ClusterProto.ServiceInfo;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class DummyDiscoveryProvider implements DiscoveryProvider {

    private final ServiceInfoProvider serviceInfoProvider;
    private final PartitionProvider partitionProvider;

    public DummyDiscoveryProvider(ServiceInfoProvider serviceInfoProvider, PartitionProvider partitionProvider) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionProvider = partitionProvider;
    }

    @AfterStartUp(order = AfterStartUp.DISCOVERY_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        partitionProvider.recalculatePartitions(serviceInfoProvider.getServiceInfo(), Collections.emptyList());
    }

    @Override
    public List<ServiceInfo> getOtherServers() {
        return Collections.emptyList();
    }

}
