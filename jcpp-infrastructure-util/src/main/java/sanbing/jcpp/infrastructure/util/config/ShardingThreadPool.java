/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.config;

import com.google.common.hash.HashFunction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;
import sanbing.jcpp.infrastructure.util.trace.TracerRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static sanbing.jcpp.infrastructure.util.JCPPHashUtil.forName;
import static sanbing.jcpp.infrastructure.util.JCPPHashUtil.hash;

/**
 * @author 九筒
 */
@Component
@Slf4j
public class ShardingThreadPool {
    @Value("${service.thread-pool.sharding.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    @Value("${service.thread-pool.sharding.parallelism:8}")
    private int parallelism;

    private HashFunction hashFunction;

    private final Map<Integer, ExecutorService> executorServiceMap = new ConcurrentHashMap<>(8);

    @PostConstruct
    public void init() {
        this.hashFunction = forName(hashFunctionName);
    }

    @PreDestroy
    public void destroy() {
        for (ExecutorService executorService : executorServiceMap.values()) {
            executorService.shutdownNow();
            log.info("Sharding Thread [{}] Shutdown completed.", executorService);
        }
    }

    @Scheduled(fixedDelayString = "${service.thread-pool.sharding.stats-print-interval-ms:10000}")
    public void printStats() {
        executorServiceMap.forEach((k, v) -> {

            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) v;

            int size = threadPoolExecutor.getQueue().size();

            if (size > 1) {
                log.info("分区 {} 的线程池中剩余 {} 条待执行任务,当前正在执行的线程数 {}, 已完成任务 {} / {}",
                        k,
                        size,
                        threadPoolExecutor.getActiveCount(),
                        threadPoolExecutor.getCompletedTaskCount(),
                        threadPoolExecutor.getTaskCount());
            }
        });
    }

    /**
     * 提交分片任务
     */
    public void execute(UUID hashKey, TracerRunnable runnable) {
        int partition = hash(hashFunction, hashKey);

        executorServiceMap.computeIfAbsent(Math.abs(partition % parallelism),
                        p -> Executors.newFixedThreadPool(1, JCPPThreadFactory.forName("sharding-threads-" + p)))
                .execute(runnable);
    }
}