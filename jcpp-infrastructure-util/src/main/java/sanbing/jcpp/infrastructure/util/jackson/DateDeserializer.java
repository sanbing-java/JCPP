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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间反序列化
 *
 * @author 九筒
 */
public class DateDeserializer extends JsonDeserializer<Date> {
    public static final DateDeserializer INSTANCE = new DateDeserializer();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private DateDeserializer() {
    }

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String dateString = p.getText();

        return dateString.length() > 19
                ? Date.from(LocalDateTime.parse(dateString, DATE_TIME_FORMATTER_MS).atZone(ZoneOffset.systemDefault()).toInstant())
                : Date.from(LocalDateTime.parse(dateString, DATE_TIME_FORMATTER).atZone(ZoneOffset.systemDefault()).toInstant());
    }
}