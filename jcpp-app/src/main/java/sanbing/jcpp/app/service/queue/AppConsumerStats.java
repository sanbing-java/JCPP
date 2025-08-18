/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.queue;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.stats.StatsCounter;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AppConsumerStats {
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String LOGIN_EVENTS = "loginEvents";
    public static final String HEARTBEAT_EVENTS = "heartBeatEvents";
    public static final String GUN_RUN_STATUS_EVENTS = "gunRunStatusEvents";
    public static final String CHARGING_PROGRESS_EVENTS = "chargingProgressEvents";
    public static final String TRANSACTION_RECORD_EVENTS = "transactionRecordEvents";

    private final StatsCounter totalCounter;
    private final StatsCounter loginCounter;
    private final StatsCounter heartBeatCounter;
    private final StatsCounter gunRunStatusCounter;
    private final StatsCounter chargingProgressCounter;
    private final StatsCounter transactionRecordCounter;
    private final Timer appConsumerTimer;

    private final List<StatsCounter> counters = new ArrayList<>();

    public AppConsumerStats(StatsFactory statsFactory) {
        String statsKey = "appConsumer";

        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.loginCounter = register(statsFactory.createStatsCounter(statsKey, LOGIN_EVENTS));
        this.heartBeatCounter = register(statsFactory.createStatsCounter(statsKey, HEARTBEAT_EVENTS));
        this.gunRunStatusCounter = register(statsFactory.createStatsCounter(statsKey, GUN_RUN_STATUS_EVENTS));
        this.chargingProgressCounter = register(statsFactory.createStatsCounter(statsKey, CHARGING_PROGRESS_EVENTS));
        this.transactionRecordCounter = register(statsFactory.createStatsCounter(statsKey, TRANSACTION_RECORD_EVENTS));
        this.appConsumerTimer = statsFactory.createTimer(statsKey);
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

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            log.info("App Queue Consumer Stats: {}", stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}
