/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.async;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public interface ListeningExecutor extends Executor {

    <T> ListenableFuture<T> executeAsync(Callable<T> task);

    default ListenableFuture<?> executeAsync(Runnable task) {
        return executeAsync(() -> {
            task.run();
            return null;
        });
    }

    default <T> ListenableFuture<T> submit(Callable<T> task) {
        return executeAsync(task);
    }

    default ListenableFuture<?> submit(Runnable task) {
        return executeAsync(task);
    }

}
