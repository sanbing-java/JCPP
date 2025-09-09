/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.codec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BCDUtil {
    private static final String HEX = "0123456789ABCDEF";

    /**
     * 十进制 转 BCD字节数组
     *
     * @param num long （8字节）
     * @return byte[]
     */
    public static byte[] longToBcdBytes(long num) {
        int digits = 0;
        long temp = num;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }
        int byteLen = digits % 2 == 0 ? digits / 2 : (digits + 1) / 2;
        byte[] bcd = new byte[byteLen];
        for (int i = 0; i < digits; i++) {
            byte tmp = (byte) (num % 10);
            if (i % 2 == 0) {
                bcd[i / 2] = tmp;
            } else {
                bcd[i / 2] |= (byte) (tmp << 4);
            }
            num /= 10;
        }
        for (int i = 0; i < byteLen / 2; i++) {
            byte tmp = bcd[i];
            bcd[i] = bcd[byteLen - i - 1];
            bcd[byteLen - i - 1] = tmp;
        }
        return bcd;
    }

    /**
     * BCD字节数组 转 十进制
     *
     * @param bcd byte[]
     * @return long
     */
    public static long bcdBytesToLong(byte[] bcd) {
        return Long.parseLong(BCDUtil.toString(bcd));
    }

    /**
     * bcd字节数组 转 数字字符串
     *
     * @param bcd byte[]
     * @return String
     */
    public static String toString(byte[] bcd) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bcd) {
            sb.append(toString(b));
        }
        return sb.toString();
    }

    /**
     * 单个字节BCD 转 数字字符串
     *
     * @param bcd byte
     * @return String
     */
    public static String toString(byte bcd) {
        StringBuilder sb = new StringBuilder();
        byte high = (byte) (bcd & 0xf0);
        high >>>= (byte) 4;
        high = (byte) (high & 0x0f);
        byte low = (byte) (bcd & 0x0f);

        sb.append(high);
        sb.append(low);
        return sb.toString();
    }

    /**
     * 数字字符串 转 BCD字节数组
     *
     * @param str 数字字符串
     * @return BCD字节数组
     */
    public static byte[] numStrToBcdBytes(String str) {
        //若为奇数，补0为偶
        if ((str.length() & 0x1) == 1) {
            str = "0" + str;
        }
        byte[] ret = new byte[str.length() / 2];
        byte[] bs = str.getBytes();
        for (int i = 0; i < ret.length; i++) {
            byte high = ascII2Bcd(bs[2 * i]);
            byte low = ascII2Bcd(bs[2 * i + 1]);
            ret[i] = (byte) ((high << 4) | low);
        }
        return ret;
    }

    public static byte ascII2Bcd(byte asc) {
        if ((asc >= '0') && (asc <= '9'))
            return (byte) (asc - '0');
        else if ((asc >= 'A') && (asc <= 'F'))
            return (byte) (asc - 'A' + 10);
        else if ((asc >= 'a') && (asc <= 'f'))
            return (byte) (asc - 'a' + 10);
        else
            return (byte) (asc - 48);
    }

    /**
     * BCD 转 数字
     *
     * @param bcd byte
     * @return int
     */
    public static int bcdByteToInt(byte bcd) {
        return ((bcd & 0xF0) >>> 4) * 10 + (bcd & 0x0F);
    }


    /**
     * char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) HEX.indexOf(c);
    }

    /**
     * Hex 转 BCD字节数组
     *
     * @param hex String
     * @return BCD字节数组
     */
    public static byte[] toBytes(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] cr = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (charToByte(cr[pos]) << 4 | charToByte(cr[pos + 1]));
        }
        return result;
    }

    /**
     * BCD字节数组 转 Hex
     *
     * @param bcd BCD字节数组
     * @return Hex
     */
    public static String bcdBytesToHex(byte[] bcd) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bcd) {
            int highNibble = (b >> 4) & 0x0F;
            int lowNibble = b & 0x0F;
            sb.append(Integer.toHexString(highNibble));
            sb.append(Integer.toHexString(lowNibble));
        }
        return sb.toString().toUpperCase();
    }

    private static final int HOUR_24 = 0x24;
    private static final int ZERO_TIME = 0x00;
    private static final int BCD_DATE_LENGTH = 8;
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * BCD编码的时间转换为LocalDateTime对象
     * 格式: YYYYMMDDHHmmss (8字节BCD)
     * 示例: 20240315235959
     *
     * @param bcdBytes BCD编码的时间字节数组，必须是8字节
     * @return 转换后的LocalDateTime对象，如果输入无效则返回null
     * @throws IllegalArgumentException 如果输入字节数组长度不是8
     */
    public static LocalDateTime bcdToDate(byte[] bcdBytes) {
        if (bcdBytes == null || bcdBytes.length != BCD_DATE_LENGTH) {
            throw new IllegalArgumentException("BCD日期字节必须为8字节长度");
        }

        // 检查是否全为0
        boolean allZero = true;
        for (int i = 0; i < 7; i++) {
            if (bcdBytes[i] != ZERO_TIME) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            return null;
        }

        // 使用StringBuilder预分配容量，避免扩容
        StringBuilder timeStr = new StringBuilder(14);

        // 年月日
        for (int i = 0; i < 4; i++) {
            appendBcdByte(timeStr, bcdBytes[i]);
        }

        // 小时特殊处理
        byte hour = bcdBytes[4];
        if ((hour & 0xff) == HOUR_24) {
            timeStr.append("00");
        } else {
            appendBcdByte(timeStr, hour);
        }

        // 分秒
        appendBcdByte(timeStr, bcdBytes[5]);
        appendBcdByte(timeStr, bcdBytes[6]);

        try {
            return LocalDateTime.parse(timeStr.toString(), DATETIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将单个BCD字节追加到StringBuilder
     * 性能优化: 直接计算BCD值并追加，避免字符串转换
     */
    private static void appendBcdByte(StringBuilder sb, byte bcd) {
        // 高4位
        sb.append(DIGITS[(bcd >> 4) & 0x0F]);
        // 低4位
        sb.append(DIGITS[bcd & 0x0F]);
    }
    /**
     * LocalDateTime转换为8字节BCD编码
     * 格式: YYYYMMDDHHmmss + 0xFF (8字节BCD)
     * 示例: 20240315235959FF
     *
     * @param dateTime 要转换的LocalDateTime对象
     * @return 8字节BCD编码的字节数组，最后一个字节固定为0xFF；如果输入为null则返回null
     */
    public static byte[] dateToBcd8(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        byte[] bytes = new byte[8];
        
        // 年 (2字节)
        int year = dateTime.getYear();
        bytes[0] = (byte) ((year / 100) << 4 | (year / 10 % 10));
        bytes[1] = (byte) ((year % 10) << 4);

        // 月 (与年的最后4位共用一个字节)
        int month = dateTime.getMonthValue();
        bytes[1] |= (byte) (month / 10);
        bytes[2] = (byte) ((month % 10) << 4);

        // 日 (与月的最后4位共用一个字节)
        int day = dateTime.getDayOfMonth();
        bytes[2] |= (byte) (day / 10);
        bytes[3] = (byte) ((day % 10) << 4);

        // 时 (与日的最后4位共用一个字节)
        int hour = dateTime.getHour();
        bytes[3] |= (byte) (hour / 10);
        bytes[4] = (byte) ((hour % 10) << 4);

        // 分 (与时的最后4位共用一个字节)
        int minute = dateTime.getMinute();
        bytes[4] |= (byte) (minute / 10);
        bytes[5] = (byte) ((minute % 10) << 4);

        // 秒 (与分的最后4位共用一个字节)
        int second = dateTime.getSecond();
        bytes[5] |= (byte) (second / 10);
        bytes[6] = (byte) ((second % 10) << 4);

        // 最后一个字节固定为0xFF
        bytes[7] = (byte) 0xFF;

        return bytes;
    }

}
