/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.discovery;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import sanbing.jcpp.infrastructure.util.SystemUtil;
import sanbing.jcpp.proto.gen.ClusterProto.ServiceInfo;
import sanbing.jcpp.proto.gen.ClusterProto.SystemInfoProto;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;


/**
 * @author 九筒
 */
@Component
@Slf4j
public class DefaultServiceInfoProvider implements ServiceInfoProvider {

    @Value("${service.id:#{null}}")
    @Getter
    private String serviceId;

    @Getter
    @Value("${service.type:monolith}")
    private String serviceType;

    private List<ServiceType> serviceTypes;

    private ServiceInfo serviceInfo;

    @Getter
    private String hostAddress;

    @Getter
    @Value("${server.port}")
    private int restPort;

    @Getter
    @Value("${service.protocol.rpc.port:9090}")
    private int grpcPort;

    @PostConstruct
    public void init() throws UnknownHostException {
        if (!StringUtils.hasText(this.serviceId)) {
            try {
                this.serviceId = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                this.serviceId = RandomStringUtils.secure().nextAlphabetic(10);
            }
        }
        log.info("Current Service ID: {}", serviceId);

        hostAddress = InetAddress.getLocalHost().getHostAddress();

        log.info("Current Service HostAddress: {}, RestPort:{}, GrpcPort:{}", hostAddress, restPort, grpcPort);
        if (serviceType.equalsIgnoreCase("monolith")) {
            serviceTypes = List.of(ServiceType.values());
        } else {
            serviceTypes = Collections.singletonList(ServiceType.of(serviceType));
        }

        generateNewServiceInfoWithCurrentSystemInfo();
    }


    @Override
    public boolean isMonolith() {
        return "monolith".equals(getServiceType());
    }

    @Override
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public ServiceInfo generateNewServiceInfoWithCurrentSystemInfo() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setServiceId(serviceId)
                .addAllServiceTypes(serviceTypes.stream().map(ServiceType::name).toList())
                .setSystemInfo(getCurrentSystemInfoProto());
        return serviceInfo = builder.build();
    }

    private SystemInfoProto getCurrentSystemInfoProto() {
        SystemInfoProto.Builder builder = SystemInfoProto.newBuilder();

        SystemUtil.getCpuUsage().ifPresent(builder::setCpuUsage);
        SystemUtil.getMemoryUsage().ifPresent(builder::setMemoryUsage);
        SystemUtil.getDiscSpaceUsage().ifPresent(builder::setDiskUsage);

        SystemUtil.getCpuCount().ifPresent(builder::setCpuCount);
        SystemUtil.getTotalMemory().ifPresent(builder::setTotalMemory);
        SystemUtil.getTotalDiscSpace().ifPresent(builder::setTotalDiscSpace);

        return builder.build();
    }

}

