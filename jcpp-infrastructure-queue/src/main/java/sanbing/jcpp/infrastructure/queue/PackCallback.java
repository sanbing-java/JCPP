/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class PackCallback<T> implements Callback {
    private final PackProcessingContext<T> ctx;
    private final UUID id;

    public PackCallback(UUID id, PackProcessingContext<T> ctx) {
        this.id = id;
        this.ctx = ctx;
    }

    @Override
    public void onSuccess() {
        log.trace("[{}] ON SUCCESS", id);
        ctx.onSuccess(id);
    }

    @Override
    public void onFailure(Throwable t) {
        log.trace("[{}] ON FAILURE", id, t);
        ctx.onFailure(id, t);
    }
}
