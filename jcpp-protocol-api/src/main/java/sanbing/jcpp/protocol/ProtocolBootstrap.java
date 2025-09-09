/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import sanbing.jcpp.protocol.cfg.ForwarderCfg;
import sanbing.jcpp.protocol.cfg.ProtocolCfg;
import sanbing.jcpp.protocol.cfg.TcpCfg;
import sanbing.jcpp.protocol.cfg.enums.ForwarderType;
import sanbing.jcpp.protocol.forwarder.Forwarder;
import sanbing.jcpp.protocol.forwarder.KafkaForwarder;
import sanbing.jcpp.protocol.forwarder.MemoryForwarder;
import sanbing.jcpp.protocol.listener.Listener;
import sanbing.jcpp.protocol.listener.tcp.TcpListener;

import static org.springframework.boot.actuate.health.Status.UP;

/**
 * @author 九筒
 */
@Slf4j
public abstract class ProtocolBootstrap implements HealthIndicator {

    @Resource
    protected ProtocolContext protocolContext;

    protected ProtocolCfg protocolCfg;

    protected Listener listener;

    protected Forwarder forwarder;

    @PostConstruct
    public void init() throws InterruptedException {
        String protocolName = getProtocolName();

        log.info("Protocol Service [{}] Initializing...", protocolName);

        protocolCfg = protocolContext.getProtocolsConfigProvider().loadConfig(protocolName);

        ForwarderCfg forwarderCfg = protocolCfg.getForwarder();

        if (protocolContext.getServiceInfoProvider().isMonolith() && forwarderCfg.getType() == ForwarderType.memory) {

            forwarder = new MemoryForwarder(getProtocolName(), forwarderCfg,
                    protocolContext.getStatsFactory(),
                    protocolContext.getAppQueueFactory(),
                    protocolContext.getPartitionProvider(),
                    protocolContext.getServiceInfoProvider());

        } else if (forwarderCfg.getType() == ForwarderType.kafka) {

            forwarder = new KafkaForwarder(getProtocolName(), forwarderCfg,
                    protocolContext.getStatsFactory(),
                    protocolContext.getAppQueueFactory(),
                    protocolContext.getPartitionProvider(),
                    protocolContext.getServiceInfoProvider());
        } else {
            throw new IllegalArgumentException("未知的转发器类型: " + forwarderCfg.getType());
        }

        TcpCfg tcpCfg = protocolCfg.getListener().getTcp();

        if (tcpCfg != null) {

            listener = new TcpListener(protocolName, tcpCfg, messageProcessor(), protocolContext.getStatsFactory());
        }

        _init();
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        log.info("{} destroy...", getProtocolName());

        if (listener != null) {
            listener.destroy();
        }

        if (forwarder != null) {
            forwarder.destroy();
        }

        _destroy();
    }


    @Override
    public Health health() {
        Health.Builder healthBuilder;

        if (listener != null && listener.health().getStatus() == UP && forwarder != null && forwarder.health().getStatus() == UP) {
            healthBuilder = Health.up();
        } else {
            healthBuilder = Health.down();
        }

        if (listener != null) {
            healthBuilder.withDetail("listener", listener.health().getStatus());
        }

        if (forwarder != null) {
            healthBuilder.withDetail("forwarder", forwarder.health().getStatus());
        }

        return healthBuilder.build();
    }


    protected abstract String getProtocolName();

    protected abstract void _init();

    protected abstract void _destroy();

    protected abstract ProtocolMessageProcessor messageProcessor();

}