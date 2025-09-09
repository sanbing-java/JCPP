/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.stats;

import io.micrometer.core.instrument.Timer;

public interface StatsFactory {

    /**
     * 创建状态计数器，默认带一个statsName的Tag，并可以自定义扩展其他Tag
     *
     * @param key 指标名
     * @param statsName statsName的标签值
     * @param otherTags 其他Tag键值对，参数个数需要是偶数
     */
    StatsCounter createStatsCounter(String key, String statsName, String... otherTags);

    /**
     * 创建计数器，可自定义Tag
     *
     * @param key 指标名
     * @param tags 自定义Tag键值对，参数个数需要是偶数
     */
    DefaultCounter createDefaultCounter(String key, String... tags);

    /**
     * 创建消息计数器，消息计数器默认包含三种状态（总数、成功数、失败数）
     *
     * @param key 指标名
     * @param tags 自定义Tag键值对，参数个数需要是偶数
     */
    MessagesStats createMessagesStats(String key, String... tags);

    /**
     * 创建计时器
     *
     * @param key 指标名
     * @param tags 自定义Tag键值对，参数个数需要是偶数
     */
    Timer createTimer(String key, String... tags);

    /**
     * 创建计量器，用于记录某个值的当前状态，可以是瞬时数值
     *
     * @param key 指标名
     * @param number 初始值
     * @param tags 自定义Tag键值对，参数个数需要是偶数
     */
    <T extends Number> T createGauge(String key, T number, String... tags);

}
