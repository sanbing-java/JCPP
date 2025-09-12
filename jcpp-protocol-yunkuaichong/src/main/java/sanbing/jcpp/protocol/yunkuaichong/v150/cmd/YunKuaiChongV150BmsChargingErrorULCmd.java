/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.UplinkProto.BmsChargingErrorProto;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0充电桩错误报文
 */
@Slf4j
@ProtocolCmd(value = 0x1B, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150BmsChargingErrorULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0 充电桩与 BMS 充电错误报文", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        //1 交易流水号
        String tradeNo = BCDUtil.toString(tradeNoBytes);

        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        //2 桩编号
        String pileCode = BCDUtil.toString(pileCodeBytes);
        //3 枪号
        byte[] gunCodeBytes = new byte[1];
        byteBuf.readBytes(gunCodeBytes);
        String gunCode = BCDUtil.toString(gunCodeBytes);
        //<00>：=正常；<01>：=超时；<10>： =不可信状态
        //4 接收 SPN2560=0x00 的充电机辨识报文超时 2位
        //5 接收 SPN2560=0xAA 的充电机辨识报文超时 2位
        //6 预留位 4位
        byte chargerByte = byteBuf.readByte();
        int time00 = chargerByte & 0x03;
        int timeAA = (chargerByte & 0x0C) >>> 2;
        additionalInfo.put("接收 SPN2560=0x00 的充电机辨识报文超时", getErrorDescriptions(time00));
        additionalInfo.put("接收 SPN2560=0xAA 的充电机辨识报文超时", getErrorDescriptions(timeAA));
        //7 接收充电机的时间同步和充电机最大输出能力报文超时 2位
        //8 接收充电机完成充电准备报文超时 2位
        //9 预留位 4位
        byte chargerByte2 = byteBuf.readByte();
        int timeSyn = chargerByte2 & 0x03;
        int timeFinish = (chargerByte2 & 0x0C) >>> 2;
        additionalInfo.put("接收充电机的时间同步和充电机最大输出能力报文超时", getErrorDescriptions(timeSyn));
        additionalInfo.put("接收充电机完成充电准备报文超时", getErrorDescriptions(timeFinish));
        //10 接收充电机充电状态报文超时 2位
        //11 接收充电机中止充电报文超时 2位
        //12 预留位 4位
        byte chargerByte3 = byteBuf.readByte();
        int timeStatus = chargerByte3 & 0x03;
        int timeEnd = (chargerByte3 & 0x0C) >>> 2;
        additionalInfo.put("接收充电机充电状态报文超时", getErrorDescriptions(timeStatus));
        additionalInfo.put("接收充电机中止充电报文超时", getErrorDescriptions(timeEnd));
        //13 接收充电机充电统计报文超时 2位
        //14 BMS 其他 6位
        byte chargerByte4 = byteBuf.readByte();
        int timeStatistics = chargerByte4 & 0x03;
        additionalInfo.put("接收充电机充电统计报文超时", getErrorDescriptions(timeStatistics));
        //15 接收 BMS 和车辆的辨识报文超时 2位
        //16 预留位 6位
        byte bmsByte = byteBuf.readByte();
        int timeBms = bmsByte & 0x03;
        additionalInfo.put("接收 BMS 和车辆的辨识报文超时", getErrorDescriptions(timeBms));
        //17 接收电池充电参数报文超时 2位
        //18 接收 BMS 完成充电准备报文超时 2位
        //19 预留位 4位
        byte bmsByte2 = byteBuf.readByte();
        int timeBmsParam = bmsByte2 & 0x03;
        int timeBmsEnd = (bmsByte2 & 0x0C) >>> 2;
        additionalInfo.put("接收电池充电参数报文超时", getErrorDescriptions(timeBmsParam));
        additionalInfo.put("接收 BMS 完成充电准备报文超时", getErrorDescriptions(timeBmsEnd));
        //20 接收电池充电总状态报文超时 2位
        //21 接收电池充电要求报文超时 2位
        //22 接收 BMS 中止充电报文超时 2位
        //23 预留位 2位
        byte batteryByte = byteBuf.readByte();
        int timeBatteryStatus = batteryByte & 0x03;
        int timeBatteryRequest = (batteryByte & 0x0C) >>> 2;
        int timeBatteryEnd = (batteryByte & 0x30) >>> 4;
        additionalInfo.put("接收电池充电总状态报文超时", getErrorDescriptions(timeBatteryStatus));
        additionalInfo.put("接收电池充电要求报文超时", getErrorDescriptions(timeBatteryRequest));
        additionalInfo.put("接收 BMS 中止充电报文超时", getErrorDescriptions(timeBatteryEnd));
        //24 接收 BMS 充电统计报文超时 2位
        //25 充电机其他 6位
        byte bmsByte3 = byteBuf.readByte();
        int timeBmsTotal = bmsByte3 & 0x03;
        additionalInfo.put("接收 BMS 充电统计报文超时", getErrorDescriptions(timeBmsTotal));

        tcpSession.addPileCode(pileCode);

        // 注册前置会话
        ctx.getProtocolSessionRegistryProvider().register(tcpSession);

        // 转发到后端
        BmsChargingErrorProto bmsChargingErrorProto = BmsChargingErrorProto.newBuilder()
                .setPileCode(pileCode)
                .setTradeNo(tradeNo)
                .setGunCode(gunCode)
                .setAdditionalInfo(additionalInfo.toString())
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(bmsChargingErrorProto.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setBmsChargingErrorProto(bmsChargingErrorProto)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    public static String getErrorDescriptions(int code) {
        //<00>：=正常；<01>：=超时；<10>： =不可信状态
        if (code == 0) {
            return "正常";
        } else if (code == 1) {
            return "超时";
        } else if (code == 2) {
            return "不可信状态";
        }
        return "未知";
    }

}
