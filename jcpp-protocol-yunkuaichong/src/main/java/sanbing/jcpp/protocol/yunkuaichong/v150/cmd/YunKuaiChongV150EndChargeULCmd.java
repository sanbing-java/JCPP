/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;


import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V150;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V160;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V170;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.UplinkProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

/**
 * 云快充1.5.0 充电结束
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x19, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150EndChargeULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0充电结束", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        // 交易流水号
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = BCDUtil.toString(tradeNoBytes);

        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 枪号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        // 4 BMS 中止荷电状态
        additionalInfo.put("BMS 中止荷电状态 SOC", byteBuf.readByte());

        // 5 BMS 动力蓄电池单体最低电压
        additionalInfo.put("BMS 动力蓄电池单体最低电压", byteBuf.readShortLE());

        // 6 BMS 动力蓄电池单体最高电压
        additionalInfo.put("BMS 动力蓄电池单体最高电压", byteBuf.readShortLE());

        // 7 BMS 动力蓄电池最低温度
        additionalInfo.put("BMS 动力蓄电池最低温度", byteBuf.readByte());

        // 8 BMS 动力蓄电池最高温度
        additionalInfo.put("BMS 动力蓄电池最高温度", byteBuf.readByte());

        // 9 电桩累计充电时间
        additionalInfo.put("电桩累计充电时间", byteBuf.readShortLE());

        // 10 电桩输出能量
        additionalInfo.put("电桩输出能量", byteBuf.readShortLE());

        // 11 电桩充电机编号
        byte[] chargerCode = new byte[4];
        byteBuf.readBytes(chargerCode);
        additionalInfo.put("电桩充电机编号", BCDUtil.toString(chargerCode));

        // 转发到后端
        UplinkProto.UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setEndChargeProto(UplinkProto.EndChargeProto.newBuilder()
                        .setTradeNo(tradeNo)
                        .setPileCode(pileCode)
                        .setGunCode(gunCode)
                        .setAdditionalInfo(additionalInfo.toString())
                        .build())
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

}