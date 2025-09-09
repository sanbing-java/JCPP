/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


/**
 * 类型转换
 *
 * @author 九筒
 */
public class DataTypeModule extends SimpleModule {
    public static final DataTypeModule INSTANCE = new DataTypeModule();

    private DataTypeModule() {
        super(DataTypeModule.class.getName());

        // number
        this.addSerializer(Long.class, BigNumberSerializer.instance);
        this.addSerializer(Long.TYPE, BigNumberSerializer.instance);
        this.addSerializer(BigInteger.class, BigNumberSerializer.instance);
        this.addSerializer(BigDecimal.class, BigNumberSerializer.instance);

        // time
        this.addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE);
        this.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        this.addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE);
        this.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        this.addSerializer(Date.class, DateSerializer.INSTANCE);
        this.addSerializer(java.sql.Date.class, SqlDateSerializer.INSTANCE);
        this.addSerializer(Timestamp.class, TimestampSerializer.INSTANCE);

        this.addDeserializer(LocalTime.class, LocalTimeDeserializer.INSTANCE);
        this.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        this.addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE);
        this.addDeserializer(Instant.class, InstantDeserializer.INSTANCE);
        this.addDeserializer(Date.class, DateDeserializer.INSTANCE);
        this.addDeserializer(java.sql.Date.class, SqlDateDeserializer.INSTANCE);
        this.addDeserializer(Timestamp.class, TimestampDeserializer.INSTANCE);
    }
}
