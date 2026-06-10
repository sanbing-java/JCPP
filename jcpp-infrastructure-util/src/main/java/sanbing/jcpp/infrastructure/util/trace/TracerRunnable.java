/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.trace;

import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;

public class TracerRunnable implements Runnable {

    private Tracer tracer;
    private final Runnable runnable;

    public TracerRunnable(Runnable runnable) {
        this.tracer = TracerContextUtil.getCurrentTracer();
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            if (this.tracer != null) {
                TracerContextUtil.newTracer(tracer.getTraceId(), tracer.getOrigin(), tracer.getTracerTs());

                MDCUtils.recordTracer();
            }

            this.runnable.run();
        } finally {
            TracerContextUtil.cleanTracer();

            MDCUtils.cleanTracer();

            this.tracer = null;
        }
    }
}
