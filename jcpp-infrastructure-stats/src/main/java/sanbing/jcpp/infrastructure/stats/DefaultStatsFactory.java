/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.stats;

import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultStatsFactory implements StatsFactory {
    private static final String TOTAL_MSGS = "totalMsgs";
    private static final String SUCCESSFUL_MSGS = "successfulMsgs";
    private static final String FAILED_MSGS = "failedMsgs";

    private static final String STATS_NAME_TAG = "statsName";

    private static final Counter STUB_COUNTER = new StubCounter();

    @Resource
    private MeterRegistry meterRegistry;

    @Value("${metrics.enabled:true}")
    private Boolean metricsEnabled;

    @Value("${metrics.timer.percentiles:0.5}")
    private String timerPercentilesStr;

    private double[] timerPercentiles;

    @PostConstruct
    public void init() {
        if (StringUtils.isNotEmpty(timerPercentilesStr)) {
            String[] split = timerPercentilesStr.split(",");
            timerPercentiles = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                timerPercentiles[i] = Double.parseDouble(split[i]);
            }
        }
    }

    @Override
    public StatsCounter createStatsCounter(String key, String statsName, String... otherTags) {
        String[] tags = getTags(statsName, otherTags);
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ? meterRegistry.counter(key, tags) : STUB_COUNTER,
                statsName
        );
    }

    @Override
    public DefaultCounter createDefaultCounter(String key, String... tags) {
        return new DefaultCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, tags)
                        : STUB_COUNTER
        );
    }

    @Override
    public MessagesStats createMessagesStats(String key, String... tags) {
        StatsCounter totalCounter = createStatsCounter(key, TOTAL_MSGS, tags);
        StatsCounter successfulCounter = createStatsCounter(key, SUCCESSFUL_MSGS, tags);
        StatsCounter failedCounter = createStatsCounter(key, FAILED_MSGS, tags);
        return new DefaultMessagesStats(totalCounter, successfulCounter, failedCounter);
    }

    @Override
    public Timer createTimer(String key, String... tags) {
        Timer.Builder timerBuilder = Timer.builder(key)
                .tags(tags)
                .publishPercentiles();
        if (timerPercentiles != null && timerPercentiles.length > 0) {
            timerBuilder.publishPercentiles(timerPercentiles);
        }
        return timerBuilder.register(meterRegistry);
    }

    @Override
    public <T extends Number> T createGauge(String key, T number, String... tags) {
        return meterRegistry.gauge(key, Tags.of(tags), number);
    }

    private static String[] getTags(String statsName, String[] otherTags) {
        String[] tags = new String[]{STATS_NAME_TAG, statsName};
        if (otherTags.length > 0) {
            if (otherTags.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid tags array size");
            }
            tags = ArrayUtils.addAll(tags, otherTags);
        }
        return tags;
    }

    private static class StubCounter implements Counter {
        @Override
        public void increment(double amount) {
        }

        @Override
        public double count() {
            return 0;
        }

        @Override
        public Id getId() {
            return null;
        }
    }
}
