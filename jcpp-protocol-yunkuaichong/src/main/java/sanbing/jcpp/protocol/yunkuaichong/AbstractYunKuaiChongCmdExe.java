/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong;

import cn.hutool.core.text.CharSequenceUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.PeriodProto;
import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.listener.tcp.enums.SequenceNumberLength;
import sanbing.jcpp.protocol.mapping.DownlinkCmdConverter;
import sanbing.jcpp.protocol.yunkuaichong.mapping.YunKuaiChongDownlinkCmdConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.List;

import static sanbing.jcpp.infrastructure.util.codec.ByteUtil.crcSum;
import static sanbing.jcpp.infrastructure.util.codec.ByteUtil.toBytes;

/**
 * @author 九筒
 */
public class AbstractYunKuaiChongCmdExe {

    private static final byte TOP_BYTE = 0x00;
    private static final byte PEAK_BYTE = 0x01;
    private static final byte FLAT_BYTE = 0x02;
    private static final byte VALLEY_BYTE = 0x03;

    protected static final byte YUNKUAICHONG_HEAD = 0x68;
    protected static final int YUNKUAICHONG_NORMAL_ENCRYPTION_FLAG = 0;
    
    /**
     * 下行命令转换器单例，用于将通用命令转换为协议特定命令值
     */
    private static final DownlinkCmdConverter DOWNLINK_CMD_CONVERTER = YunKuaiChongDownlinkCmdConverter.getInstance();

    private static final DecimalFormat PRICING_ID_DECIMAL_FORMAT = new DecimalFormat("0000");


    protected static final String SUCCESS = "成功";

    protected static final String UNKNOWN_MSG = "未知的异常";


    protected static String decodeTradeNo(byte[] tradeNo) {
        String tradeNoStr = BCDUtil.toString(tradeNo);
        return CharSequenceUtil.strip(tradeNoStr, "0", null);
    }

    protected byte[] encodePricingId(long pricingId) {
        return BCDUtil.toBytes(PRICING_ID_DECIMAL_FORMAT.format(pricingId % 10000));
    }

    protected static byte getFlagForCurrentTime(List<PeriodProto> periodList, LocalTime currentTime) {
        for (PeriodProto period : periodList) {
            LocalTime beginLt = LocalTime.parse(period.getBegin());
            LocalTime endLt = "00:00".equals(period.getEnd()) ? LocalTime.MAX : LocalTime.parse(period.getEnd());
            if ((currentTime.equals(beginLt) || currentTime.isAfter(beginLt)) && currentTime.isBefore(endLt)) {
                switch (period.getFlag()) {
                    case TOP:
                        return TOP_BYTE;
                    case PEAK:
                        return PEAK_BYTE;
                    case VALLEY:
                        return VALLEY_BYTE;
                    default:
                        return FLAT_BYTE;
                }
            }
        }
        return FLAT_BYTE; // 默认情况下返回平价
    }

    protected static byte[] encodePileCode(String pileCode) {
        if (StringUtils.length(pileCode) > 14) {
            throw new IllegalArgumentException("云快充可接受最大桩编号为14位");
        }

        String pileCodeStr = StringUtils.leftPad(pileCode, 14, '0');

        return BCDUtil.toBytes(pileCodeStr);
    }

    protected static byte[] encodeGunCode(String gunCode) {
        if (StringUtils.length(gunCode) > 2) {
            throw new IllegalArgumentException("云快充可接受最大枪编号为2位");
        }

        String gunCodeStr = StringUtils.leftPad(gunCode, 2, '0');

        return BCDUtil.toBytes(gunCodeStr);
    }

    protected static byte[] encodeTradeNo(String tradeNo) {
        if (StringUtils.length(tradeNo) > 32) {
            throw new IllegalArgumentException("云快充1.5可接受最大交易流水号为32位");
        }

        String tradeNoStr = StringUtils.leftPad(tradeNo, 32, '0');

        return BCDUtil.toBytes(tradeNoStr);
    }

    /**
     * 编码卡号为BCD格式
     * 云快充协议卡号为8字节（16位BCD码），需要做长度校验和补0操作
     */
    protected static byte[] encodeCardNo(String cardNo) {
        if (StringUtils.length(cardNo) > 16) {
            throw new IllegalArgumentException("云快充可接受最大卡号为16位");
        }

        String cardNoStr = StringUtils.leftPad(cardNo, 16, '0');

        return BCDUtil.toBytes(cardNoStr);
    }

    protected byte[] encode(int downlinkCmd,
                            int seqNo,
                            int encryptionFlag,
                            ByteBuf msgBody) {
        int msgBodyLength = msgBody.readableBytes();
        ByteBuf response = Unpooled.buffer(msgBodyLength + 6);
        response.writeByte(YUNKUAICHONG_HEAD);
        response.writeByte(msgBodyLength + 4);
        response.writeShortLE(seqNo);
        response.writeByte(encryptionFlag);
        response.writeByte(downlinkCmd);
        response.writeBytes(msgBody);

        // 帧校验域：从序列号域到数据域的 CRC 校验，校验多项式为 0x180D,低字节在前，高字节在后
        byte[] checkArr = new byte[msgBodyLength + 4];
        System.arraycopy(response.array(), 2, checkArr, 0, checkArr.length);

        response.writeShortLE(crcSum(checkArr));

        return toBytes(response);
    }

    protected void encodeAndWriteFlush(int downlinkCmd,
                                       int seqNo,
                                       int encryptionFlag,
                                       ByteBuf msgBody,
                                       TcpSession tcpSession) {

        byte[] encode = encode(downlinkCmd, seqNo, encryptionFlag, msgBody);

        tcpSession.writeAndFlush(Unpooled.copiedBuffer(encode));
    }

    protected void encodeAndWriteFlush(int downlinkCmd,
                                       ByteBuf msgBody,
                                       TcpSession tcpSession) {

        byte[] encode = encode(downlinkCmd,
                tcpSession.nextSeqNo(SequenceNumberLength.SHORT),
                YUNKUAICHONG_NORMAL_ENCRYPTION_FLAG,
                msgBody);

        tcpSession.writeAndFlush(Unpooled.copiedBuffer(encode));
    }

    /**
     * 便捷方法：直接使用 DownlinkCmdEnum 发送命令
     */
    protected void encodeAndWriteFlush(DownlinkCmdEnum downlinkCmdEnum,
                                       int seqNo,
                                       int encryptionFlag,
                                       ByteBuf msgBody,
                                       TcpSession tcpSession) {
        if (!DOWNLINK_CMD_CONVERTER.supports(downlinkCmdEnum)) {
            throw new IllegalArgumentException("云快充协议不支持下行命令: " + downlinkCmdEnum);
        }
        Integer cmd = DOWNLINK_CMD_CONVERTER.convertToCmd(downlinkCmdEnum);
        encodeAndWriteFlush(cmd, seqNo, encryptionFlag, msgBody, tcpSession);
    }

    /**
     * 便捷方法：直接使用 DownlinkCmdEnum 发送命令（自动生成序列号）
     */
    protected void encodeAndWriteFlush(DownlinkCmdEnum downlinkCmdEnum,
                                       ByteBuf msgBody,
                                       TcpSession tcpSession) {
        if (!DOWNLINK_CMD_CONVERTER.supports(downlinkCmdEnum)) {
            throw new IllegalArgumentException("云快充协议不支持下行命令: " + downlinkCmdEnum);
        }
        Integer cmd = DOWNLINK_CMD_CONVERTER.convertToCmd(downlinkCmdEnum);
        encodeAndWriteFlush(cmd, msgBody, tcpSession);
    }

    protected static BigDecimal reduceMagnification(long value, int magnification) {
        return new BigDecimal(value).divide(new BigDecimal(magnification), 4, RoundingMode.HALF_UP);
    }

    protected static BigDecimal reduceMagnification(long value, int magnification, int scale) {
        return new BigDecimal(value).divide(new BigDecimal(magnification), scale, RoundingMode.HALF_UP);
    }

    /**
     * 高性能版本：直接写入目标ByteBuf，避免创建中间ByteBuf对象
     * 将字符串参数写入ByteBuf，不足指定长度用0填充（右侧填充）
     * 使用ASCII编码，适用于协议中的字符串字段
     * 
     * @param target 目标ByteBuf
     * @param param 要写入的字符串参数，可以为null
     * @param maxLength 目标长度（字节数）
     * @throws IllegalArgumentException 如果参数字节长度超过maxLength
     */
    protected static void writeParamFillZero(ByteBuf target, String param, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大长度必须为正数");
        }
        
        if (param == null) {
            param = "";
        }
        
        // 使用ASCII编码，适用于协议字段
        byte[] paramBytes = param.getBytes(StandardCharsets.US_ASCII);
        
        // 检查长度
        if (paramBytes.length > maxLength) {
            throw new IllegalArgumentException(
                String.format("云快充1.5.0 参数: %s 字节长度 %d 超过最大长度 %d",
                    param, paramBytes.length, maxLength));
        }
        
        // 确保目标ByteBuf有足够的写入空间
        target.ensureWritable(maxLength);
        
        // 写入数据
        target.writeBytes(paramBytes);
        
        // 填充剩余空间
        int remainingBytes = maxLength - paramBytes.length;
        if (remainingBytes > 0) {
            target.writeZero(remainingBytes);
        }
    }

    /**
     * 高性能版本：将整数参数作为字节值直接写入ByteBuf，不足指定长度用0填充（右侧填充）
     * 
     * @param target 目标ByteBuf
     * @param param 要写入的整数参数（将作为字节值写入，只使用低8位）
     * @param maxLength 目标长度（字节数）
     */
    protected static void writeParamFillZero(ByteBuf target, int param, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大长度必须为正数");
        }
        
        // 确保目标ByteBuf有足够的写入空间
        target.ensureWritable(maxLength);
        
        // 将整数作为字节值写入（只使用低8位）
        target.writeByte(param & 0xFF);
        
        // 计算剩余空间并填充0
        int remainingBytes = maxLength - 1; // 已经写入了1个字节
        if (remainingBytes > 0) {
            target.writeZero(remainingBytes);
        }
    }

}