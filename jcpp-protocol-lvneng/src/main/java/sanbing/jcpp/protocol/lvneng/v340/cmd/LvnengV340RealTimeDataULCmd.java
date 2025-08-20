/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.v340.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.proto.gen.ProtocolProto.GunRunStatus;
import sanbing.jcpp.proto.gen.ProtocolProto.GunRunStatusProto;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;
import sanbing.jcpp.protocol.lvneng.annotation.LvnengCmd;
import sanbing.jcpp.protocol.lvneng.enums.LvnengAlarmCodeEnum;
import sanbing.jcpp.protocol.lvneng.enums.LvnengPileStartTypeEnum;
import sanbing.jcpp.protocol.lvneng.enums.LvnengPileStatusEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static sanbing.jcpp.protocol.lvneng.enums.LvnengDownlinkCmdEnum.REAL_TIME_DATA_ACK;

/**
 * 绿能3.4 充电桩状态信息包上报
 */
@Slf4j
@LvnengCmd(109)
public class LvnengV340RealTimeDataULCmd extends LvnengUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4充电桩状态信息包上报请求", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        // 从Tracer总获取当前时间
        long ts = TracerContextUtil.getCurrentTracer().getTracerTs();

        //1预留
        byteBuf.skipBytes(2);
        //2预留
        byteBuf.skipBytes(2);

        //3充电桩编码
        byte[] pileCodeBytes = new byte[32];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = StringUtils.trim(new String(pileCodeBytes, StandardCharsets.US_ASCII));

        //4 充电枪数量
        byte gunsNum = byteBuf.readByte();
        additionalInfo.put("充电枪数量", gunsNum);

        //5 充电枪口
        int gunCode = byteBuf.readByte();
        additionalInfo.put("充电枪口", gunCode);

        //6 充电枪类型
        byte gunType = byteBuf.readByte();
        additionalInfo.put("充电枪类型", gunType);

        //7 工作状态
        byte pileStatus = byteBuf.readByte();
        String pileStatusCode = LvnengPileStatusEnum.getByCode(pileStatus);
        additionalInfo.put("工作状态", pileStatusCode);

        //8 当前 SOC %
        byte soc = byteBuf.readByte();
        additionalInfo.put("soc", soc);

        /** 9 告警码,0-无告警 非0参靠枚举类
         * @see LvnengAlarmCodeEnum
         */
        long alarmCode = byteBuf.readUnsignedInt();
        String alarmCodeDesc = alarmCode == 0L ? "" : LvnengAlarmCodeEnum.getByCode(alarmCode);
        additionalInfo.put("告警码", alarmCodeDesc);

        //10 车连接状态
        byte carLinkStatus = byteBuf.readByte();
        additionalInfo.put("车连接状态", getCarLinkDesc(carLinkStatus));

        //11 本次充电累计充电费用 精度0.01元
        BigDecimal totalAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 100, 2);
        additionalInfo.put("本次充电累计充电费用", totalAmount);
        //12 当前时间
        byte[] bytes = new byte[8];
        byteBuf.readBytes(bytes);
        LocalDateTime localDateTime = BCDUtil.bcdToDate(bytes);
        long instant = localDateTime!=null?localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli():0L;
        additionalInfo.put("当前时间", instant);

        //13 直流充电电压
        short DCV = byteBuf.readShortLE();
        additionalInfo.put("直流充电电压", DCV);

        //14 直流充电电流
        short DCC = byteBuf.readShortLE();
        additionalInfo.put("直流充电电压", DCC);

        //15 BMS 需求电压
        short bmsDCV = byteBuf.readShortLE();
        additionalInfo.put(" BMS 需求电压", bmsDCV);

        //16 BMS 需求电流
        short bmsDCC = byteBuf.readShortLE();
        additionalInfo.put(" BMS 需求电压", bmsDCC);

        //17 BMS 充电模式 1- 恒压 2- 恒流
        byte bmsChargingType = byteBuf.readByte();
        additionalInfo.put(" BMS 需求电压", getBmsChargingTypeDesc(bmsChargingType));

        //18 交流 A 相充电电压
        short aACV = byteBuf.readShortLE();
        additionalInfo.put("交流 A 相充电电压", aACV);

        //19 交流B相充电电压
        short bACV = byteBuf.readShortLE();
        additionalInfo.put("交流B相充电电压", bACV);

        //20 交流 C相充电电压
        short cACV = byteBuf.readShortLE();
        additionalInfo.put("交流 C相充电电压", cACV);

        //21 预留
        byteBuf.skipBytes(2);
        //22 预留
        byteBuf.skipBytes(2);
        //23 预留
        byteBuf.skipBytes(2);

        //24 剩余充电时间 min
        short remainingTimeMin = byteBuf.readShortLE();
        additionalInfo.put("剩余充电时间 min", remainingTimeMin);

        //25 充电时长 秒
        long totalChargingTime = byteBuf.readUnsignedIntLE();
        additionalInfo.put("充电时长 秒", totalChargingTime);

        //26 本次充电累计充电电量 精度：0.001lkWh
        BigDecimal totalChargingEnergyKWh = reduceMagnification(byteBuf.readUnsignedIntLE(), 1000, 3);
        additionalInfo.put("本次充电累计充电电量", totalChargingEnergyKWh.toString());

        //27 充电前电表读数 精度：0.001lkWh
        BigDecimal beforeNum = reduceMagnification(byteBuf.readLongLE(), 1000, 3);
        additionalInfo.put("充电前电表读数", beforeNum.toString());

        //28 当前电表读数 精度：0.001lkWh
        BigDecimal currentNum = reduceMagnification(byteBuf.readLongLE(), 1000, 3);
        additionalInfo.put("当前电表读数", currentNum.toString());

        //29 充电启动方式
        byte chargingType = byteBuf.readByte();
        String chargingTypeDesc = LvnengPileStartTypeEnum.getByCode(chargingType);
        additionalInfo.put("充电启动方式", chargingTypeDesc);

        //30 预留
        byteBuf.skipBytes(1);
        //31 预留
        byteBuf.skipBytes(4);
        //32 预留
        byteBuf.skipBytes(1);

        //33 云平台充电流水号/充电卡号
        byte[] tradeNoBytes = new byte[32];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = BCDUtil.toString(tradeNoBytes);
        additionalInfo.put("云平台充电流水号/充电卡号", tradeNo);

        //34 预留
        byteBuf.skipBytes(1);
        //35 预留
        byteBuf.skipBytes(8);

        //36 当前电费,这里是整型，要乘以 0.01 才能得到真实的金额，单位元
        BigDecimal currentFee = reduceMagnification(byteBuf.readUnsignedIntLE(), 100, 2);
        additionalInfo.put("当前电费", currentFee.toString());

        //37 当前服务费 这里是整型，要乘以 0.01 才能得到真实的金额，单位元
        BigDecimal currentServiceFee = reduceMagnification(byteBuf.readUnsignedIntLE(), 100, 2);
        additionalInfo.put("当前服务费", currentServiceFee.toString());

        //38 充电功率 分辨率：0.1kW
        BigDecimal chargingPower = reduceMagnification(byteBuf.readUnsignedIntLE(), 10, 1);
        additionalInfo.put("充电功率", chargingPower.toString());

        //39 预留
        byteBuf.skipBytes(4);
        //40 预留
        byteBuf.skipBytes(4);
        //41 预留
        byteBuf.skipBytes(4);

        //42 出风口温度 预留 偏移量-50, -50-200
        byteBuf.skipBytes(1);
        //43 环境温度 预留 偏移量-50, -50-200
        byteBuf.skipBytes(1);
        //44 充电枪温度 预留 偏移量-50, -50-200
        byteBuf.skipBytes(1);

        //45 车辆 VIN 码 直流桩有效，正常有效长度是字节17，18 位为‘\0’
        byte[] vinCodeByte = new byte[18];
        byteBuf.readBytes(vinCodeByte);
        String vinCode = new String(vinCodeByte, StandardCharsets.US_ASCII);
        additionalInfo.put("车辆 VIN 码", vinCode);

        //46 预留 1
        byteBuf.skipBytes(1);


        tcpSession.addPileCode(pileCode);

        // 注册前置会话
        ctx.getProtocolSessionRegistryProvider().register(tcpSession);

        // 抢状态
        GunRunStatus gunRunStatus = parseGunRunStatus(pileStatus);
        GunRunStatusProto gunRunStatusProto = GunRunStatusProto.newBuilder()
                .setTs(ts)
                .setPileCode(pileCode)
                .setGunCode(gunCode + "")
                .setGunRunStatus(gunRunStatus)
                .addAllFaultMessages(Stream.of(alarmCodeDesc).collect(Collectors.toList()))
                .setAdditionalInfo(additionalInfo.toString())
                .build();


        // 转发到后端
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(gunRunStatusProto.getPileCode(), tcpSession, lvnengUplinkMessage)
                .setGunRunStatusProto(gunRunStatusProto)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);

        if (StringUtils.isNotBlank(tradeNo)) {

            // 充电进度
            ProtocolProto.ChargingProgressProto.Builder chargingProgressProtoBuilder = ProtocolProto.ChargingProgressProto.newBuilder()
                    .setTs(ts)
                    .setPileCode(pileCode)
                    .setGunCode(gunCode + "")
                    .setTradeNo(tradeNo)
                    .setOutputVoltage(String.valueOf(DCV))
                    .setOutputCurrent(String.valueOf(DCC))
                    .setSoc(soc)
                    .setTotalChargingDurationMin((int)totalChargingTime)
                    .setTotalChargingEnergyKWh(totalChargingEnergyKWh.toPlainString())
                    .setTotalChargingCostYuan(totalAmount.toPlainString())
                    .setAdditionalInfo(additionalInfo.toString());

            UplinkQueueMessage chargingProgressMessage = uplinkMessageBuilder(pileCode, tcpSession, lvnengUplinkMessage)
                    .setChargingProgressProto(chargingProgressProtoBuilder)
                    .build();

            tcpSession.getForwarder().sendMessage(chargingProgressMessage);
        }

        //服务器应答充电桩状态信息包
        realTimeDataAck(tcpSession, gunCode);
    }

    private void realTimeDataAck(TcpSession tcpSession, int gunCode) {
        ByteBuf pingAckMsgBody = Unpooled.buffer(5);
        pingAckMsgBody.writeShortLE(0);
        pingAckMsgBody.writeShortLE(0);
        pingAckMsgBody.writeByte(gunCode);

        encodeAndWriteFlush(REAL_TIME_DATA_ACK,
                pingAckMsgBody,
                tcpSession);
    }

    protected static BigDecimal reduceMagnification(long value, int magnification, int scale) {
        return new BigDecimal(value).divide(new BigDecimal(magnification), scale, RoundingMode.HALF_UP);
    }


    protected static String getBmsChargingTypeDesc(int value) {
        return switch (value) {
            case 1 -> "恒压";
            case 2 -> "恒流";
            default -> "未知";
        };
    }

    protected static String getCarLinkDesc(int value) {
        // 目前只有 0 和 2 状态。
        return switch (value) {
            case 0 -> "断开";
            case 1 -> "半连接";
            case 2 -> "连接";
            default -> "未知";
        };
    }

    private static GunRunStatus parseGunRunStatus(int gunStatus) {
        GunRunStatus gunRunStatus = GunRunStatus.UNKNOWN;
        if (gunStatus == 0) {
            gunRunStatus = GunRunStatus.IDLE;
        } else if (gunStatus == 2) {
            gunRunStatus = GunRunStatus.CHARGING;
        } else if (gunStatus == 6) {
            gunRunStatus = GunRunStatus.FAULT;
        }
        return gunRunStatus;
    }
}
