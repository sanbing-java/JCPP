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
import sanbing.jcpp.proto.gen.UplinkProto.BmsChargingInfoProto;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 充电过程BMS信息
 *
 * @author facai
 */
@Slf4j
@ProtocolCmd(value = 0x25, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150BmsChargingInfoULCmd extends YunKuaiChongUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0充电过程BMS信息", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        // 1.交易流水号
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = BCDUtil.toString(tradeNoBytes);
        // 2.桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);
        // 3.枪号
        byte[] gunCodeBytes = new byte[1];
        byteBuf.readBytes(gunCodeBytes);
        String gunCode = BCDUtil.toString(gunCodeBytes);
        // 4.BMS最高单体动力蓄电池电压所在编号
        additionalInfo.put("BMS最高单体动力蓄电池电压所在编号", byteBuf.readByte());
        // 5.BMS最高动力蓄电池温度
        additionalInfo.put("BMS最高动力蓄电池温度", byteBuf.readByte());
        // 6.最高温度检测点编号
        additionalInfo.put("最高温度检测点编号", byteBuf.readByte());
        // 7.最低动力蓄电池温度
        additionalInfo.put("最低动力蓄电池温度", byteBuf.readByte());
        // 8.最低动力蓄电池温度检测点编号
        additionalInfo.put("最低动力蓄电池温度检测点编号", byteBuf.readByte());
        // 9.BMS单体动力蓄电池电压过高/过低 00:正常 01:过高 10:过低
        additionalInfo.put("BMS单体动力蓄电池电压过高/过低", stateToStr(byteBuf.readShortLE()));
        // 10.BMS整车动力蓄电池荷电状态SOC 过高/过低 00:正常 01:过高 10:过低
        additionalInfo.put("BMS整车动力蓄电池荷电状态SOC", stateToStr(byteBuf.readShortLE()));
        // 11.BMS动力蓄电池充电过电流 00:正常 01:过流 10:不可信状态
        additionalInfo.put("BMS动力蓄电池充电过电流", stateToStr1(byteBuf.readShortLE()));
        // 12.BMS动力蓄电池温度过高 00:正常 01:过流 10:不可信状态
        additionalInfo.put("BMS动力蓄电池温度过高", stateToStr1(byteBuf.readShortLE()));
        // 13.BMS动力蓄电池绝缘状态 00:正常 01:过流 10:不可信状态
        additionalInfo.put("BMS动力蓄电池绝缘状态", stateToStr1(byteBuf.readShortLE()));
        // 14.BMS动力蓄电池组输出连接器连接状态 00:正常 01:过流 10:不可信状态
        additionalInfo.put("BMS动力蓄电池组输出连接器连接状态", stateToStr1(byteBuf.readShortLE()));
        // 15.充电禁止 00:禁止 01:允许
        additionalInfo.put("BMS动力蓄电池组输出连接器连接状态", byteBuf.readShortLE() == 0 ? "禁止" : "允许");
        // 16.预留位
        byteBuf.skipBytes(2);

        BmsChargingInfoProto bmsCharingInfoProto = BmsChargingInfoProto.newBuilder()
                .setPileCode(pileCode)
                .setTradeNo(tradeNo)
                .setGunCode(gunCode)
                .setAdditionalInfo(additionalInfo.toString())
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(bmsCharingInfoProto.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setBmsChargingInfoProto(bmsCharingInfoProto)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    private String stateToStr(short state) {
        return switch (state) {
            case 0 -> "正常";
            case 1 -> "过高";
            default -> "过低";
        };
    }

    private String stateToStr1(short state) {
        return switch (state) {
            case 0 -> "正常";
            case 1 -> "过高";
            default -> "不可信状态";
        };
    }
}
