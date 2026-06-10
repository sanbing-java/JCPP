/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalDateTime类型反序列化
 * 需要用到的字段上加 @JsonDeserialize(using = EnergyLocalTimeDeserializer.class)
 */
public class LocalTimeDeserializer extends StdDeserializer<LocalTime> {
    public static final LocalTimeDeserializer INSTANCE = new LocalTimeDeserializer();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_MS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private LocalTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalTime deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException {

        String dateString = jsonParser.getText();
        return dateString.length() > 8
                ? LocalTime.parse(dateString, DATE_TIME_FORMATTER_MS)
                : LocalTime.parse(dateString, DATE_TIME_FORMATTER);

    }

}
