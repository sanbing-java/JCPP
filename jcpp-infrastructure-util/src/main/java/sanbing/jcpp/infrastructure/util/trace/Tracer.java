/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.trace;

import lombok.Data;

import java.io.Serializable;

@Data
public class Tracer implements Serializable {

    private String traceId;

    private String origin;

    private final long tracerTs;

    public Tracer(String traceId, String origin) {
        this.traceId = traceId;
        this.origin = origin;
        this.tracerTs = System.currentTimeMillis();
    }

    public Tracer(String traceId, String origin, long tracerTs) {
        this.traceId = traceId;
        this.origin = origin;
        this.tracerTs = tracerTs;
    }

    public Tracer(String traceId, long tracerTs) {
        this.traceId = traceId;
        this.origin = "JCPP";
        this.tracerTs = tracerTs;

    }
}
