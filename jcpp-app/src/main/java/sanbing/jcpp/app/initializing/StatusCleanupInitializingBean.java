/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.initializing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.entity.Attribute;
import sanbing.jcpp.app.dal.mapper.AttributeMapper;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.app.data.kv.*;
import sanbing.jcpp.app.data.page.PageDataIterable;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.PileService;
import sanbing.jcpp.app.service.cache.session.PileSessionCacheKey;
import sanbing.jcpp.infrastructure.cache.CacheValueWrapper;
import sanbing.jcpp.infrastructure.cache.TransactionalCache;

import java.util.UUID;

/**
 * 状态清洗组件
 * 在Spring容器初始化时执行充电桩状态的全量清洗
 * 如果失败会阻止应用启动，确保数据状态一致性
 * 
 * @author baigod
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10) // 在数据库初始化之后执行
public class StatusCleanupInitializingBean implements InitializingBean {
    
    private final PileService pileService;
    private final TransactionalCache<PileSessionCacheKey, PileSession> pileSessionCache;
    private final AttributeMapper attributeMapper;
    private final AttributeService attributeService;

    @Value("${service.protocol.sessions.default-inactivity-timeout-in-sec:600}")
    private int inactivityTimeoutInSec;
    
    // 分页大小，控制每次处理的充电桩数量
    private static final int FETCH_PAGE_SIZE = 1000;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("开始执行状态清洗...");
        
        try {
            performStatusCleanup();
            log.info("状态清洗执行完成");
        } catch (Exception e) {
            log.error("状态清洗执行失败，应用启动终止", e);
            // 抛出异常阻止Spring容器启动，确保数据状态一致性
            throw new RuntimeException("系统状态清理失败，应用无法在数据状态不一致的情况下启动", e);
        }
    }

    /**
     * 执行充电桩状态清洗任务。
     *
     * 该方法的主要功能是检查所有充电桩的状态，并根据预定义的逻辑进行必要的更新，以确保数据库和缓存的状态一致性。
     *
     * 核心逻辑：
     * 1. 数据库状态为准，缓存为辅助判断。
     * 2. 有会话连接 = 在线，无会话连接 = 可能离线。
     * 3. 确保数据库和缓存的最终一致性。
     *
     * 处理场景：
     * - 场景1：有会话连接，但数据库状态为OFFLINE -> 更新为ONLINE。
     * - 场景2：无会话连接，但数据库状态为ONLINE -> 检查最后活跃时间，超时则设为OFFLINE。
     * - 场景3：无会话连接，数据库状态也为OFFLINE -> 跳过。
     * - 场景4：有会话连接，数据库状态也为ONLINE -> 跳过。
     *
     * 异常处理：
     * - 网络分区：依赖最后活跃时间判断。
     * - 系统重启：会话丢失，通过重连恢复状态。
     * - 缓存异常：以数据库状态为准。
     * - 数据库异常：记录错误，继续处理其他设备。
     *
     * @throws RuntimeException 如果在执行状态清洗过程中发生不可恢复的异常。
     */
    private void performStatusCleanup() {
        log.info("开始执行充电桩状态清洗...");

        long startTime = System.currentTimeMillis();
        int processedCount = 0; // 已处理的充电桩数量
        int updatedCount = 0;   // 状态已更新的充电桩数量
        int onlineCount = 0;    // 最终在线的充电桩数量
        int offlineCount = 0;   // 最终离线的充电桩数量

        // 计算不活跃阈值时间，用于判断是否超时
        long currentTime = System.currentTimeMillis();
        long timeoutThreshold = currentTime - (inactivityTimeoutInSec * 1000L);

        try {
            // 使用分页查询所有充电桩，避免一次性加载过多数据导致内存溢出
            PageDataIterable<sanbing.jcpp.app.dal.entity.Pile> pileIterable = new PageDataIterable<>(
                pileService::findPilesWithPagination,
                FETCH_PAGE_SIZE
            );

            for (var pile : pileIterable) {
                processedCount++;
                String pileCode = pile.getPileCode();

                try {
                    // 获取当前数据库中的状态
                    String currentDbStatus = pileService.findPileStatus(pile.getId());
                    boolean isCurrentlyOnline = PileStatusEnum.ONLINE.name().equals(currentDbStatus);

                    // 检查是否有活跃的会话连接
                    boolean hasActiveSession = checkActiveSession(pileCode);

                    // 根据会话状态、数据库状态和超时时间决定目标状态
                    String targetStatus = determineTargetStatus(hasActiveSession, isCurrentlyOnline, pile.getId(), timeoutThreshold);

                    // 如果需要更新状态，则执行更新操作
                    if (!targetStatus.equals(currentDbStatus)) {
                        updatePileStatusWithTimestamp(pile.getId(), targetStatus, currentTime);
                        log.info("更新充电桩状态: pileCode={}, 从 {} 更新为 {}, 会话状态={}",
                                pileCode, currentDbStatus, targetStatus, hasActiveSession ? "有" : "无");
                        updatedCount++;
                    }

                    // 统计最终状态
                    if (PileStatusEnum.ONLINE.name().equals(targetStatus)) {
                        onlineCount++;
                    } else {
                        offlineCount++;
                    }

                } catch (Exception e) {
                    // 捕获单个充电桩处理过程中的异常，记录日志并继续处理其他充电桩
                    log.error("处理充电桩状态清洗失败: pileCode={}", pileCode, e);
                }
            }

        } catch (Exception e) {
            // 捕获全局异常，记录日志并抛出运行时异常
            log.error("执行状态清洗过程中发生异常", e);
            throw new RuntimeException("状态清洗执行失败", e);
        }

        long endTime = System.currentTimeMillis();
        // 记录状态清洗的汇总信息，包括处理数量、更新数量、在线数量、离线数量和耗时
        log.info("充电桩状态清洗完成: 处理数量={}, 更新数量={}, 在线数量={}, 离线数量={}, 耗时={}ms",
                processedCount, updatedCount, onlineCount, offlineCount, endTime - startTime);
    }

    /**
     * 检查充电桩是否有活跃的会话连接
     */
    private boolean checkActiveSession(String pileCode) {
        try {
            CacheValueWrapper<PileSession> sessionWrapper = pileSessionCache.get(new PileSessionCacheKey(pileCode));
            return sessionWrapper != null && sessionWrapper.get() != null;
        } catch (Exception e) {
            log.warn("检查充电桩会话失败: pileCode={}", pileCode, e);
            return false;
        }
    }

    /**
     * 根据会话状态、数据库状态和超时时间决定目标状态
     */
    private String determineTargetStatus(boolean hasActiveSession, boolean isCurrentlyOnline, UUID pileId, long timeoutThreshold) {
        // 有活跃会话，应该在线
        if (hasActiveSession) {
            return PileStatusEnum.ONLINE.name();
        }
        
        // 无活跃会话，需要检查最后活跃时间
        if (isCurrentlyOnline) {
            // 当前显示在线但无会话，检查最后活跃时间
            Attribute lastActiveAttr = attributeMapper.findByEntityAndKey(pileId, AttrKeyEnum.LAST_ACTIVE_TIME.getCode());
            if (lastActiveAttr != null && lastActiveAttr.getLongV() != null) {
                long lastActiveTime = lastActiveAttr.getLongV();
                if (lastActiveTime < timeoutThreshold) {
                    // 超时了，应该设为离线
                    log.debug("充电桩超时未活跃，设为离线: pileId={}, lastActiveTime={}, threshold={}",
                            pileId, lastActiveTime, timeoutThreshold);
                    return PileStatusEnum.OFFLINE.name();
                }
            } else {
                // 没有最后活跃时间记录，但当前显示在线且无会话，保守地设为离线
                log.debug("充电桩无最后活跃时间记录但当前在线且无会话，设为离线: pileId={}", pileId);
                return PileStatusEnum.OFFLINE.name();
            }
        }
        
        // 其他情况保持当前状态
        return isCurrentlyOnline ? PileStatusEnum.ONLINE.name() : PileStatusEnum.OFFLINE.name();
    }

    /**
     * 更新充电桩状态，包括时间戳
     */
    private void updatePileStatusWithTimestamp(UUID pileId, String status, long currentTime) {
        try {
            // 更新状态属性
            AttributeKvEntry statusAttr = new BaseAttributeKvEntry(
                new StringDataEntry(AttrKeyEnum.STATUS.getCode(), status), 
                currentTime
            );
            attributeService.save(pileId, statusAttr);

            // 根据状态更新相应的时间戳
            if (PileStatusEnum.ONLINE.name().equals(status)) {
                // 设为在线时更新连接时间和最后活跃时间
                updatePileAttribute(pileId, AttrKeyEnum.CONNECTED_AT, currentTime);
                updatePileAttribute(pileId, AttrKeyEnum.LAST_ACTIVE_TIME, currentTime);
            } else if (PileStatusEnum.OFFLINE.name().equals(status)) {
                // 设为离线时更新断开连接时间
                updatePileAttribute(pileId, AttrKeyEnum.DISCONNECTED_AT, currentTime);
            }
            
        } catch (Exception e) {
            log.error("更新充电桩状态失败: pileId={}, status={}", pileId, status, e);
            throw e;
        }
    }

    /**
     * 更新充电桩特定属性
     */
    private void updatePileAttribute(UUID pileId, AttrKeyEnum key, long value) {
        long currentTime = System.currentTimeMillis();
        AttributeKvEntry attr = new BaseAttributeKvEntry(
            new LongDataEntry(key.getCode(), value), 
            currentTime
        );
        attributeService.save(pileId, attr);
    }
}
