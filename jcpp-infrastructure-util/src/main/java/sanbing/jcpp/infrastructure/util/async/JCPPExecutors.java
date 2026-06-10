/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class JCPPExecutors {

    public static ExecutorService newWorkStealingPool(int parallelism, String namePrefix) {
        return new ForkJoinPool(parallelism,
                new JCPPForkJoinWorkerThreadFactory(namePrefix),
                null, true);
    }

    public static ExecutorService newWorkStealingPool(int parallelism, Class<?> clazz) {
        return newWorkStealingPool(parallelism, clazz.getSimpleName());
    }

    public static ExecutorService newVirtualThreadPool(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(new JCPPVirtualThreadFactory(namePrefix));
    }

}
