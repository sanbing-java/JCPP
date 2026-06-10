/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.time.LocalDateTime;

public class CP56Time2aUtil {

    // 常量定义，用于字段完整性校验
    private static final int MAX_MILLIS = 59999; // 毫秒最大值 (60 * 1000 - 1)
    private static final int MAX_MINUTE = 59; // 分钟最大值
    private static final int MAX_HOUR = 23; // 小时最大值
    private static final int MIN_DAY = 1; // 天最小值
    private static final int MAX_MONTH = 12; // 月份最大值
    private static final int MAX_YEAR = 99; // 年份最大值（两位数年份）

    /**
     * 编码 LocalDateTime 为 CP56Time2a 格式（使用 Netty 的 ByteBuf，小端字节序）
     *
     * @param dateTime 要编码的 LocalDateTime 对象
     * @return 编码后的字节数组
     */
    public static byte[] encode(LocalDateTime dateTime) {
        ByteBuf buffer = Unpooled.buffer(7); // 创建一个 7 字节的缓冲区

        // 编码毫秒部分（16 位无符号整数，存储为小端字节序）
        int millis = (dateTime.getSecond() * 1000) + (dateTime.getNano() / 1_000_000);
        buffer.writeByte(millis & 0xFF);       // 写入低字节
        buffer.writeByte((millis >> 8) & 0xFF); // 写入高字节

        // 编码分钟（6 位有效 + 1 位有效性位 IV，假设 IV 为 0 表示时间有效）
        byte minute = (byte) (dateTime.getMinute() & 0x3F); // 仅保留 6 位
        buffer.writeByte(minute);

        // 编码小时（5 位有效）
        byte hour = (byte) (dateTime.getHour() & 0x1F); // 仅保留 5 位
        buffer.writeByte(hour);

        // 编码天（5 位有效）
        byte day = (byte) (dateTime.getDayOfMonth() & 0x1F); // 仅保留 5 位
        buffer.writeByte(day);

        // 编码月份（4 位有效）
        byte month = (byte) (dateTime.getMonthValue() & 0x0F); // 仅保留 4 位
        buffer.writeByte(month);

        // 编码年份（7 位有效，两位数年份）
        byte year = (byte) (dateTime.getYear() % 100); // 取年份后两位
        buffer.writeByte(year);

        // 返回字节数组
        byte[] result = new byte[7];
        buffer.readBytes(result);
        return result;
    }

    /**
     * 解码 CP56Time2a 格式字节数组为 LocalDateTime 对象（使用 Netty 的 ByteBuf，小端字节序）
     *
     * @param data 字节数组，长度必须为 7
     * @return 解码后的 LocalDateTime 对象
     * @throws IllegalArgumentException 如果字段值超出范围或数组长度不合法
     */
    public static LocalDateTime decode(byte[] data) {
        // 校验输入字节数组长度
        if (data == null || data.length != 7) {
            throw new IllegalArgumentException("CP56Time2a 数据长度必须为 7 字节");
        }

        ByteBuf buffer = Unpooled.wrappedBuffer(data); // 创建 ByteBuf 包装输入数据

        // 解码毫秒部分（16 位无符号整数，小端字节序）
        int millis = (buffer.readUnsignedByte()) | (buffer.readUnsignedByte() << 8);
        if (millis > MAX_MILLIS) {
            throw new IllegalArgumentException("毫秒值超出范围: " + millis);
        }
        int seconds = millis / 1000; // 秒
        int nanos = (millis % 1000) * 1_000_000; // 纳秒

        // 解码分钟（校验 6 位有效值）
        int minute = buffer.readUnsignedByte() & 0x3F;
        if (minute > MAX_MINUTE) {
            throw new IllegalArgumentException("分钟值超出范围: " + minute);
        }

        // 解码小时（校验 5 位有效值）
        int hour = buffer.readUnsignedByte() & 0x1F;
        if (hour > MAX_HOUR) {
            throw new IllegalArgumentException("小时值超出范围: " + hour);
        }

        // 解码天（校验 5 位有效值）
        int day = buffer.readUnsignedByte() & 0x1F;
        if (day < MIN_DAY) {
            throw new IllegalArgumentException("天值超出范围: " + day);
        }

        // 解码月份（校验 4 位有效值）
        int month = buffer.readUnsignedByte() & 0x0F;
        if (month < 1 || month > MAX_MONTH) {
            throw new IllegalArgumentException("月份值超出范围: " + month);
        }

        // 解码年份（校验 7 位有效值）
        int year = 2000 + (buffer.readUnsignedByte() & 0x7F); // 假设年份基于 2000 年
        if ((year - 2000) > MAX_YEAR) {
            throw new IllegalArgumentException("年份值超出范围: " + year);
        }

        // 构造 LocalDateTime 对象
        return LocalDateTime.of(year, month, day, hour, minute, seconds, nanos);
    }
}