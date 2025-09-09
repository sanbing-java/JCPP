/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 13位时间戳反序列化器
 * @author 九筒
 */
public class LongTimestampDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        // 判定是否是long类型
        if ("LONG".equals(jsonParser.getNumberType().name())) {
            return jsonParser.getLongValue();
        }
        LocalDateTime localDateTime = LocalDateTime.parse(jsonParser.getValueAsString().replace(" ", "T").replace("Z", ""));
        ZoneId systemDefaultZone = ZoneId.systemDefault();
        return localDateTime.atZone(systemDefaultZone).toInstant().toEpochMilli();
    }

}
