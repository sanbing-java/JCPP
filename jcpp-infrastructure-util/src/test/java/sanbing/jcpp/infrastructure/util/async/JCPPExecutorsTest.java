/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.async;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerRunnable;

import java.util.concurrent.ExecutorService;

class JCPPExecutorsTest {

    @Test
    void newVirtualThreadPool() {
        ExecutorService executorService = JCPPExecutors.newVirtualThreadPool("test-consumer-virtual");

        TracerContextUtil.newTracer();
        MDCUtils.recordTracer();

        System.out.println(MDC.get("TRACE_ID"));

        executorService.submit(new TracerRunnable(() -> {
            System.out.println(MDC.get("TRACE_ID"));
        }));
    }
}