/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.codec;


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

}
