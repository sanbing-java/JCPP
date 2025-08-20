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
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.proto.gen.ProtocolProto.TimePeriodDetail;
import sanbing.jcpp.proto.gen.ProtocolProto.TimePeriodDetail.PeriodItem;
import sanbing.jcpp.proto.gen.ProtocolProto.TransactionDetail;
import sanbing.jcpp.proto.gen.ProtocolProto.TransactionRecordRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;
import sanbing.jcpp.protocol.lvneng.annotation.LvnengCmd;
import sanbing.jcpp.protocol.lvneng.enums.LvnengPileFinishReasonEnum;
import sanbing.jcpp.protocol.lvneng.enums.LvnengPileStartTypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 绿能3.4 充电桩上报充电订单
 */
@Slf4j
@LvnengCmd(203)
public class LvnengV340TransactionRecordULCmd extends LvnengUplinkCmdExe {


    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4充电桩上报充电订单请求", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        //1预留
        byteBuf.skipBytes(2);
        //2预留
        byteBuf.skipBytes(2);

        //3充电桩编码
        byte[] pileCodeBytes = new byte[32];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = StringUtils.trim(new String(pileCodeBytes, StandardCharsets.US_ASCII));

        //4 充电枪位置类型
        byte gunType = byteBuf.readByte();
        additionalInfo.put("充电枪位置类型", gunType);

        //5 充电枪口
        int gunCode = byteBuf.readByte();
        additionalInfo.put("充电枪口", gunCode);

        //6 卡号
        byte[] cardCodeBytes = new byte[32];
        byteBuf.readBytes(cardCodeBytes);
        String cardCode = StringUtils.trim(new String(cardCodeBytes, StandardCharsets.US_ASCII));
        additionalInfo.put("卡号", cardCode);

        //7 充电开始时间
        byte[] chargingStartTimeBytes = new byte[8];
        byteBuf.readBytes(chargingStartTimeBytes);
        LocalDateTime start = BCDUtil.bcdToDate(chargingStartTimeBytes);
        long startTs = start != null ? start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;

        //8 充电结束时间
        byte[] chargingEndTimeBytes = new byte[8];
        byteBuf.readBytes(chargingEndTimeBytes);
        LocalDateTime endTime = BCDUtil.bcdToDate(chargingEndTimeBytes);
        long endTs = endTime != null ? endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;

        //9 充电时间长度 单位秒
        long chargingTotalTime = byteBuf.readUnsignedIntLE();
        additionalInfo.put("充电时间长度 单位秒", chargingTotalTime);

        //10 开始 SOC
        byte startSoc = byteBuf.readByte();
        additionalInfo.put("开始 SOC", startSoc);

        //11 结束 SOC
        byte endSoc = byteBuf.readByte();
        additionalInfo.put("结束 SOC", endSoc);

        //12 充电结束原因
        long finishReasonCode = byteBuf.readUnsignedIntLE();
        String finishReason = LvnengPileFinishReasonEnum.getByCode(finishReasonCode);

        //13 本次充电电量 精度：0.001kWh
        BigDecimal totalEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 1000, 3);

        //14 充电前电表读数 精度：0.001kWh
        BigDecimal chargingBeforeNum = reduceMagnification(byteBuf.readLongLE(), 1000, 3);
        additionalInfo.put("充电前电表读数", chargingBeforeNum);

        //15 充电后电表读数 精度：0.001kWh
        BigDecimal chargingAfterNum = reduceMagnification(byteBuf.readLongLE(), 1000, 3);
        additionalInfo.put("充电后电表读数", chargingAfterNum);

        //16 本次充电金额 0.01 元 ，电费和服务费之和。
        BigDecimal totalAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 100, 2);

        //17 内部索引号 4 字节有符号整形，每一条充电记录 都唯一编号，用于充电机内部做唯一标志
        int index = byteBuf.readIntLE();
        additionalInfo.put("内部索引号", index);
        additionalInfo.put("index", index);

        //18 预留
        byteBuf.skipBytes(4);
        //19 预留
        byteBuf.skipBytes(4);
        //20 预留
        byteBuf.skipBytes(4);
        //21 预留
        byteBuf.skipBytes(1);
        //22 预留
        byteBuf.skipBytes(1);
        //23 预留
        byteBuf.skipBytes(4);

        //24 车辆 VIN
        byte[] cardVinBytes = new byte[17];
        byteBuf.readBytes(cardVinBytes);
        String cardVin = StringUtils.trim(new String(cardVinBytes, StandardCharsets.US_ASCII));
        additionalInfo.put("车辆 VIN", cardVin);

        //25 车牌号
        byte[] cardNumBytes = new byte[8];
        byteBuf.readBytes(cardNumBytes);
        String cardNum = StringUtils.trim(new String(cardNumBytes, StandardCharsets.US_ASCII));
        additionalInfo.put("车牌号", cardNum);

        // 24小时，48个时间段，开始时间段[00:00:00〜00:30:00]，最后时间段[23:30:00-00:00:00] 0.001kwh
        LocalTime currentTime = LocalTime.MIDNIGHT;
        List<PeriodItem> list = new ArrayList<>();
        for (int i = 1; i < 49; i++) {
            BigDecimal chargingCapacity = reduceMagnification(byteBuf.readUnsignedIntLE(), 1000, 3);
            PeriodItem timePeriodDetail = PeriodItem.newBuilder()
                    .setPeriodNo(i)
                    .setStartTime(dateFormat(currentTime))
                    .setEndTime(dateFormat(currentTime.plusMinutes(30)))
                    .setEnergyKWh(chargingCapacity.toString())
                    .build();
            list.add(timePeriodDetail);
        }

        //74 启动方式
        byte startType = byteBuf.readByte();
        String startTypeCode = LvnengPileStartTypeEnum.getByCode(startType);
        additionalInfo.put("启动方式", startTypeCode);

        //75 充电流水号
        byte[] tradeNoBytes = new byte[32];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = StringUtils.trim(new String(tradeNoBytes, StandardCharsets.US_ASCII));

        //76 充电服务费
        BigDecimal serviceFee = reduceMagnification(byteBuf.readUnsignedIntLE(), 100, 2);
        additionalInfo.put("充电服务费", serviceFee.toString());

        tcpSession.addPileCode(pileCode);


        //充电明细
        TimePeriodDetail timePeriodDetail = TimePeriodDetail.newBuilder()
                .addAllPeriods(list)
                .build();
        //订单明细
        TransactionDetail transactionDetail = TransactionDetail.newBuilder()
                .setType(ProtocolProto.DetailType.TIME_PERIOD)
                .setTimePeriod(timePeriodDetail)
                .build();

        // 注册前置会话
        ctx.getProtocolSessionRegistryProvider().register(tcpSession);

        // 转发到后端
        TransactionRecordRequest transactionRecord = TransactionRecordRequest.newBuilder()
                .setPileCode(pileCode)
                .setGunCode(gunCode + "")
                .setTradeNo(tradeNo)
                .setStartTs(startTs)
                .setEndTs(endTs)
                .setTotalEnergyKWh(totalEnergy.toString())
                .setTotalAmountYuan(totalAmount.toString())
                .setStopReason(finishReason)
                .setDetail(transactionDetail)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(transactionRecord.getPileCode(), tcpSession, lvnengUplinkMessage)
                .setTransactionRecordRequest(transactionRecord)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);


    }


    protected static BigDecimal reduceMagnification(long value, int magnification, int scale) {
        return new BigDecimal(value).divide(new BigDecimal(magnification), scale, RoundingMode.HALF_UP);
    }

    protected static String dateFormat(LocalTime time) {
        return time.format(DATE_TIME_FORMATTER);
    }


}
