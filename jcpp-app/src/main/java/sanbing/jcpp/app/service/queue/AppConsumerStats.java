/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.queue;

import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.stats.StatsCounter;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AppConsumerStats {
    // StatsCounter相关常量
    public static final String STATS_KEY_APP_CONSUMER = "appConsumer";
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String LOGIN_EVENTS = "loginEvents";
    public static final String HEARTBEAT_EVENTS = "heartBeatEvents";
    public static final String GUN_RUN_STATUS_EVENTS = "gunRunStatusEvents";
    public static final String CHARGING_PROGRESS_EVENTS = "chargingProgressEvents";
    public static final String TRANSACTION_RECORD_EVENTS = "transactionRecordEvents";

    // Timer相关常量
    public static final String STATS_KEY_APP_CONSUMER_MSGS = "appConsumerMsgs";
    public static final String TIMER_TAG_MSG_TYPE = "msgType";

    /**
     * 消费者处理阶段枚举，对应ProtocolUplinkConsumerService#processMsgs中的不同条件分支
     */
    @Getter
    public enum AppConsumerPhaseEnum {
        LOGIN_REQUEST("loginRequest", "登录请求"),
        HEART_BEAT_REQUEST("heartBeatRequest", "心跳请求"),
        SESSION_CLOSE_EVENT("sessionCloseEvent", "会话关闭事件"),
        VERIFY_PRICING_REQUEST("verifyPricingRequest", "验证定价请求"),
        QUERY_PRICING_REQUEST("queryPricingRequest", "查询定价请求"),
        GUN_RUN_STATUS("gunRunStatus", "充电枪运行状态"),
        CHARGING_PROGRESS("chargingProgress", "充电进度"),
        SET_PRICING_RESPONSE("setPricingResponse", "设置定价响应"),
        REMOTE_START_CHARGING_RESPONSE("remoteStartChargingResponse", "远程启动充电响应"),
        REMOTE_STOP_CHARGING_RESPONSE("remoteStopChargingResponse", "远程停止充电响应"),
        TRANSACTION_RECORD_REQUEST("transactionRecordRequest", "交易记录请求"),
        BMS_CHARGING_ERROR("bmsChargingError", "BMS充电错误"),
        BMS_PARAM_CONFIG_REPORT("bmsParamConfigReport", "BMS参数配置报告"),
        BMS_CHARGING_INFO("bmsChargingInfo", "BMS充电信息"),
        BMS_ABORT("bmsAbort", "BMS中止"),
        RESTART_PILE_RESPONSE("restartPileResponse", "重启充电桩响应"),
        BMS_HANDSHAKE("bmsHandshake", "BMS握手"),
        OTA_RESPONSE("otaResponse", "OTA响应"),
        GROUND_LOCK_STATUS("groundLockStatus", "地锁状态"),
        OFFLINE_CARD_BALANCE_UPDATE_RESPONSE("offlineCardBalanceUpdateResponse", "离线卡余额更新响应"),
        OFFLINE_CARD_SYNC_RESPONSE("offlineCardSyncResponse", "离线卡同步响应"),
        TIME_SYNC_RESPONSE("timeSyncResponse", "时间同步响应"),
        UNKNOWN("unknown", "未知类型");

        private final String timerKey;
        private final String description;

        AppConsumerPhaseEnum(String timerKey, String description) {
            this.timerKey = timerKey;
            this.description = description;
        }

    }

    private final StatsCounter totalCounter;
    private final StatsCounter loginCounter;
    private final StatsCounter heartBeatCounter;
    private final StatsCounter gunRunStatusCounter;
    private final StatsCounter chargingProgressCounter;
    private final StatsCounter transactionRecordCounter;

    private final Timer appConsumerTimer;

    private final Map<String, Timer> appConsumerTimerMap = new HashMap<>();
    
    // Timer统计配置参数
    private final int timerTopN;

    private final List<StatsCounter> counters = new ArrayList<>();

    public AppConsumerStats(StatsFactory statsFactory, int timerTopN) {
        this.timerTopN = timerTopN;
        this.totalCounter = register(statsFactory.createStatsCounter(STATS_KEY_APP_CONSUMER, TOTAL_MSGS));
        this.loginCounter = register(statsFactory.createStatsCounter(STATS_KEY_APP_CONSUMER, LOGIN_EVENTS));
        this.heartBeatCounter = register(statsFactory.createStatsCounter(STATS_KEY_APP_CONSUMER, HEARTBEAT_EVENTS));
        this.gunRunStatusCounter = register(statsFactory.createStatsCounter(STATS_KEY_APP_CONSUMER, GUN_RUN_STATUS_EVENTS));
        this.chargingProgressCounter = register(statsFactory.createStatsCounter(STATS_KEY_APP_CONSUMER, CHARGING_PROGRESS_EVENTS));
        this.transactionRecordCounter = register(statsFactory.createStatsCounter(STATS_KEY_APP_CONSUMER, TRANSACTION_RECORD_EVENTS));

        // 初始化通用消费计时器
        this.appConsumerTimer = statsFactory.createTimer(STATS_KEY_APP_CONSUMER);

        // 初始化各消息类型的计时器映射（排除CONSUME类型）
        for (AppConsumerPhaseEnum phase : AppConsumerPhaseEnum.values()) {
            this.appConsumerTimerMap.put(phase.getTimerKey(), statsFactory.createTimer(STATS_KEY_APP_CONSUMER_MSGS, TIMER_TAG_MSG_TYPE, phase.getTimerKey()));
        }
    }

    private StatsCounter register(StatsCounter counter) {
        counters.add(counter);
        return counter;
    }

    public void log(UplinkQueueMessage msg) {
        totalCounter.increment();
        if (msg.hasLoginRequest()) {
            loginCounter.increment();
        } else if (msg.hasHeartBeatRequest()) {
            heartBeatCounter.increment();
        } else if (msg.hasGunRunStatusProto()) {
            gunRunStatusCounter.increment();
        } else if (msg.hasChargingProgressProto()) {
            chargingProgressCounter.increment();
        } else if (msg.hasTransactionRecordRequest()) {
            transactionRecordCounter.increment();
        }

        appConsumerTimer.record(Duration.ofMillis(System.currentTimeMillis() - TracerContextUtil.getCurrentTracer().getTracerTs()));
    }

    /**
     * 根据消息类型记录处理耗时
     *
     * @param msg 上行队列消息
     */
    public void msgTimer(UplinkQueueMessage msg) {
        AppConsumerPhaseEnum phase = getPhaseByMessage(msg);

        Timer timer = appConsumerTimerMap.get(phase.getTimerKey());

        if (timer != null) {
            timer.record(Duration.ofMillis(System.currentTimeMillis() - TracerContextUtil.getCurrentTracer().getTracerTs()));
        }
    }

    /**
     * 根据消息内容判断消息类型阶段
     *
     * @param msg 上行队列消息
     * @return 对应的消息处理阶段枚举
     */
    private AppConsumerPhaseEnum getPhaseByMessage(UplinkQueueMessage msg) {
        if (msg.hasLoginRequest()) {
            return AppConsumerPhaseEnum.LOGIN_REQUEST;
        } else if (msg.hasHeartBeatRequest()) {
            return AppConsumerPhaseEnum.HEART_BEAT_REQUEST;
        } else if (msg.hasSessionCloseEventProto()) {
            return AppConsumerPhaseEnum.SESSION_CLOSE_EVENT;
        } else if (msg.hasVerifyPricingRequest()) {
            return AppConsumerPhaseEnum.VERIFY_PRICING_REQUEST;
        } else if (msg.hasQueryPricingRequest()) {
            return AppConsumerPhaseEnum.QUERY_PRICING_REQUEST;
        } else if (msg.hasGunRunStatusProto()) {
            return AppConsumerPhaseEnum.GUN_RUN_STATUS;
        } else if (msg.hasChargingProgressProto()) {
            return AppConsumerPhaseEnum.CHARGING_PROGRESS;
        } else if (msg.hasSetPricingResponse()) {
            return AppConsumerPhaseEnum.SET_PRICING_RESPONSE;
        } else if (msg.hasRemoteStartChargingResponse()) {
            return AppConsumerPhaseEnum.REMOTE_START_CHARGING_RESPONSE;
        } else if (msg.hasRemoteStopChargingResponse()) {
            return AppConsumerPhaseEnum.REMOTE_STOP_CHARGING_RESPONSE;
        } else if (msg.hasTransactionRecordRequest()) {
            return AppConsumerPhaseEnum.TRANSACTION_RECORD_REQUEST;
        } else if (msg.hasBmsChargingErrorProto()) {
            return AppConsumerPhaseEnum.BMS_CHARGING_ERROR;
        } else if (msg.hasBmsParamConfigReportProto()) {
            return AppConsumerPhaseEnum.BMS_PARAM_CONFIG_REPORT;
        } else if (msg.hasBmsChargingInfoProto()) {
            return AppConsumerPhaseEnum.BMS_CHARGING_INFO;
        } else if (msg.hasBmsAbortProto()) {
            return AppConsumerPhaseEnum.BMS_ABORT;
        } else if (msg.hasRestartPileResponse()) {
            return AppConsumerPhaseEnum.RESTART_PILE_RESPONSE;
        } else if (msg.hasBmsHandshakeProto()) {
            return AppConsumerPhaseEnum.BMS_HANDSHAKE;
        } else if (msg.hasOtaResponse()) {
            return AppConsumerPhaseEnum.OTA_RESPONSE;
        } else if (msg.hasGroundLockStatusProto()) {
            return AppConsumerPhaseEnum.GROUND_LOCK_STATUS;
        } else if (msg.hasOfflineCardBalanceUpdateResponse()) {
            return AppConsumerPhaseEnum.OFFLINE_CARD_BALANCE_UPDATE_RESPONSE;
        } else if (msg.hasOfflineCardSyncResponse()) {
            return AppConsumerPhaseEnum.OFFLINE_CARD_SYNC_RESPONSE;
        } else if (msg.hasTimeSyncResponse()) {
            return AppConsumerPhaseEnum.TIME_SYNC_RESPONSE;
        } else {
            return AppConsumerPhaseEnum.UNKNOWN;
        }
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            log.info("App Queue Consumer Stats: {}", stats);
            
            printTimerStats();
        }
    }

    private void printTimerStats() {
        if (appConsumerTimerMap.isEmpty() || timerTopN <= 0) {
            return;
        }
        
        // 获取耗时前N的消息类型
        List<Map.Entry<String, Timer>> topNTimers = appConsumerTimerMap.entrySet().stream()
                .filter(entry -> entry.getValue().count() > 0)
                .sorted(Map.Entry.<String, Timer>comparingByValue(
                        Comparator.comparingDouble(timer -> timer.mean(TimeUnit.MILLISECONDS))
                ).reversed())
                .limit(timerTopN)
                .toList();
        
        if (!topNTimers.isEmpty()) {
            StringBuilder timerStats = new StringBuilder(String.format("App Consumer Timer Stats (Top %d by avg time): ", timerTopN));
            
            for (int i = 0; i < topNTimers.size(); i++) {
                Map.Entry<String, Timer> entry = topNTimers.get(i);
                Timer timer = entry.getValue();
                
                timerStats.append(String.format("%s[count=%d, avg=%.2fms, max=%.2fms]", 
                    entry.getKey(),
                    timer.count(),
                    timer.mean(TimeUnit.MILLISECONDS),
                    timer.max(TimeUnit.MILLISECONDS)
                ));
                
                if (i < topNTimers.size() - 1) {
                    timerStats.append(", ");
                }
            }
            
            log.info("{}", timerStats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}
