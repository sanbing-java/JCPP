/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkRequestMessage;
import sanbing.jcpp.proto.gen.UplinkProto.SessionCloseEventProto;
import sanbing.jcpp.proto.gen.UplinkProto.SessionCloseReason;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.forwarder.Forwarder;

import java.io.Closeable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

/**
 * @author 九筒
 */
@Getter
@Slf4j
public abstract class ProtocolSession implements Closeable {

    private static final int REQUEST_CACHE_LIMIT = 1000;

    protected final String protocolName;

    protected final UUID id;

    @Setter
    protected LocalDateTime lastActivityTime;

    protected final Set<String> pileCodeSet;

    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();

    private final Cache<String, Object> requestCache = Caffeine.newBuilder()
            .initialCapacity(REQUEST_CACHE_LIMIT)
            .maximumSize(REQUEST_CACHE_LIMIT)
            .expireAfterAccess(Duration.ofMinutes(1))
            .build();

    @Setter
    private Forwarder forwarder;

    protected ProtocolSession(String protocolName) {
        this.protocolName = protocolName;
        this.pileCodeSet = new LinkedHashSet<>();
        this.id = UUID.randomUUID();
        this.lastActivityTime = LocalDateTime.now();
    }

    public abstract void onDownlink(DownlinkRequestMessage downlinkMsg);

    @Override
    public void close() {
        close(SessionCloseReason.SESSION_CLOSE_DESTRUCTION);
    }

    public void close(SessionCloseReason reason) {
        log.info("[{}] Protocol会话关闭，原因: {}", this, reason);

        scheduledFutures.values().forEach(scheduledFuture -> scheduledFuture.cancel(true));
        scheduledFutures.clear();

        // 转发会话关闭事件到后端
        if (forwarder != null && !pileCodeSet.isEmpty()) {
            
            for (String pileCode : pileCodeSet) {
                SessionCloseEventProto sessionCloseEvent = SessionCloseEventProto.newBuilder()
                        .setPileCode(pileCode)
                        .setReason(reason)
                        .setAdditionalInfo("Session closed: " + reason)
                        .build();
                
                UplinkQueueMessage uplinkQueueMessage = UplinkQueueMessage.newBuilder()
                        .setMessageIdMSB(UUID.randomUUID().getMostSignificantBits())
                        .setMessageIdLSB(UUID.randomUUID().getLeastSignificantBits())
                        .setSessionIdMSB(id.getMostSignificantBits())
                        .setSessionIdLSB(id.getLeastSignificantBits())
                        .setMessageKey(pileCode + "_session_close")
                        .setProtocolName(protocolName)
                        .setSessionCloseEventProto(sessionCloseEvent)
                        .build();
                
                try {
                    forwarder.sendMessage(uplinkQueueMessage);
                    log.debug("[{}] 会话关闭事件已转发，桩编码: {}, 原因: {}", this, pileCode, reason);
                } catch (Exception e) {
                    log.error("[{}] 转发会话关闭事件失败，桩编码: {}", this, pileCode, e);
                }
            }
        }
    }
    


    @Override
    public String toString() {
        return "[" + id + "]" + pileCodeSet;
    }

    public void addPileCode(String pileCode) {
        this.pileCodeSet.add(pileCode);
    }

    public void addSchedule(String name, Function<String, ScheduledFuture<?>> scheduledFutureFunction) {
        scheduledFutures.computeIfAbsent(name, scheduledFutureFunction);
    }
}