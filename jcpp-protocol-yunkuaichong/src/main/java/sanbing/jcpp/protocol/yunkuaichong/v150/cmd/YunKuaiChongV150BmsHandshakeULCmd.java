/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import cn.hutool.core.util.HexUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.BmsHandshakeProto;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;

import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 充电握手
 *
 * @author baigod
 */
@Slf4j
@YunKuaiChongCmd(value = 0x15, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150BmsHandshakeULCmd extends YunKuaiChongUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0充电握手", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        // 从Tracer总获取当前时间
        long ts = TracerContextUtil.getCurrentTracer().getTracerTs();

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        // 1.交易流水号
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = BCDUtil.toString(tradeNoBytes);

        // 2.桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 3.抢号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        // 4.BMS 通信协议版本号
        byte[] bmsConnectVersionBytes = new byte[3];
        byteBuf.readBytes(bmsConnectVersionBytes);

        // 5.BMS 电池类型
        int bmsBatteryType = byteBuf.readUnsignedByte();

        // 6.BMS 整车动力蓄电池系统额定容量
        int bmsPowerCapacity = byteBuf.readUnsignedShortLE();

        // 7.BMS 整车动力蓄电池系统额定总电压
        int bmsPowerMaxVoltage = byteBuf.readUnsignedShortLE();

        // 8.BMS 电池生产厂商名称
        byte[] bmsFactoryBytes = new byte[4];
        byteBuf.readBytes(bmsFactoryBytes);
        String bmsFactory = new String(bmsFactoryBytes, StandardCharsets.US_ASCII);

        // 9.BMS 电池组序号
        int bmsSerialNo = byteBuf.readIntLE();

        // 10.BMS 电池组生产日期年
        int bmsCreateYear = byteBuf.readUnsignedByte();

        // 11.BMS 电池组生产日期月
        int bmsCreateMonth = byteBuf.readUnsignedByte();

        // 12.BMS 电池组生产日期日
        int bmsCreateDay = byteBuf.readUnsignedByte();

        // 13.BMS 电池组充电次数
        int bmsChargeCount = byteBuf.readUnsignedMedium();

        // 14.BMS 电池组产权标识
        int bmsPropertyRightLabel = byteBuf.readUnsignedByte();

        // 15.预留位
        byteBuf.skipBytes(1);

        // 16.BMS 车辆识别码
        byte[] carVINBytes = new byte[17];
        byteBuf.readBytes(carVINBytes);
        String bmsVinCode = new String(carVINBytes, StandardCharsets.US_ASCII);

        // 17.BMS 软件版本号
        byte[] bmsSoftVersionBytes = new byte[8];
        byteBuf.readBytes(bmsSoftVersionBytes);

        // 构建BmsHandshakeProto对象
        BmsHandshakeProto bmsHandshakeProto = BmsHandshakeProto.newBuilder()
                .setTs(ts)
                .setPileCode(pileCode)
                .setGunCode(gunCode)
                .setTradeNo(tradeNo)
                .setBmsProtocolVersion(HexUtil.encodeHexStr(bmsConnectVersionBytes))
                .setBmsBatteryType(bmsBatteryType)
                .setBmsPowerCapacity(bmsPowerCapacity)
                .setBmsPowerMaxVoltage(bmsPowerMaxVoltage)
                .setBmsFactory(bmsFactory)
                .setBmsSerialNo(bmsSerialNo)
                .setBmsCreateYear(bmsCreateYear)
                .setBmsCreateMonth(bmsCreateMonth)
                .setBmsCreateDay(bmsCreateDay)
                .setBmsChargeCount(bmsChargeCount)
                .setBmsPropertyRightLabel(bmsPropertyRightLabel)
                .setCarVinCode(bmsVinCode)
                .setBmsSoftwareVersion(HexUtil.encodeHexStr(bmsSoftVersionBytes))
                .setAdditionalInfo(additionalInfo.toString())
                .build();
                
        // 构建上行队列消息
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setBmsHandshakeProto(bmsHandshakeProto)
                .build();
                
        // 转发到应用层
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }
}