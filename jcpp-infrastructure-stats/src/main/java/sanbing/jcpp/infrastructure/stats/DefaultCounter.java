/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultCounter {
    private final AtomicInteger aiCounter;
    private final Counter micrometerCounter;

    public DefaultCounter(AtomicInteger aiCounter, Counter micrometerCounter) {
        this.aiCounter = aiCounter;
        this.micrometerCounter = micrometerCounter;
    }

    public void increment() {
        aiCounter.incrementAndGet();
        micrometerCounter.increment();
    }

    public void clear() {
        aiCounter.set(0);
    }

    public int get() {
        return aiCounter.get();
    }

    public void add(int delta){
        aiCounter.addAndGet(delta);
        micrometerCounter.increment(delta);
    }
}
