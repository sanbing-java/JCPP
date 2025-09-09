/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.batch;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.stats.MessagesStats;
import sanbing.jcpp.infrastructure.util.CollectionsUtil;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class SqlBlockingQueue<E, R> implements SqlQueue<E, R> {

    private final BlockingQueue<SqlQueueElement<E, R>> queue = new LinkedBlockingQueue<>();
    private final SqlBlockingQueueParams params;

    private ExecutorService executor;
    private final MessagesStats stats;

    public SqlBlockingQueue(SqlBlockingQueueParams params, MessagesStats stats) {
        this.params = params;
        this.stats = stats;
    }

    @Override
    public void init(ScheduledLogExecutorComponent logExecutor, Function<List<E>, List<R>> saveFunction, Comparator<E> batchUpdateComparator, Function<List<SqlQueueElement<E, R>>, List<SqlQueueElement<E, R>>> filter, int index) {
        executor = Executors.newSingleThreadExecutor(JCPPThreadFactory.forName("sql-queue-" + index + "-" + params.getLogName().toLowerCase()));
        executor.submit(() -> {
            String logName = params.getLogName();
            int batchSize = params.getBatchSize();
            long maxDelay = params.getMaxDelay();
            final List<SqlQueueElement<E, R>> entities = new ArrayList<>(batchSize);
            while (!Thread.interrupted()) {
                try {
                    long currentTs = System.currentTimeMillis();
                    SqlQueueElement<E, R> attr = queue.poll(maxDelay, TimeUnit.MILLISECONDS);
                    if (attr == null) {
                        continue;
                    } else {
                        entities.add(attr);
                    }
                    queue.drainTo(entities, batchSize - 1);
                    boolean fullPack = entities.size() == batchSize;
                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Going to save {} entities", logName, entities.size());
                        log.trace("[{}] Going to save entities: {}", logName, entities);
                    }

                    List<SqlQueueElement<E, R>> entitiesToSave = filter.apply(entities);

                    if (params.isBatchSortEnabled()) {
                        entitiesToSave = entitiesToSave.stream().sorted((o1, o2) -> batchUpdateComparator.compare(o1.entity(), o2.entity())).toList();
                    }

                    List<R> result = saveFunction.apply(entitiesToSave.stream().map(SqlQueueElement::entity).collect(Collectors.toList()));

                    if (params.isWithResponse()) {
                        for (int i = 0; i < entitiesToSave.size(); i++) {
                            entitiesToSave.get(i).future().set(result.get(i));
                        }

                        if (entities.size() > entitiesToSave.size()) {
                            CollectionsUtil.diffLists(entitiesToSave, entities).forEach(v -> v.future().set(null));
                        }
                    } else {
                        entities.forEach(v -> v.future().set(null));
                    }

                    stats.incrementSuccessful(entities.size());
                    if (!fullPack) {
                        long remainingDelay = maxDelay - (System.currentTimeMillis() - currentTs);
                        if (remainingDelay > 0) {
                            Thread.sleep(remainingDelay);
                        }
                    }
                } catch (Throwable t) {
                    if (t instanceof InterruptedException) {
                        log.info("[{}] Queue polling was interrupted", logName);
                        break;
                    } else {
                        log.error("[{}] Failed to save {} entities", logName, entities.size(), t);
                        try {
                            stats.incrementFailed(entities.size());
                            entities.forEach(entityFutureWrapper -> entityFutureWrapper.future().setException(t));
                        } catch (Throwable th) {
                            log.error("[{}] Failed to set future exception", logName, th);
                        }
                    }
                } finally {
                    entities.clear();
                }
            }
            log.info("[{}] Queue polling completed", logName);
        });

        logExecutor.scheduleAtFixedRate(() -> {
            if (!queue.isEmpty() || stats.getTotal() > 0 || stats.getSuccessful() > 0 || stats.getFailed() > 0) {
                log.info("Queue-{} [{}] queueSize [{}] totalAdded [{}] totalSaved [{}] totalFailed [{}]", index,
                        params.getLogName(), queue.size(), stats.getTotal(), stats.getSuccessful(), stats.getFailed());
                stats.reset();
            }
        }, params.getStatsPrintIntervalMs(), params.getStatsPrintIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<R> add(E element) {
        SettableFuture<R> future = SettableFuture.create();
        queue.add(new SqlQueueElement<>(future, element));
        stats.incrementTotal();
        return future;
    }
}
