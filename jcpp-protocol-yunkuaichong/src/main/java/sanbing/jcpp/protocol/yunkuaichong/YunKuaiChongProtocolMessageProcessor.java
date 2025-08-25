/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.JCPPPair;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.DownlinkRequestMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;
import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;
import sanbing.jcpp.protocol.domain.ListenerToHandlerMsg;
import sanbing.jcpp.protocol.domain.SessionToHandlerMsg;
import sanbing.jcpp.protocol.forwarder.Forwarder;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.mapping.DownlinkCmdConverter;
import sanbing.jcpp.protocol.routing.ProtocolCommandRouter;
import sanbing.jcpp.protocol.yunkuaichong.mapping.YunKuaiChongDownlinkCmdConverter;

import java.util.Arrays;
import java.util.UUID;

import static sanbing.jcpp.infrastructure.util.codec.ByteUtil.checkCrcSum;

@Slf4j
public class YunKuaiChongProtocolMessageProcessor extends ProtocolMessageProcessor {
    
    private final ProtocolCommandRouter<YunKuaiChongUplinkCmdExe> uplinkRouter;
    private final ProtocolCommandRouter<YunKuaiChongDownlinkCmdExe> downlinkRouter;
    private final DownlinkCmdConverter downlinkCmdConverter;

    public YunKuaiChongProtocolMessageProcessor(Forwarder forwarder, ProtocolContext protocolContext) {
        super(forwarder, protocolContext);

        // 使用 CommandRouter 替代手动注册逻辑
        this.uplinkRouter = new ProtocolCommandRouter<>(
            this.getClass(),
            YunKuaiChongUplinkCmdExe.class::isAssignableFrom
        );

        this.downlinkRouter = new ProtocolCommandRouter<>(
            this.getClass(),
            YunKuaiChongDownlinkCmdExe.class::isAssignableFrom
        );
        
        // 获取下行命令转换器单例
        this.downlinkCmdConverter = YunKuaiChongDownlinkCmdConverter.getInstance();
    }

    @Override
    public void uplinkHandle(ListenerToHandlerMsg listenerToHandlerMsg) {
        final UUID msgId = listenerToHandlerMsg.id();
        final byte[] msg = listenerToHandlerMsg.msg();
        final TcpSession session = (TcpSession) listenerToHandlerMsg.session();

        // ================== 前置快速失败检查 ==================
        if (msg.length < 8 || msg[0] != 0x68) {
            return;
        }

        ByteBuf in = Unpooled.wrappedBuffer(msg);
        try {
            // ================== 协议头解析 ==================
            final int dataLength = in.getUnsignedByte(1);
            final int bodyLength = dataLength - 4;
            final int checksumPos = 6 + bodyLength;

            // ================== 组合边界检查 ==================
            if (dataLength < 4 || in.readableBytes() < checksumPos + 2) {
                return;
            }

            // ================== 字段快速解析 ==================
            final int seqNo = in.getUnsignedShort(2);
            final int encryptFlag = in.getUnsignedByte(4);
            final int frameType = in.getUnsignedByte(5);

            // ================== 校验和双模式处理 ==================
            final int checkSumLE = in.getUnsignedShortLE(checksumPos);
            final int checkSumBE = in.getUnsignedShort(checksumPos);
            byte[] checkSumBytes = new byte[2];
            in.getBytes(checksumPos, checkSumBytes);

            // ================== 校验数据智能拷贝 ==================
            final byte[] checkData = Arrays.copyOfRange(msg, 2, 2 + dataLength);

            // ================== 短路校验流程 ==================
            JCPPPair<Boolean, Integer> checkResult = checkCrcSum(checkData, checkSumLE);
            if (!checkResult.getFirst()) {
                if (log.isDebugEnabled()) { // 日志惰性计算
                    log.debug("{} 云快充校验域一次校验失败 CMD:{} 校验和：0x{} 期望校验和:{}",
                            session, Integer.toHexString(frameType), ByteBufUtil.hexDump(checkSumBytes), checkResult.getSecond());
                }
                checkResult = checkCrcSum(checkData, checkSumBE);
            }

            // ================== 最终校验失败处理 ==================
            if (!checkResult.getFirst()) {
                log.info("{} 云快充校验域二次校验失败 CMD:{} 校验和：0x{} 期望校验和:{}",
                        session, Integer.toHexString(frameType), ByteBufUtil.hexDump(checkSumBytes), checkResult.getSecond());
                return;
            }

            // ================== 消息对象智能构建 ==================
            ByteBuf slicedBuf = in.slice(6, bodyLength);

            if (slicedBuf.readableBytes() != bodyLength) {
                log.error("协议体长度异常: expected={}, actual={}",
                        bodyLength, slicedBuf.readableBytes());
                return;
            }

            byte[] msgBody = new byte[bodyLength];
            slicedBuf.readBytes(msgBody);

            exeCmd(new YunKuaiChongUplinkMessage(msgId)
                            .setHead(0x68)
                            .setDataLength(dataLength)
                            .setSequenceNumber(seqNo)
                            .setEncryptionFlag(encryptFlag)
                            .setCmd(frameType)
                            .setMsgBody(msgBody)  // 使用正确长度的数组
                            .setCheckSum(checkResult.getSecond())
                            .setRawFrame(msg),
                    session);
        } finally {
            in.release();
        }
    }

    @Override
    public void downlinkHandle(SessionToHandlerMsg sessionToHandlerMsg) {
        TcpSession session = (TcpSession) sessionToHandlerMsg.session();

        DownlinkRequestMessage protocolDownlinkMsg = sessionToHandlerMsg.downlinkMsg();

        DownlinkCmdEnum downlinkCmd = DownlinkCmdEnum.valueOf(protocolDownlinkMsg.getDownlinkCmd());
        
        // 首先检查是否支持该命令
        if (!downlinkCmdConverter.supports(downlinkCmd)) {
            log.warn("云快充协议不支持下行命令: {}", downlinkCmd);
            return;
        }
        
        // 支持的命令直接转换（这里不会返回null）
        Integer cmd = downlinkCmdConverter.convertToCmd(downlinkCmd);

        YunKuaiChongDwonlinkMessage message = new YunKuaiChongDwonlinkMessage();
        message.setId(new UUID(protocolDownlinkMsg.getMessageIdMSB(), protocolDownlinkMsg.getMessageIdLSB()));
        message.setCmd(cmd);
        message.setMsg(protocolDownlinkMsg);

        if (protocolDownlinkMsg.hasRequestIdMSB() && protocolDownlinkMsg.hasRequestIdLSB()) {
            message.setRequestId(new UUID(protocolDownlinkMsg.getRequestIdMSB(), protocolDownlinkMsg.getRequestIdLSB()));
        }

        if (protocolDownlinkMsg.hasRequestData()) {
            message.setRequestData(JacksonUtil.fromBytes(protocolDownlinkMsg.getRequestData().toByteArray(), YunKuaiChongUplinkMessage.class));
        }

        exeCmd(message, session);
    }

    private void exeCmd(YunKuaiChongUplinkMessage message, TcpSession session) {
        String protocolName = session.getProtocolName();
        int cmd = message.getCmd();
        
        YunKuaiChongUplinkCmdExe uplinkCmdExe = uplinkRouter.getExecutor(protocolName, cmd);

        if (uplinkCmdExe == null) {
            log.info("{} 云快充协议接收到未知的上行指令，协议: {}, 指令: 0x{}", 
                session, protocolName, Integer.toHexString(cmd));
            return;
        }

        uplinkCmdExe.execute(session, message, protocolContext);
    }

    private void exeCmd(YunKuaiChongDwonlinkMessage message, TcpSession session) {
        String protocolName = session.getProtocolName();
        int cmd = message.getCmd();
        
        YunKuaiChongDownlinkCmdExe downlinkCmdExe = downlinkRouter.getExecutor(protocolName, cmd);

        if (downlinkCmdExe == null) {
            log.info("{} 云快充协议接收到未知的下行指令，协议: {}, 指令: 0x{}", 
                session, protocolName, Integer.toHexString(cmd));
            return;
        }

        downlinkCmdExe.execute(session, message, protocolContext);
    }


}
