/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.trace;

import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;

import java.util.concurrent.Callable;

public class TracerCallable<T> implements Callable<T> {

    private Tracer tracer;
    private final Callable<T> callable;

    public TracerCallable(Callable<T> callable) {
        this.tracer = TracerContextUtil.getCurrentTracer();
        this.callable = callable;
    }

    @Override
    public T call() throws Exception {
        try {
            if (this.tracer != null) {
                TracerContextUtil.newTracer(tracer.getTraceId(), tracer.getOrigin(), tracer.getTracerTs());

                MDCUtils.recordTracer();
            }

            return this.callable.call();
        } finally {
            TracerContextUtil.cleanTracer();

            MDCUtils.cleanTracer();

            this.tracer = null;
        }
    }

}
