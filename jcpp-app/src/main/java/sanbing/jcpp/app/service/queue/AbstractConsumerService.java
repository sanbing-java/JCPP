/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.queue;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.queue.discovery.event.JCPPApplicationEventListener;
import sanbing.jcpp.infrastructure.queue.discovery.event.PartitionChangeEvent;
import sanbing.jcpp.infrastructure.util.annotation.AfterStartUp;
import sanbing.jcpp.infrastructure.util.async.JCPPExecutors;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractConsumerService extends JCPPApplicationEventListener<PartitionChangeEvent> {

    protected ExecutorService consumersExecutor;
    protected ExecutorService mgmtExecutor;
    protected ScheduledExecutorService scheduler;

    public void init(String prefix) {
        this.consumersExecutor = JCPPExecutors.newVirtualThreadPool(prefix + "-consumer-virtual");
        this.mgmtExecutor = JCPPExecutors.newWorkStealingPool(getMgmtThreadPoolSize(), prefix + "-mgmt");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(JCPPThreadFactory.forName(prefix + "-consumer-scheduler"));
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        startConsumers();
    }

    protected void startConsumers() {
    }

    protected void stopConsumers() {
    }

    protected abstract int getMgmtThreadPoolSize();

    @PreDestroy
    public void destroy() {
        stopConsumers();
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (mgmtExecutor != null) {
            mgmtExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

}
