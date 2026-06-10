/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.mdc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import sanbing.jcpp.infrastructure.util.trace.Tracer;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;

public class MDCUtils {

    private static final String TRACE_ID = "TRACE_ID";

    public static String putIfAbsentTracer() {
        String traceId = MDC.get(TRACE_ID);

        if (StringUtils.isEmpty(traceId)) {
            return recordTracer();
        }

        return traceId;
    }

    public static String recordTracer() {
        Tracer tracer = TracerContextUtil.getCurrentTracer();

        if (!StringUtils.isEmpty(tracer.getTraceId())) {
            MDC.put(TRACE_ID, tracer.getTraceId());
        } else {
            MDC.remove(TRACE_ID);
        }

        return tracer.getTraceId();
    }

    public static void cleanTracer() {
        MDC.remove(TRACE_ID);
    }
}
