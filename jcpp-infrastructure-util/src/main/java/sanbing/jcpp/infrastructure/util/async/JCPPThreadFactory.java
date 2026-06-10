/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;

public class JCPPThreadFactory {
    public static final String THREAD_TOPIC_SEPARATOR = " | ";

    public static ThreadFactory forName(String name) {
        return new ThreadFactoryBuilder()
                .setNameFormat(name)
                .setDaemon(true)
                .setPriority(Thread.NORM_PRIORITY)
                .build();
    }

    public static ThreadFactory forName(String name, int priority) {
        return new ThreadFactoryBuilder()
                .setNameFormat(name)
                .setDaemon(true)
                .setPriority(priority)
                .build();
    }
    public static void updateCurrentThreadName(String threadSuffix) {
        String name = Thread.currentThread().getName();
        int spliteratorIndex = name.indexOf(THREAD_TOPIC_SEPARATOR);
        if (spliteratorIndex > 0) {
            name = name.substring(0, spliteratorIndex);
        }
        name = name + THREAD_TOPIC_SEPARATOR + threadSuffix;
        Thread.currentThread().setName(name);
    }

    public static void addThreadNamePrefix(String prefix) {
        String name = Thread.currentThread().getName();
        name = prefix + "-" + name;
        Thread.currentThread().setName(name);
    }

}