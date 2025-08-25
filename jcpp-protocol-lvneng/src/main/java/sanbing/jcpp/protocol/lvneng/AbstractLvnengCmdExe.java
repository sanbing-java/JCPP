/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import sanbing.jcpp.infrastructure.util.codec.ByteUtil;
import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.listener.tcp.enums.SequenceNumberLength;
import sanbing.jcpp.protocol.lvneng.mapping.LvnengDownlinkCmdConverter;
import sanbing.jcpp.protocol.mapping.DownlinkCmdConverter;

/**
 * 绿能协议基础类
 */
public class AbstractLvnengCmdExe {

    protected static final int LVNENG_HEAD = 0xAAF5;
    protected static final int LVNENG_ENCRYPTION_FLAG = 0x10;
    
    /**
     * 下行命令转换器单例，用于将通用命令转换为协议特定命令值
     */
    private static final DownlinkCmdConverter DOWNLINK_CMD_CONVERTER = LvnengDownlinkCmdConverter.getInstance();

    /**
     * 编码下行消息
     * 格式：帧头(2) + 长度(2) + 加密标识(1) + 序号(1) + 命令字(2) + 数据域(n) + 校验和(1)
     */
    protected byte[] encode(int downlinkCmd,
                          int seqNo,
                          int encryptionFlag,
                          ByteBuf msgBody) {
        // 1. 计算长度
        int msgBodyLength = msgBody.readableBytes();
        int totalLength = msgBodyLength + 9;  // 总长度 = 数据域长度 + 9字节固定头尾

        // 2. 构建消息头和数据域
        ByteBuf response = Unpooled.buffer(totalLength);
        response.writeShort(LVNENG_HEAD);             // 帧头
        response.writeShortLE(totalLength);           // 长度
        response.writeByte(encryptionFlag);           // 加密标识
        response.writeByte(seqNo);                    // 序号
        response.writeShortLE(downlinkCmd);  // 命令字
        response.writeBytes(msgBody);                 // 数据域

        // 3. 准备校验和计算的数据（命令字 + 数据域）
        byte[] sumData = new byte[2 + msgBodyLength];  // 2字节命令字 + 数据域
        sumData[0] = (byte) (downlinkCmd & 0xFF);
        sumData[1] = (byte) ((downlinkCmd >> 8) & 0xFF);
        if (msgBodyLength > 0) {
            System.arraycopy(response.array(), 8, sumData, 2, msgBodyLength);
        }

        // 4. 计算并写入校验和
        response.writeByte(ByteUtil.calculateSum(sumData));

        // 5. 转换为字节数组
        return ByteUtil.toBytes(response);
    }

    /**
     * 编码并发送消息（完整参数版本）
     */
    protected void encodeAndWriteFlush(int downlinkCmd,
                                     int seqNo,
                                     int encryptionFlag,
                                     ByteBuf msgBody,
                                     TcpSession tcpSession) {
        byte[] encode = encode(downlinkCmd, seqNo, encryptionFlag, msgBody);
        tcpSession.writeAndFlush(Unpooled.copiedBuffer(encode));
    }

    /**
     * 编码并发送消息（简化参数版本）
     * 使用默认的加密标识和自增序号
     */
    protected void encodeAndWriteFlush(int downlinkCmd,
                                     ByteBuf msgBody,
                                     TcpSession tcpSession) {
        byte[] encode = encode(downlinkCmd,
                tcpSession.nextSeqNo(SequenceNumberLength.SHORT),
                LVNENG_ENCRYPTION_FLAG,
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
            throw new IllegalArgumentException("绿能协议不支持下行命令: " + downlinkCmdEnum);
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
            throw new IllegalArgumentException("绿能协议不支持下行命令: " + downlinkCmdEnum);
        }
        Integer cmd = DOWNLINK_CMD_CONVERTER.convertToCmd(downlinkCmdEnum);
        encodeAndWriteFlush(cmd, msgBody, tcpSession);
    }
}