/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.provider.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;
import sanbing.jcpp.infrastructure.util.config.ThreadPoolConfiguration;
import sanbing.jcpp.proto.gen.UplinkProto.SessionCloseReason;
import sanbing.jcpp.protocol.domain.ProtocolSession;
import sanbing.jcpp.protocol.provider.ProtocolSessionRegistryProvider;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author 九筒
 */
@Service
@Slf4j
public class DefaultProtocolSessionRegistryProvider implements ProtocolSessionRegistryProvider {
    private static final int INIT_CACHE_LIMIT = 100_000;
    private static final int MAXIMUM_SIZE = 1_000_000;

    @Value("${service.protocol.sessions.default-inactivity-timeout-in-sec}")
    private int defaultInactivityTimeoutInSec;

    @Value("${service.protocol.sessions.default-state-check-interval-in-sec}")
    private int defaultStateCheckIntervalInSec;

    @Getter
    private final Cache<UUID, ProtocolSession> sessionCache = buildCache();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(JCPPThreadFactory.forName("session-state-checker"));

    @PostConstruct
    public void init() {
        scheduledExecutorService.scheduleAtFixedRate(() -> sessionCache.asMap().forEach((id, session) -> {
            if (session.getLastActivityTime().isBefore(LocalDateTime.now().minusSeconds(defaultInactivityTimeoutInSec))) {
                session.close(SessionCloseReason.SESSION_CLOSE_DESTRUCTION);
                unregister(session.getId());
            }
        }), defaultStateCheckIntervalInSec, defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduledExecutorService.shutdownNow();
    }

    @Override
    public void register(ProtocolSession protocolSession) {

        if (log.isDebugEnabled()) {
            log.debug("Registering session {}", protocolSession);
        }

        sessionCache.put(protocolSession.getId(), protocolSession);

    }

    @Override
    public void unregister(UUID sessionId) {

        log.info("Unregistering session {}", sessionId);

        sessionCache.invalidate(sessionId);
    }

    @Override
    public ProtocolSession get(UUID sessionId) {

        log.debug("Get session {}", sessionId);

        return sessionCache.get(sessionId, uuid -> null);
    }

    @Override
    public void activate(ProtocolSession protocolSession) {

        if (log.isDebugEnabled()) {
            log.debug("Activating session {}", protocolSession);
        }

        protocolSession.setLastActivityTime(LocalDateTime.now());

        sessionCache.put(protocolSession.getId(), protocolSession);
    }

    private Cache<UUID, ProtocolSession> buildCache() {
        return Caffeine.newBuilder()
                .initialCapacity(INIT_CACHE_LIMIT)
                .maximumSize(MAXIMUM_SIZE)
                .executor(ThreadPoolConfiguration.JCPP_COMMON_THREAD_POOL)
                .build();
    }
}