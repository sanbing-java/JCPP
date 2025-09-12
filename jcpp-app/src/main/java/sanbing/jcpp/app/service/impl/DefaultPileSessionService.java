/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.app.data.kv.*;
import sanbing.jcpp.app.data.page.PageDataIterable;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.PileService;
import sanbing.jcpp.app.service.PileSessionService;
import sanbing.jcpp.app.service.cache.session.PileSessionCacheKey;
import sanbing.jcpp.infrastructure.cache.CacheValueWrapper;
import sanbing.jcpp.infrastructure.cache.TransactionalCache;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.infrastructure.queue.discovery.PartitionProvider;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceType;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PileSession管理服务默认实现
 *
 * @author 九筒
 */
@Service
@Slf4j
public class DefaultPileSessionService implements PileSessionService {

    @Resource
    private TransactionalCache<PileSessionCacheKey, PileSession> pileSessionCache;

    @Resource
    private PileService pileService;

    @Resource
    private AttributeService attributeService;


    @Resource
    private PartitionProvider partitionProvider;

    @Value("${service.protocol.sessions.default-inactivity-timeout-in-sec:600}")
    private int inactivityTimeoutInSec;

    private static final int FETCH_PAGE_SIZE = 1000;

    @Override
    public PileSession createOrUpdateSession(UplinkQueueMessage uplinkQueueMessage,
                                             Pile pile,
                                             String remoteAddress,
                                             String nodeId,
                                             String nodeIp,
                                             int restPort,
                                             int grpcPort) {

        String pileCode = pile.getPileCode();
        PileSessionCacheKey cacheKey = new PileSessionCacheKey(pileCode);

        // 直接创建或更新会话，一步缓存操作
        PileSession pileSession = createSession(uplinkQueueMessage, pile, remoteAddress,
                nodeId, nodeIp, restPort, grpcPort);

        pileSessionCache.put(cacheKey, pileSession);

        log.debug("保存PileSession: pileCode={}, sessionId={}",
                pileCode, pileSession.getProtocolSessionId());

        return pileSession;
    }

    @Override
    public Optional<PileSession> getSession(String pileCode) {
        try {
            CacheValueWrapper<PileSession> wrapper = pileSessionCache.get(new PileSessionCacheKey(pileCode));
            if (wrapper != null && wrapper.get() != null) {
                return Optional.of(wrapper.get());
            }
        } catch (Exception e) {
            log.warn("获取PileSession失败: pileCode={}", pileCode, e);
        }
        return Optional.empty();
    }


    @Override
    public boolean hasActiveSession(String pileCode) {
        return getSession(pileCode).isPresent();
    }


    @Override
    public void removeSession(String pileCode) {
        try {
            PileSessionCacheKey cacheKey = new PileSessionCacheKey(pileCode);
            pileSessionCache.evict(cacheKey);
            log.info("PileSession已移除: pileCode={}", pileCode);
        } catch (Exception e) {
            log.error("移除PileSession失败: pileCode={}", pileCode, e);
        }
    }


    @Override
    public List<String> checkActiveSessions(List<String> pileCodes) {
        if (pileCodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> activePileCodes = new ArrayList<>();

        for (String pileCode : pileCodes) {
            if (hasActiveSession(pileCode)) {
                activePileCodes.add(pileCode);
            }
        }

        log.debug("批量检查会话状态完成: 检查数量={}, 活跃数量={}",
                pileCodes.size(), activePileCodes.size());

        return activePileCodes;
    }

    /**
     * 执行会话健康检查和状态清洗
     * 基于 service.protocol.sessions 配置进行兜底检查
     * 使用分区逻辑避免多实例冲突
     * 维护充电桩的 status、connectedAt、disconnectedAt、lastActiveTime 属性
     */
    @Scheduled(fixedDelayString = "#{${service.protocol.sessions.default-state-check-interval-in-sec:60} * 1000}")
    public void performSessionHealthCheck() {
        try {
            log.debug("开始执行会话健康检查和状态清洗");

            long startTime = System.currentTimeMillis();
            int processedCount = 0;
            int updatedCount = 0;
            int onlineCount = 0;
            int offlineCount = 0;

            // 计算超时阈值
            long currentTime = System.currentTimeMillis();
            long timeoutThreshold = currentTime - (inactivityTimeoutInSec * 1000L);

            // 分页处理所有充电桩
            PageDataIterable<Pile> pileIterable = new PageDataIterable<>(
                    pileService::findPilesWithPagination,
                    FETCH_PAGE_SIZE
            );

            for (Pile pile : pileIterable) {
                processedCount++;
                String pileCode = pile.getPileCode();

                // 使用分区逻辑判断是否由本实例处理
                if (!isMyPartition(pileCode)) {
                    continue;
                }

                try {
                    PileHealthCheckResult result = checkAndUpdatePileStatus(pile, timeoutThreshold, currentTime);
                    if (result.statusUpdated) {
                        updatedCount++;
                    }

                    // 统计最终状态
                    if (PileStatusEnum.ONLINE.name().equals(result.finalStatus)) {
                        onlineCount++;
                    } else {
                        offlineCount++;
                    }

                } catch (Exception e) {
                    log.error("处理充电桩健康检查失败: pileCode={}", pileCode, e);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("会话健康检查完成: 处理数量={}, 更新数量={}, 在线数量={}, 离线数量={}, 耗时={}ms",
                    processedCount, updatedCount, onlineCount, offlineCount, endTime - startTime);

        } catch (Exception e) {
            log.error("会话健康检查执行失败", e);
        }
    }

    /**
     * 健康检查结果
     */
    private record PileHealthCheckResult(boolean statusUpdated, String finalStatus) {
    }

    /**
     * 判断充电桩是否由当前实例处理（分区逻辑）
     */
    private boolean isMyPartition(String pileCode) {
        try {
            TopicPartitionInfo partitionInfo = partitionProvider.resolve(ServiceType.APP, "pile-session", pileCode);
            return partitionInfo.isMyPartition();
        } catch (Exception e) {
            log.warn("分区判断失败: pileCode={}, 默认由当前实例处理", pileCode, e);
            return true; // 出错时默认处理，避免遗漏
        }
    }

    /**
     * 检查并更新充电桩状态（优化版本）
     */
    private PileHealthCheckResult checkAndUpdatePileStatus(Pile pile, long timeoutThreshold, long currentTime) {
        String pileCode = pile.getPileCode();
        UUID pileId = pile.getId();

        // 检查是否有活跃会话
        boolean hasActiveSession = hasActiveSession(pileCode);

        // 获取当前状态和最后活跃时间（通过AttributeService一次性获取）
        try {
            List<AttributeKvEntry> attributes = attributeService.find(pileId,
                    List.of(AttrKeyEnum.STATUS.getCode(), AttrKeyEnum.LAST_ACTIVE_TIME.getCode())).get();

            String currentStatus = null;
            Long lastActiveTime = null;

            for (AttributeKvEntry attr : attributes) {
                if (AttrKeyEnum.STATUS.getCode().equals(attr.getKey())) {
                    currentStatus = attr.getStrValue().orElse(PileStatusEnum.OFFLINE.name());
                } else if (AttrKeyEnum.LAST_ACTIVE_TIME.getCode().equals(attr.getKey())) {
                    lastActiveTime = attr.getLongValue().orElse(null);
                }
            }

            // 如果当前状态为空，默认为离线
            if (currentStatus == null) {
                currentStatus = PileStatusEnum.OFFLINE.name();
            }

            // 决定目标状态
            String targetStatus = determineTargetStatusOptimized(hasActiveSession, currentStatus, lastActiveTime, timeoutThreshold);

            // 更新状态（如果需要）
            boolean statusUpdated = false;
            if (!targetStatus.equals(currentStatus)) {
                updatePileStatusOptimized(pileId, targetStatus, currentTime);
                log.info("健康检查更新充电桩状态: pileCode={}, 从 {} 更新为 {}, 会话状态={}",
                        pileCode, currentStatus, targetStatus, hasActiveSession ? "有" : "无");
                statusUpdated = true;
            }

            return new PileHealthCheckResult(statusUpdated, targetStatus);

        } catch (Exception e) {
            log.error("检查充电桩状态失败: pileCode={}", pileCode, e);
            // 异常情况下，返回当前可能的状态
            String fallbackStatus = hasActiveSession ? PileStatusEnum.ONLINE.name() : PileStatusEnum.OFFLINE.name();
            return new PileHealthCheckResult(false, fallbackStatus);
        }
    }

    /**
     * 优化的状态判断逻辑
     */
    private String determineTargetStatusOptimized(boolean hasActiveSession, String currentStatus, Long lastActiveTime, long timeoutThreshold) {
        // 有活跃会话，应该在线
        if (hasActiveSession) {
            return PileStatusEnum.ONLINE.name();
        }

        // 无活跃会话，需要检查最后活跃时间
        boolean isCurrentlyOnline = PileStatusEnum.ONLINE.name().equals(currentStatus);
        if (isCurrentlyOnline) {
            // 当前显示在线但无会话，检查最后活跃时间
            if (lastActiveTime != null && lastActiveTime < timeoutThreshold) {
                return PileStatusEnum.OFFLINE.name();
            } else if (lastActiveTime == null) {
                // 没有最后活跃时间记录，设为离线
                return PileStatusEnum.OFFLINE.name();
            }
        }

        // 其他情况保持当前状态
        return currentStatus;
    }

    /**
     * 优化的状态更新方法（批量更新相关属性）
     */
    private void updatePileStatusOptimized(UUID pileId, String status, long currentTime) {
        try {
            List<AttributeKvEntry> attributesToUpdate = new ArrayList<>();

            // 更新状态属性
            attributesToUpdate.add(new BaseAttributeKvEntry(
                    new StringDataEntry(AttrKeyEnum.STATUS.getCode(), status),
                    currentTime));

            // 根据状态更新相应的时间戳
            if (PileStatusEnum.ONLINE.name().equals(status)) {
                // 设为在线时更新连接时间和最后活跃时间
                attributesToUpdate.add(new BaseAttributeKvEntry(
                        new LongDataEntry(AttrKeyEnum.CONNECTED_AT.getCode(), currentTime),
                        currentTime));
                attributesToUpdate.add(new BaseAttributeKvEntry(
                        new LongDataEntry(AttrKeyEnum.LAST_ACTIVE_TIME.getCode(), currentTime),
                        currentTime));

                // 删除断开连接时间（如果存在）
                attributeService.removeAll(pileId, List.of(AttrKeyEnum.DISCONNECTED_AT.getCode()));

            } else if (PileStatusEnum.OFFLINE.name().equals(status)) {
                // 设为离线时更新断开连接时间
                attributesToUpdate.add(new BaseAttributeKvEntry(
                        new LongDataEntry(AttrKeyEnum.DISCONNECTED_AT.getCode(), currentTime),
                        currentTime));
            }

            // 批量保存属性
            attributeService.save(pileId, attributesToUpdate);

        } catch (Exception e) {
            log.error("更新充电桩状态失败: pileId={}, status={}", pileId, status, e);
            throw e;
        }
    }


    /**
     * 创建PileSession实例
     * 从原来的DefaultPileProtocolService中提取的逻辑
     */
    private PileSession createSession(UplinkQueueMessage uplinkQueueMessage,
                                      Pile pile,
                                      String remoteAddress,
                                      String nodeId,
                                      String nodeIp,
                                      int restPort,
                                      int grpcPort) {

        PileSession pileSession = new PileSession(pile.getId(), pile.getPileCode(),
                uplinkQueueMessage.getProtocolName());

        // 设置协议会话ID
        pileSession.setProtocolSessionId(new UUID(uplinkQueueMessage.getSessionIdMSB(),
                uplinkQueueMessage.getSessionIdLSB()));

        // 设置网络信息
        pileSession.setRemoteAddress(remoteAddress);
        pileSession.setNodeId(nodeId);
        pileSession.setNodeIp(nodeIp);
        pileSession.setNodeRestPort(restPort);
        pileSession.setNodeGrpcPort(grpcPort);


        return pileSession;
    }
}
