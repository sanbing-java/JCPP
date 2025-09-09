/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.jackson;

import java.time.format.DateTimeFormatter;

/**
 * Instant 序列化
 *
 * @author 九筒
 */
public class InstantSerializer extends com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer {
    public static final InstantSerializer INSTANCE = new InstantSerializer();

    private InstantSerializer() {
        super(com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer.INSTANCE, true,false, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

}
