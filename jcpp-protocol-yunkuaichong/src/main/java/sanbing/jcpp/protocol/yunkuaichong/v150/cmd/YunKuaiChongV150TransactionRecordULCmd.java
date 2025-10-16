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
import sanbing.jcpp.infrastructure.util.codec.CP56Time2aUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.UplinkProto.*;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V150;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V160;

/**
 * 云快充1.5.0 交易记录
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x3B, protocolNames = {V150, V160})
public class YunKuaiChongV150TransactionRecordULCmd extends YunKuaiChongUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0交易记录", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        // 1.交易流水号
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = decodeTradeNo(tradeNoBytes);

        // 2.桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 3.抢号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        // 4.开始时间
        byte[] startTimeBytes = new byte[7];
        byteBuf.readBytes(startTimeBytes);
        Instant startTime = CP56Time2aUtil.decode(startTimeBytes).atZone(ZoneId.systemDefault()).toInstant();

        // 5.结束时间
        byte[] endTimeBytes = new byte[7];
        byteBuf.readBytes(endTimeBytes);
        Instant endTime = CP56Time2aUtil.decode(endTimeBytes).atZone(ZoneId.systemDefault()).toInstant();

        // 6.尖单价
        BigDecimal topPrice = reduceMagnification(byteBuf.readUnsignedIntLE(), 100000);
        additionalInfo.put("尖单价", topPrice);
        // 7. 尖电量
        BigDecimal topEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        // 8.计损尖电量
        BigDecimal topLoseEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        additionalInfo.put("计损尖电量", topLoseEnergy);
        // 9.尖金额
        BigDecimal topAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);

        // 10.峰单价
        BigDecimal peakPrice = reduceMagnification(byteBuf.readUnsignedIntLE(), 100000);
        additionalInfo.put("峰单价", peakPrice);
        // 11. 峰电量
        BigDecimal peakEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        // 12.计损峰电量
        BigDecimal peakLoseEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        additionalInfo.put("计损峰电量", peakLoseEnergy);
        // 13.峰金额
        BigDecimal peakAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);

        // 14.平单价
        BigDecimal flatPrice = reduceMagnification(byteBuf.readUnsignedIntLE(), 100000);
        additionalInfo.put("平单价", flatPrice);
        // 15. 平电量
        BigDecimal flatEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        // 16.计损平电量
        BigDecimal flatLoseEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        additionalInfo.put("计损平电量", flatLoseEnergy);
        // 17.平金额
        BigDecimal flatAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);

        // 18.谷单价
        BigDecimal valleyPrice = reduceMagnification(byteBuf.readUnsignedIntLE(), 100000);
        additionalInfo.put("谷单价", valleyPrice);
        // 19. 谷电量
        BigDecimal valleyEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        // 20.计损谷电量
        BigDecimal valleyLoseEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);
        additionalInfo.put("计损谷电量", valleyLoseEnergy);
        // 21.谷金额
        BigDecimal valleyAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);

        // 22.电表总起值
        byte[] meterStartValueBytes = new byte[5];
        byteBuf.readBytes(meterStartValueBytes);
        BigDecimal startMeterValue = reduceMagnification(readLongLE5Byte(meterStartValueBytes), 10000, 4);
        additionalInfo.put("电表总起值", startMeterValue);

        // 23.电表总止值
        byte[] meterEndValueBytes = new byte[5];
        byteBuf.readBytes(meterEndValueBytes);
        BigDecimal endMeterValue = reduceMagnification(readLongLE5Byte(meterEndValueBytes), 10000, 4);
        additionalInfo.put("电表总止值", endMeterValue);

        // 24.总电量
        BigDecimal totalEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000, 4);
        // 25.计损总电量
        BigDecimal totalLoseEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000, 4);
        additionalInfo.put("计损总电量", totalLoseEnergy);
        // 26 .消费金额
        BigDecimal totalAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);

        // 27.电动汽车唯一标识
        byte[] carVINBytes = new byte[17];
        byteBuf.readBytes(carVINBytes);
        String bmsVinCode = new String(carVINBytes, StandardCharsets.US_ASCII);
        additionalInfo.put("电动汽车唯一标识", bmsVinCode);

        // 28.交易标识 0x01：app 启动0x02：卡启动 0x04：离线卡启动 0x05: vin 码启动充电
        byte tradeFlag = byteBuf.readByte();
        additionalInfo.put("交易标识", mapStartFlag(tradeFlag));

        // 29.交易日期、时间
        byte[] tradeTimeBytes = new byte[7];
        byteBuf.readBytes(tradeTimeBytes);
        Instant tradeTime = CP56Time2aUtil.decode(tradeTimeBytes).atZone(ZoneId.systemDefault()).toInstant();

        // 30.停止原因
        byte stopReasonByte = byteBuf.readByte();
        String stopReason = mapStopReason(stopReasonByte);

        //31 物理卡号 8字节long值（小端序）
        long physicalCardNoLong = byteBuf.readLongLE();
        String cardNo = String.valueOf(physicalCardNoLong);
        additionalInfo.put("物理卡号", cardNo);

        // 构建峰谷电量明细
        PeakValleyDetail peakValleyDetail = PeakValleyDetail.newBuilder()
                .setTopEnergyKWh(topEnergy.toPlainString())
                .setTopAmountYuan(topAmount.toPlainString())
                .setPeakEnergyKWh(peakEnergy.toPlainString())
                .setPeakAmountYuan(peakAmount.toPlainString())
                .setFlatEnergyKWh(flatEnergy.toPlainString())
                .setFlatAmountYuan(flatAmount.toPlainString())
                .setValleyEnergyKWh(valleyEnergy.toPlainString())
                .setValleyAmountYuan(valleyAmount.toPlainString())
                .build();

        // 构建交易明细
        TransactionDetail transactionDetail = TransactionDetail.newBuilder()
                .setType(DetailType.PEAK_VALLEY)
                .setPeakValley(peakValleyDetail)
                .build();

        // 构建交易记录
        TransactionRecordRequest transactionRecordRequest = TransactionRecordRequest.newBuilder()
                .setPileCode(pileCode)
                .setGunNo(gunCode)
                .setTradeNo(tradeNo)
                .setStartTs(startTime.toEpochMilli())
                .setEndTs(endTime.toEpochMilli())
                .setTotalEnergyKWh(totalEnergy.toPlainString())
                .setTotalAmountYuan(totalAmount.toPlainString())
                .setTradeTs(tradeTime.toEpochMilli())
                .setStopReason(stopReason)
                .setDetail(transactionDetail)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        // 转发到后端
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setTransactionRecordRequest(transactionRecordRequest)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    public static long readLongLE5Byte(byte[] bytes) {
        // 确保字节数组的长度至少为 5
        if (bytes.length < 5) {
            throw new IllegalArgumentException("字节数组长度必须至少为5字节");
        }

        // 使用小端字节序读取 5 字节数字
        int byte1 = bytes[0] & 0xFF;
        int byte2 = bytes[1] & 0xFF;
        int byte3 = bytes[2] & 0xFF;
        int byte4 = bytes[3] & 0xFF;
        int byte5 = bytes[4] & 0xFF;

        // 将读取的字节合并成一个 long 值
        return ((long) byte1) |
                ((long) byte2 << 8) |
                ((long) byte3 << 16) |
                ((long) byte4 << 24) |
                ((long) byte5 << 32);
    }

    public static String mapStartFlag(byte startFlag) {
        return switch (startFlag) {
            case 0x01 -> "app 启动";
            case 0x02 -> "卡启动";
            case 0x04 -> "离线卡启动";
            case 0x05 -> "vin 码启动充电";
            default -> "未知启动方式";
        };
    }

    public static String mapStopReason(byte stopReasonCode) {
        return switch (stopReasonCode) {
            case (byte) 0x40 -> "结束充电，APP 远程停止";
            case (byte) 0x41 -> "结束充电，SOC 达到 100%";
            case (byte) 0x42 -> "结束充电，充电电量满足设定条件";
            case (byte) 0x43 -> "结束充电，充电金额满足设定条件";
            case (byte) 0x44 -> "结束充电，充电时间满足设定条件";
            case (byte) 0x45 -> "结束充电，手动停止充电";
            case (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49 -> "结束充电，其他方式（预留）";
            case (byte) 0x4A -> "充电启动失败，充电桩控制系统故障(需要重启或自动恢复)";
            case (byte) 0x4B -> "充电启动失败，控制导引断开";
            case (byte) 0x4C -> "充电启动失败，断路器跳位";
            case (byte) 0x4D -> "充电启动失败，电表通信中断";
            case (byte) 0x4E -> "充电启动失败，余额不足";
            case (byte) 0x4F -> "充电启动失败，充电模块故障";
            case (byte) 0x50 -> "充电启动失败，急停开入";
            case (byte) 0x51 -> "充电启动失败，防雷器异常";
            case (byte) 0x52 -> "充电启动失败，BMS 未就绪";
            case (byte) 0x53 -> "充电启动失败，温度异常";
            case (byte) 0x54 -> "充电启动失败，电池反接故障";
            case (byte) 0x55 -> "充电启动失败，电子锁异常";
            case (byte) 0x56 -> "充电启动失败，合闸失败";
            case (byte) 0x57 -> "充电启动失败，绝缘异常";
            case (byte) 0x58 -> "充电启动失败，预留";
            case (byte) 0x59 -> "充电启动失败，接收 BMS 握手报文 BHM 超时";
            case (byte) 0x5A -> "充电启动失败，接收 BMS 和车辆的辨识报文超时 BRM";
            case (byte) 0x5B -> "充电启动失败，接收电池充电参数报文超时 BCP";
            case (byte) 0x5C -> "充电启动失败，接收 BMS 完成充电准备报文超时 BRO AA";
            case (byte) 0x5D -> "充电启动失败，接收电池充电总状态报文超时 BCS";
            case (byte) 0x5E -> "充电启动失败，接收电池充电要求报文超时 BCL";
            case (byte) 0x5F -> "充电启动失败，接收电池状态信息报文超时 BSM";
            case (byte) 0x60 -> "充电启动失败，GB2015 电池在 BHM 阶段有电压不允许充电";
            case (byte) 0x61 -> "充电启动失败，GB2015 辨识阶段在 BRO_AA 时候电池实际电压与 BCP 报文电池电压差距大于 5%";
            case (byte) 0x62 -> "充电启动失败，B2015 充电机在预充电阶段从 BRO_AA 变成BRO_00 状态";
            case (byte) 0x63 -> "充电启动失败，接收主机配置报文超时";
            case (byte) 0x64 -> "充电启动失败，充电机未准备就绪,我们没有回 CRO AA，对应老国标";
            case (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69 -> "充电启动失败，其他原因（预留）";
            case (byte) 0x6A -> "充电异常中止，系统闭锁";
            case (byte) 0x6B -> "充电异常中止，导引断开";
            case (byte) 0x6C -> "充电异常中止，断路器跳位";
            case (byte) 0x6D -> "充电异常中止，电表通信中断";
            case (byte) 0x6E -> "充电异常中止，余额不足";
            case (byte) 0x6F -> "充电异常中止，交流保护动作";
            case (byte) 0x70 -> "充电异常中止，直流保护动作";
            case (byte) 0x71 -> "充电异常中止，充电模块故障";
            case (byte) 0x72 -> "充电异常中止，急停开入";
            case (byte) 0x73 -> "充电异常中止，防雷器异常";
            case (byte) 0x74 -> "充电异常中止，温度异常";
            case (byte) 0x75 -> "充电异常中止，输出异常";
            case (byte) 0x76 -> "充电异常中止，充电无流";
            case (byte) 0x77 -> "充电异常中止，电子锁异常";
            case (byte) 0x78 -> "充电异常中止，预留";
            case (byte) 0x79 -> "充电异常中止，总充电电压异常";
            case (byte) 0x7A -> "充电异常中止，总充电电流异常";
            case (byte) 0x7B -> "充电异常中止，单体充电电压异常";
            case (byte) 0x7C -> "充电异常中止，电池组过温";
            case (byte) 0x7D -> "充电异常中止，最高单体充电电压异常";
            case (byte) 0x7E -> "充电异常中止，最高电池组过温";
            case (byte) 0x7F -> "充电异常中止，BMV 单体充电电压异常";
            case (byte) 0x80 -> "充电异常中止，BMT 电池组过温";
            case (byte) 0x81 -> "充电异常中止，电池状态异常停止充电";
            case (byte) 0x82 -> "充电异常中止，车辆发报文禁止充电";
            case (byte) 0x83 -> "充电异常中止，充电桩断电";
            case (byte) 0x84 -> "充电异常中止，接收电池充电总状态报文超时";
            case (byte) 0x85 -> "充电异常中止，接收电池充电要求报文超时";
            case (byte) 0x86 -> "充电异常中止，接收电池状态信息报文超时";
            case (byte) 0x87 -> "充电异常中止，接收 BMS 中止充电报文超时";
            case (byte) 0x88 -> "充电异常中止，接收 BMS 充电统计报文超时";
            case (byte) 0x89 -> "充电异常中止，接收对侧 CCS 报文超时";
            case (byte) 0x8A, (byte) 0x8B, (byte) 0x8C, (byte) 0x8D, (byte) 0x8E, (byte) 0x8F ->
                    "充电异常中止，其他原因（预留）";
            case (byte) 0x90 -> "未知原因停止";
            default -> "无效的错误码";
        };
    }

}