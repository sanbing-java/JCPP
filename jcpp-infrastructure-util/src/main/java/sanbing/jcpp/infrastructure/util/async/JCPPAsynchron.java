/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.async;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class JCPPAsynchron {

    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure) {
        withCallback(future, onSuccess, onFailure, null);
    }

    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure, Executor executor) {
        FutureCallback<T> callback = new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                try {
                    onSuccess.accept(result);
                } catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onFailure.accept(t);
            }
        };
        Futures.addCallback(future, callback, Objects.requireNonNullElseGet(executor, MoreExecutors::directExecutor));
    }

    public static <T> ListenableFuture<T> submit(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure, Executor executor) {
        return submit(task, onSuccess, onFailure, executor, null);
    }

    public static <T> ListenableFuture<T> submit(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure, Executor executor, Executor callbackExecutor) {
        ListenableFuture<T> future = Futures.submit(task, executor);
        withCallback(future, onSuccess, onFailure, callbackExecutor);
        return future;
    }

}
