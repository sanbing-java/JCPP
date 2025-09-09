/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.common;

import static sanbing.jcpp.infrastructure.util.trace.TracerContextUtil.*;

/**
 * @author 九筒
 */
public final class QueueConstants {

    public static final String MSG_MD_PREFIX = "jcpp_";

    public static final String MSG_MD_TRACER_ID = MSG_MD_PREFIX + JCPP_TRACER_ID;

    public static final String MSG_MD_TRACER_ORIGIN = MSG_MD_PREFIX + JCPP_TRACER_ORIGIN;

    public static final String MSG_MD_TRACER_TS = MSG_MD_PREFIX + JCPP_TRACER_TS;

}