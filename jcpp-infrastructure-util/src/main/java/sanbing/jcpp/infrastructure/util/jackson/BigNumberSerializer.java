/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;

import java.io.IOException;

@JacksonStdImpl
public class BigNumberSerializer extends NumberSerializer {

    private static final long JS_NUM_MAX = 9007199254740992L;
    private static final long JS_NUM_MIN = -9007199254740992L;
    public static final BigNumberSerializer instance = new BigNumberSerializer(Number.class);

    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        long longValue = value.longValue();
        if (longValue >= JS_NUM_MIN && longValue <= JS_NUM_MAX) {
            super.serialize(value, gen, provider);
        } else {
            gen.writeString(value.toString());
        }
    }

}
