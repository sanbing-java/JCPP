/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.service.queue;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.queue.QueueConsumer;
import sanbing.jcpp.infrastructure.queue.QueueMsg;
import sanbing.jcpp.infrastructure.queue.common.QueueConfig;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Slf4j
public class AppQueueConsumerManager<M extends QueueMsg, C extends QueueConfig> {

    protected final String queueName;
    @Getter
    protected C config;
    protected final MsgPackProcessor<M, C> msgPackProcessor;
    protected final BiFunction<C, Integer, QueueConsumer<M>> consumerCreator;
    protected final ExecutorService consumerExecutor;
    protected final ScheduledExecutorService scheduler;
    protected final ExecutorService taskExecutor;

    private final Queue<QueueConsumerManagerTask> tasks = new ConcurrentLinkedQueue<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private volatile Set<TopicPartitionInfo> partitions;
    protected volatile ConsumerWrapper<M> consumerWrapper;
    protected volatile boolean stopped;

    @Builder
    public AppQueueConsumerManager(String queueName, C config,
                                   MsgPackProcessor<M, C> msgPackProcessor,
                                   BiFunction<C, Integer, QueueConsumer<M>> consumerCreator,
                                   ExecutorService consumerExecutor,
                                   ScheduledExecutorService scheduler,
                                   ExecutorService taskExecutor) {
        this.queueName = queueName;
        this.config = config;
        this.msgPackProcessor = msgPackProcessor;
        this.consumerCreator = consumerCreator;
        this.consumerExecutor = consumerExecutor;
        this.scheduler = scheduler;
        this.taskExecutor = taskExecutor;
        if (config != null) {
            init(config);
        }
    }

    public void init(C config) {
        this.config = config;
        if (config.isConsumerPerPartition()) {
            this.consumerWrapper = new ConsumerPerPartitionWrapper();
        } else {
            this.consumerWrapper = new SingleConsumerWrapper();
        }
        log.debug("[{}] Initialized consumer for queue: {}", queueName, config);
    }

    public void update(C config) {
        addTask(QueueConsumerManagerTask.configUpdate(config));
    }

    public void update(Set<TopicPartitionInfo> partitions) {
        addTask(QueueConsumerManagerTask.partitionChange(partitions));
    }

    protected void addTask(QueueConsumerManagerTask todo) {
        if (stopped) {
            return;
        }
        tasks.add(todo);
        log.info("[{}] Added task: {}", queueName, todo);
        tryProcessTasks();
    }

    @SuppressWarnings("unchecked")
    private void tryProcessTasks() {
        taskExecutor.submit(() -> {
            if (lock.tryLock()) {
                try {
                    C newConfig = null;
                    Set<TopicPartitionInfo> newPartitions = null;
                    while (!stopped) {
                        QueueConsumerManagerTask task = tasks.poll();
                        if (task == null) {
                            break;
                        }
                        log.info("[{}] Processing task: {}", queueName, task);

                        if (task.getEvent() == QueueEvent.PARTITION_CHANGE) {
                            newPartitions = task.getPartitions();
                        } else if (task.getEvent() == QueueEvent.CONFIG_UPDATE) {
                            newConfig = (C) task.getConfig();
                        } else {
                            processTask(task);
                        }
                    }
                    if (stopped) {
                        return;
                    }
                    if (newConfig != null) {
                        doUpdate(newConfig);
                    }
                    if (newPartitions != null) {
                        doUpdate(newPartitions);
                    }
                } catch (Exception e) {
                    log.error("[{}] Failed to process tasks", queueName, e);
                } finally {
                    lock.unlock();
                }
            } else {
                log.trace("[{}] Failed to acquire lock", queueName);
                scheduler.schedule(this::tryProcessTasks, 1, TimeUnit.SECONDS);
            }
        });
    }

    protected void processTask(QueueConsumerManagerTask task) {
    }

    private void doUpdate(C newConfig) {
        log.info("[{}] Processing queue update: {}", queueName, newConfig);
        var oldConfig = this.config;
        this.config = newConfig;
        if (log.isTraceEnabled()) {
            log.trace("[{}] Old queue configuration: {}", queueName, oldConfig);
            log.trace("[{}] New queue configuration: {}", queueName, newConfig);
        }

        if (oldConfig == null) {
            init(config);
        } else if (newConfig.isConsumerPerPartition() != oldConfig.isConsumerPerPartition()) {
            consumerWrapper.getConsumers().forEach(QueueConsumerTask::initiateStop);
            consumerWrapper.getConsumers().forEach(QueueConsumerTask::awaitCompletion);

            init(config);
            if (partitions != null) {
                doUpdate(partitions);
            }
        } else {
            log.trace("[{}] Silently applied new config, because consumer-per-partition not changed", queueName);
        }
    }

    private void doUpdate(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        consumerWrapper.updatePartitions(partitions);
    }

    private void launchConsumer(QueueConsumerTask<M> consumerTask) {
        log.info("[{}] Launching consumer", consumerTask.getKey());
        Future<?> consumerLoop = consumerExecutor.submit(() -> {
            JCPPThreadFactory.updateCurrentThreadName(consumerTask.getKey().toString());
            try {
                consumerLoop(consumerTask.getConsumer());
            } catch (Throwable e) {
                log.error("Failure in consumer loop", e);
            }
            log.info("[{}] Consumer stopped", consumerTask.getKey());
        });
        consumerTask.setTask(consumerLoop);
    }

    private void consumerLoop(QueueConsumer<M> consumer) {
        while (!stopped && !consumer.isStopped()) {
            try {
                List<M> msgs = consumer.poll(config.getPollInterval());
                if (msgs.isEmpty()) {
                    continue;
                }
                processMsgs(msgs, consumer, config);
            } catch (Exception e) {
                if (!consumer.isStopped()) {
                    log.warn("Failed to process messages from queue", e);
                    try {
                        Thread.sleep(config.getPollInterval());
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        }
        if (consumer.isStopped()) {
            consumer.unsubscribe();
        }
    }

    protected void processMsgs(List<M> msgs, QueueConsumer<M> consumer, C config) throws Exception {
        msgPackProcessor.process(msgs, consumer, config);
    }

    public void stop() {
        log.debug("[{}] Stopping consumers", queueName);
        consumerWrapper.getConsumers().forEach(QueueConsumerTask::initiateStop);
        stopped = true;
    }

    public void awaitStop() {
        log.debug("[{}] Waiting for consumers to stop", queueName);
        consumerWrapper.getConsumers().forEach(QueueConsumerTask::awaitCompletion);
        log.debug("[{}] Unsubscribed and stopped consumers", queueName);
    }

    private static String partitionsToString(Collection<TopicPartitionInfo> partitions) {
        return partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.joining(", ", "[", "]"));
    }

    public interface MsgPackProcessor<M extends QueueMsg, C extends QueueConfig> {
        void process(List<M> msgs, QueueConsumer<M> consumer, C config) throws Exception;
    }

    public interface ConsumerWrapper<M extends QueueMsg> {

        void updatePartitions(Set<TopicPartitionInfo> partitions);

        Collection<QueueConsumerTask<M>> getConsumers();

    }

    class ConsumerPerPartitionWrapper implements ConsumerWrapper<M> {
        private final Map<TopicPartitionInfo, QueueConsumerTask<M>> consumers = new HashMap<>();

        @Override
        public void updatePartitions(Set<TopicPartitionInfo> partitions) {
            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(consumers.keySet());

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(consumers.keySet());
            removedPartitions.removeAll(partitions);
            log.info("[{}] Added partitions: {}, removed partitions: {}", queueName, partitionsToString(addedPartitions), partitionsToString(removedPartitions));

            removedPartitions.forEach((tpi) -> consumers.get(tpi).initiateStop());
            removedPartitions.forEach((tpi) -> consumers.remove(tpi).awaitCompletion());

            addedPartitions.forEach((tpi) -> {
                Integer partitionId = tpi.getPartition().orElse(-1);
                String key = queueName + "-" + partitionId;
                QueueConsumerTask<M> consumer = new QueueConsumerTask<>(key, () -> consumerCreator.apply(config, partitionId));
                consumers.put(tpi, consumer);
                consumer.subscribe(Set.of(tpi));
                launchConsumer(consumer);
            });
        }

        @Override
        public Collection<QueueConsumerTask<M>> getConsumers() {
            return consumers.values();
        }
    }

    class SingleConsumerWrapper implements ConsumerWrapper<M> {
        private QueueConsumerTask<M> consumer;

        @Override
        public void updatePartitions(Set<TopicPartitionInfo> partitions) {
            log.info("[{}] New partitions: {}", queueName, partitionsToString(partitions));
            if (partitions.isEmpty()) {
                if (consumer != null && consumer.isRunning()) {
                    consumer.initiateStop();
                    consumer.awaitCompletion();
                }
                consumer = null;
                return;
            }

            if (consumer == null) {
                consumer = new QueueConsumerTask<>(queueName, () -> consumerCreator.apply(config, null)); // no partitionId passed
            }
            consumer.subscribe(partitions);
            if (!consumer.isRunning()) {
                launchConsumer(consumer);
            }
        }

        @Override
        public Collection<QueueConsumerTask<M>> getConsumers() {
            if (consumer == null) {
                return Collections.emptyList();
            }
            return List.of(consumer);
        }
    }
}
