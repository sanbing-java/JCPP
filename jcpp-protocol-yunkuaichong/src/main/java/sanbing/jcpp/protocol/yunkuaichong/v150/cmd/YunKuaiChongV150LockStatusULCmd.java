/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.GroundLockStatusProto;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充协议上行命令处理类 - 地锁状态/报警信息帧 (V1.5.0版本)
 * 命令码：0x61 (地锁状态/报警信息帧上行命令)
 */
@Slf4j
@ProtocolCmd(value = 0x61, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150LockStatusULCmd extends YunKuaiChongUplinkCmdExe {

    /**
     * 执行命令解析
     * @param tcpSession TCP会话对象
     * @param yunKuaiChongUplinkMessage  上行消息对象
     * @param ctx        协议上下文
     */
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0地锁状态/报警信息帧请求", tcpSession);
        // 将消息体包装为ByteBuf以便读取
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        /* 按协议顺序解析消息体 */

        // 1. 桩编号：7字节BCD编码字符串
        String pileCode = readBcdString(byteBuf, 7);

        // 2. 枪号：1字节
        int gunNo = byteBuf.readUnsignedByte();
        String gunCode = String.valueOf(gunNo);

        // 3. 车位锁状态：1字节
        int lockStatus = byteBuf.readUnsignedByte();

        // 4. 车位状态：1字节
        int parkStatus = byteBuf.readUnsignedByte();

        // 5. 地锁电量状态：1字节 (百分比值0~100)
        int lockBattery = byteBuf.readUnsignedByte();

        // 6. 报警状态：1字节
        int alarmStatus = byteBuf.readUnsignedByte();

        // 7. 预留位：4字节
        int reserved = byteBuf.readInt();

        // 记录日志
        log.info("地锁状态信息 - 桩编号:{}, 枪号:{}, 车位锁状态:{}, 车位状态:{}, 地锁电量:{}, 报警状态:{}",
                pileCode, gunCode, lockStatus, parkStatus, lockBattery, alarmStatus);

        // 构建转发消息
        GroundLockStatusProto groundLockStatusProto = GroundLockStatusProto.newBuilder()
                .setPileCode(pileCode)
                .setGunCode(gunCode)
                .setLockStatus(lockStatus)
                .setParkStatus(parkStatus)
                .setLockBattery(lockBattery)
                .setAlarmStatus(alarmStatus)
                .setReserved(reserved)
                .build();

        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(groundLockStatusProto.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setGroundLockStatusProto(groundLockStatusProto)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    //=== 协议数据解析辅助方法 ===//

    /**
     * 读取BCD编码字符串
     * @param buf    字节缓冲区
     * @param length 读取字节长度
     * @return 解析后的字符串
     */
    private String readBcdString(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return BCDUtil.toString(bytes);  // 调用BCD工具类转换
    }
}