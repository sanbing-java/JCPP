/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.codec;

import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import io.netty.buffer.ByteBuf;
import sanbing.jcpp.infrastructure.util.JCPPPair;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author 九筒
 */
public class ByteUtil {

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }

    public static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static byte[] stringToBytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);
        longBuffer.putLong(0, x);
        return longBuffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    /**
     * 计算校验和
     */
    public static int crcSum(byte[] data) {
        CRC16Modbus crc16Modbus = new CRC16Modbus();
        crc16Modbus.update(data);
        return (int) crc16Modbus.getValue();
    }

    /**
     * 验证校验和
     */
    public static JCPPPair<Boolean, Integer> checkCrcSum(byte[] data, int checkSum) {
        int expectedCs = crcSum(data);
        return JCPPPair.of(expectedCs == checkSum, expectedCs);
    }

    /**
     * ByteBuf转byte数组
     *
     * @param byteBuf ByteBuf对象
     * @return 转换后的字节数组
     */
    public static byte[] toBytes(ByteBuf byteBuf) {
        int msgLength = byteBuf.readableBytes();
        byte[] bytes = new byte[msgLength];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    /**
     * 计算字节数组的累加和，如果累加结果超过1字节，则只取低8位
     * <p>
     * 示例：
     * byte[] data = {0x01, 0x02, 0x03};
     * byte sum = calculateSum(data); // sum = 0x06
     * <p>
     * byte[] data2 = {(byte)0xFF, (byte)0xFF};
     * byte sum2 = calculateSum(data2); // sum2 = (byte)0xFE (254 + 255 = 509, 取低8位为254)
     *
     * @param data 要计算累加和的字节数组
     * @return 累加和的低8位
     * @throws IllegalArgumentException 如果输入数组为null
     */
    public static byte calculateSum(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("输入数组不能为null");
        }
        
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xFF;
        }
        
        return (byte) (sum & 0xFF);
    }

    /**
     * 验证数据的累加和是否与期望值相等
     * <p>
     * 示例：
     * byte[] data = {0x01, 0x02, 0x03};
     * boolean valid = verifySum(data, (byte)0x06); // valid = true
     * 
     * @param data 要验证的数据
     * @param expectedSum 期望的累加和
     * @return 包含验证结果和实际计算出的累加和的键值对
     * @throws IllegalArgumentException 如果输入数组为null
     */
    public static JCPPPair<Boolean, Byte> verifySum(byte[] data, byte expectedSum) {
        byte actualSum = calculateSum(data);
        return JCPPPair.of(actualSum == expectedSum, actualSum);
    }
}