/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.service.queue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.queue.QueueConsumer;
import sanbing.jcpp.infrastructure.queue.QueueMsg;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class QueueConsumerTask<M extends QueueMsg> {

    @Getter
    private final Object key;
    private volatile QueueConsumer<M> consumer;
    private volatile Supplier<QueueConsumer<M>> consumerSupplier;

    @Setter
    private Future<?> task;

    public QueueConsumerTask(Object key, Supplier<QueueConsumer<M>> consumerSupplier) {
        this.key = key;
        this.consumer = null;
        this.consumerSupplier = consumerSupplier;
    }

    public QueueConsumer<M> getConsumer() {
        if (consumer == null) {
            synchronized (this) {
                if (consumer == null) {
                    Objects.requireNonNull(consumerSupplier, "consumerSupplier for key [" + key + "] is null");
                    consumer = consumerSupplier.get();
                    Objects.requireNonNull(consumer, "consumer for key [" + key + "] is null");
                    consumerSupplier = null;
                }
            }
        }
        return consumer;
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        log.info("[{}] Subscribing to partitions: {}", key, partitions);
        getConsumer().subscribe(partitions);
    }

    public void initiateStop() {
        log.debug("[{}] Initiating stop", key);
        getConsumer().stop();
    }

    public void awaitCompletion() {
        log.trace("[{}] Awaiting finish", key);
        if (isRunning()) {
            try {
                task.get(30, TimeUnit.SECONDS);
                log.trace("[{}] Awaited finish", key);
            } catch (Exception e) {
                log.warn("[{}] Failed to await for consumer to stop", key, e);
            }
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

}
