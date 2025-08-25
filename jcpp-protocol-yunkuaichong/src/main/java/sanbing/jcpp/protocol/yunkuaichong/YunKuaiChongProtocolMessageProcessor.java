/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong;

import cn.hutool.core.util.ClassUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.JCPPPair;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.DownlinkRequestMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;
import sanbing.jcpp.protocol.domain.ListenerToHandlerMsg;
import sanbing.jcpp.protocol.domain.SessionToHandlerMsg;
import sanbing.jcpp.protocol.forwarder.Forwarder;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;
import sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongDownlinkCmdEnum;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static sanbing.jcpp.infrastructure.util.codec.ByteUtil.checkCrcSum;

@Slf4j
public class YunKuaiChongProtocolMessageProcessor extends ProtocolMessageProcessor {
    // 修改为基于协议名+cmd的路由映射，格式为 "protocolName:cmd" -> CommandExecutor
    private final Map<String, YunKuaiChongUplinkCmdExe> uplinkCmdExeMap = new ConcurrentHashMap<>();
    private final Map<String, YunKuaiChongDownlinkCmdExe> downlinkCmdExeMap = new ConcurrentHashMap<>();

    public YunKuaiChongProtocolMessageProcessor(Forwarder forwarder, ProtocolContext protocolContext) {
        super(forwarder, protocolContext);

        Set<Class<?>> cmdClasses = ClassUtil.scanPackageByAnnotation(ClassUtil.getPackage(this.getClass()), YunKuaiChongCmd.class);
        cmdClasses.stream().filter(YunKuaiChongUplinkCmdExe.class::isAssignableFrom)
                .forEach(clazz -> {
                    YunKuaiChongCmd annotation = clazz.getAnnotation(YunKuaiChongCmd.class);
                    int cmd = annotation.value();
                    String[] protocolNames = annotation.protocolNames();
                    try {
                        YunKuaiChongUplinkCmdExe yunKuaiChongUplinkCmdExe = (YunKuaiChongUplinkCmdExe) clazz.getDeclaredConstructor().newInstance();
                        // 为每个支持的协议名注册命令执行器
                        for (String protocolName : protocolNames) {
                            String key = protocolName + ":" + cmd;
                            uplinkCmdExeMap.put(key, yunKuaiChongUplinkCmdExe);
                        }
                    } catch (InstantiationException |
                             IllegalAccessException |
                             InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });

        cmdClasses.stream().filter(YunKuaiChongDownlinkCmdExe.class::isAssignableFrom)
                .forEach(clazz -> {
                    YunKuaiChongCmd annotation = clazz.getAnnotation(YunKuaiChongCmd.class);
                    int cmd = annotation.value();
                    String[] protocolNames = annotation.protocolNames();
                    try {
                        YunKuaiChongDownlinkCmdExe yunKuaiChongDownlinkCmdExe = (YunKuaiChongDownlinkCmdExe) clazz.getDeclaredConstructor().newInstance();
                        // 为每个支持的协议名注册命令执行器
                        for (String protocolName : protocolNames) {
                            String key = protocolName + ":" + cmd;
                            downlinkCmdExeMap.put(key, yunKuaiChongDownlinkCmdExe);
                        }
                    } catch (InstantiationException |
                             IllegalAccessException |
                             InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
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

        int cmd = YunKuaiChongDownlinkCmdEnum.valueOf(protocolDownlinkMsg.getDownlinkCmd()).getCmd();

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
        String key = protocolName + ":" + cmd;
        
        YunKuaiChongUplinkCmdExe uplinkCmdExe = uplinkCmdExeMap.get(key);

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
        String key = protocolName + ":" + cmd;
        
        YunKuaiChongDownlinkCmdExe downlinkCmdExe = downlinkCmdExeMap.get(key);

        if (downlinkCmdExe == null) {
            log.info("{} 云快充协议接收到未知的下行指令，协议: {}, 指令: 0x{}", 
                session, protocolName, Integer.toHexString(cmd));
            return;
        }

        downlinkCmdExe.execute(session, message, protocolContext);
    }


}
