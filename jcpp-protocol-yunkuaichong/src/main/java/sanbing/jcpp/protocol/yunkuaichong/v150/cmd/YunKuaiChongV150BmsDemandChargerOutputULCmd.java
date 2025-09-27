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
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.UplinkProto.BmsDemandChargerOutputProto;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.math.BigDecimal;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 充电过程BMS需求与充电机输出
 */
@Slf4j
@ProtocolCmd(value = 0x23, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150BmsDemandChargerOutputULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0充电过程BMS需求与充电机输出", tcpSession);
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

        // 3.枪号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        // 4.BMS电压需求 0.1 V/位， 0 V 偏移量
        additionalInfo.put("BMS电压需求", reduceMagnification(byteBuf.readUnsignedShortLE(), 10));

        // 5.BMS电流需求 0.1 A/位， -400 A 偏移量
        additionalInfo.put("BMS电流需求", reduceMagnification(byteBuf.readUnsignedShortLE(), 10).subtract(new BigDecimal("400")));

        // 6.BMS充电模式 0x01：恒压充电； 0x02：恒流充电
        additionalInfo.put("BMS充电模式", byteBuf.readByte() == 0x01 ? "恒压充电" : "恒流充电");

        // 7.BMS充电电压测量值 0.1 V/位， 0 V 偏移量
        additionalInfo.put("BMS充电电压测量值", reduceMagnification(byteBuf.readUnsignedShortLE(), 10));

        // 8.BMS充电电流测量值 0.1 A/位， -400 A 偏移量
        additionalInfo.put("BMS充电电流测量值", reduceMagnification(byteBuf.readUnsignedShortLE(), 10).subtract(new BigDecimal("400")));

        // 9.BMS最高单体动力蓄电池电压及组号
        int i = byteBuf.readUnsignedShortLE();
        // 1-12 位：最高单体动力蓄电池电压，数据分辨率： 0.01V/位，0V偏移量；数据范围：0~24V
        additionalInfo.put("BMS最高单体动力蓄电池电压", reduceMagnification(i & 0x0FFF, 100));
        // 13-16 位： 最高单体动力蓄电池电压所在组号，数据分辨率： 1/位， 0 偏移量；数据范围：0~15
        additionalInfo.put("BMS最高单体动力蓄电池电压所在组号", (i >> 12) & 0x0F);

        // 10.BMS当前荷电状态 SOC（ %）
        additionalInfo.put("BMS当前荷电状态SOC", byteBuf.readByte());

        // 11.BMS 估算剩余充电时间
        additionalInfo.put("BMS估算剩余充电时间", byteBuf.readUnsignedShortLE());

        // 12.电桩电压输出值
        additionalInfo.put("电桩电压输出值", reduceMagnification(byteBuf.readUnsignedShortLE(), 10));

        // 13.电桩电流输出值
        additionalInfo.put("电桩电流输出值", reduceMagnification(byteBuf.readUnsignedShortLE(), 10).subtract(new BigDecimal("400")));

        // 14.累计充电时间
        additionalInfo.put("累计充电时间", byteBuf.readUnsignedShortLE());

        BmsDemandChargerOutputProto proto = BmsDemandChargerOutputProto.newBuilder()
                .setPileCode(pileCode)
                .setGunNo(gunCode)
                .setTradeNo(tradeNo)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        // 转发到后端
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setBmsDemandChargerOutputProto(proto)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }
}
