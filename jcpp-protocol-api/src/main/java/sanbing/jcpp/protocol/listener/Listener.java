/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.listener;

import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.boot.actuate.health.Health;
import sanbing.jcpp.infrastructure.stats.MessagesStats;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 九筒
 */
public abstract class Listener {

    @Getter
    private final String protocolName;

    @Getter
    private final ProtocolMessageProcessor protocolMessageProcessor;

    protected final AtomicInteger connectionsGauge = new AtomicInteger();
    protected final MessagesStats uplinkMsgStats;
    protected final MessagesStats downlinkMsgStats;
    protected final Timer downlinkTimer;

    protected final ChannelHandlerParameter parameter;

    protected Listener(String protocolName, ProtocolMessageProcessor protocolMessageProcessor, StatsFactory statsFactory) {
        this.protocolName = protocolName;
        this.protocolMessageProcessor = protocolMessageProcessor;

        statsFactory.createGauge("openConnections", connectionsGauge, "protocol", protocolName);
        this.uplinkMsgStats = statsFactory.createMessagesStats("listenerUplinkMessage", "protocol", protocolName);
        this.downlinkMsgStats = statsFactory.createMessagesStats("listenerDownlinkMessage", "protocol", protocolName);
        this.downlinkTimer = statsFactory.createTimer("listenerDownlink", "protocol", protocolName);

        this.parameter = new ChannelHandlerParameter(protocolName, protocolMessageProcessor, connectionsGauge, uplinkMsgStats, downlinkMsgStats, downlinkTimer);
    }

    public abstract Health health();

    public abstract void destroy() throws InterruptedException;
}