/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

public class StatsCounter extends DefaultCounter {
    private final String name;

    public StatsCounter(AtomicInteger aiCounter, Counter micrometerCounter, String name) {
        super(aiCounter, micrometerCounter);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
