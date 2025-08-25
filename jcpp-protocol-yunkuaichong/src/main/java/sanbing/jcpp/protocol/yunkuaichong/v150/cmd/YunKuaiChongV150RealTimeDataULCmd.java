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
import org.apache.commons.lang3.StringUtils;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.ChargingProgressProto;
import sanbing.jcpp.proto.gen.ProtocolProto.GunRunStatus;
import sanbing.jcpp.proto.gen.ProtocolProto.GunRunStatusProto;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0上传实时监测数据
 *
 * @author baigod
 */
@Slf4j
@YunKuaiChongCmd(value = 0x13, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150RealTimeDataULCmd extends YunKuaiChongUplinkCmdExe {

    // 故障说明列表
    private static final String[] faultDescriptions = {
            "急停按钮动作故障",          // Bit 1
            "无可用整流模块",            // Bit 2
            "出风口温度过高",            // Bit 3
            "交流防雷故障",              // Bit 4
            "交直流模块 DC20 通信中断",   // Bit 5
            "绝缘检测模块 FC08 通信中断", // Bit 6
            "电度表通信中断",            // Bit 7
            "读卡器通信中断",            // Bit 8
            "RC10 通信中断",             // Bit 9
            "风扇调速板故障",            // Bit 10
            "直流熔断器故障",            // Bit 11
            "高压接触器故障",            // Bit 12
            "门打开"                    // Bit 13
    };

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0上传实时监测数据", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        // 从Tracer总获取当前时间
        long ts = TracerContextUtil.getCurrentTracer().getTracerTs();

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

        // 4.状态 0x00：离线 0x01：故障 0x02：空闲 0x03：充电
        int gunStatus = byteBuf.readUnsignedByte();

        // 5.枪是否归位 0x00:否 0x01:是 0x02:未知
        int gunHoming = byteBuf.readUnsignedByte();
        additionalInfo.put("枪是否归位(0否1是)", gunHoming);

        // 6.是否插枪 0x00：否 0x01：是
        int gunInsert = byteBuf.readUnsignedByte();

        // 7.输出电压
        BigDecimal outputVoltage = reduceMagnification(byteBuf.readUnsignedShortLE(), 10);

        // 8.输出电流
        BigDecimal outputCurrent = reduceMagnification(byteBuf.readUnsignedShortLE(), 10);

        // 9.枪线温度
        short gunLineTemperature = byteBuf.readUnsignedByte();
        additionalInfo.put("枪线温度", gunLineTemperature);

        // 10.枪线编码
        long gunLineCode = byteBuf.readLongLE();
        additionalInfo.put("枪线编码", gunLineCode);

        // 11.soc
        int soc = byteBuf.readUnsignedByte();

        // 12.电池组最高温度
        short maxBatteryTemperature = byteBuf.readUnsignedByte();
        additionalInfo.put("电池组最高温度", maxBatteryTemperature);

        // 13.累计充电时间（分钟）
        int totalChargeTime = byteBuf.readUnsignedShortLE();

        // 14.剩余时间（分钟）
        int remainMin = byteBuf.readUnsignedShortLE();
        additionalInfo.put("剩余时间", remainMin);

        //15.充电度数（kWh)
        BigDecimal chargeEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000, 4);
        additionalInfo.put("充电度数", chargeEnergy);

        //16.计损充电度数（kWh)
        BigDecimal loseEnergy = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000, 4);
        additionalInfo.put("计损充电度数", loseEnergy);

        // 17.已充金额 （电费+服务费）*计损充电度数
        BigDecimal chargeAmount = reduceMagnification(byteBuf.readUnsignedIntLE(), 10000);

        // 18.硬件故障 测试发现需要使用小端计算bit, 然后对照故障表查询故障码
        byte[] warnCodeBytes = new byte[2];
        byteBuf.readBytes(warnCodeBytes);
        // 解析 14 个比特位
        List<String> faults = getFaultDescriptions(parseFaults(warnCodeBytes));

        // 抢状态
        GunRunStatus gunRunStatus = parseGunRunStatus(gunStatus, gunInsert, tradeNo);
        GunRunStatusProto.Builder gunRunStatusProtoBuilder = GunRunStatusProto.newBuilder()
                .setTs(ts)
                .setPileCode(pileCode)
                .setGunCode(gunCode)
                .setGunRunStatus(gunRunStatus)
                .addAllFaultMessages(faults)
                .setAdditionalInfo(additionalInfo.toString());

        // 转发到后端
        UplinkQueueMessage gunRunStatusMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setGunRunStatusProto(gunRunStatusProtoBuilder)
                .build();

        tcpSession.getForwarder().sendMessage(gunRunStatusMessage);

        if (StringUtils.isNotBlank(tradeNo)) {

            // 充电进度
            ChargingProgressProto.Builder chargingProgressProtoBuilder = ChargingProgressProto.newBuilder()
                    .setTs(ts)
                    .setPileCode(pileCode)
                    .setGunCode(gunCode)
                    .setTradeNo(tradeNo)
                    .setOutputVoltage(outputVoltage.toPlainString())
                    .setOutputCurrent(outputCurrent.toPlainString())
                    .setSoc(soc)
                    .setTotalChargingDurationMin(totalChargeTime)
                    .setTotalChargingEnergyKWh(chargeEnergy.toPlainString())
                    .setTotalChargingCostYuan(chargeAmount.toPlainString())
                    .setAdditionalInfo(additionalInfo.toString());

            UplinkQueueMessage chargingProgressMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                    .setChargingProgressProto(chargingProgressProtoBuilder)
                    .build();

            tcpSession.getForwarder().sendMessage(chargingProgressMessage);
        }
    }

    /**
     * 解析枪运行状态
     */
    private static GunRunStatus parseGunRunStatus(int gunStatus, int gunInsert, String tradeNo) {
        GunRunStatus gunRunStatus = GunRunStatus.UNKNOWN;
        if (gunStatus == 0) {
            gunRunStatus = GunRunStatus.FAULT;
        } else if (gunStatus == 1) {
            gunRunStatus = GunRunStatus.FAULT;
        } else if (gunStatus == 2) {
            gunRunStatus = GunRunStatus.IDLE;
            if (gunInsert == 1) {
                gunRunStatus = GunRunStatus.INSERTED;
            }
        } else if (gunStatus == 3) {
            if (StringUtils.isBlank(tradeNo)) {
                gunRunStatus = GunRunStatus.INSERTED;
            } else {
                gunRunStatus = GunRunStatus.CHARGING;
            }
        }
        return gunRunStatus;
    }


    public static boolean[] parseFaults(byte[] bytes) {
        // 确保输入有效
        if (bytes.length != 2) {
            throw new IllegalArgumentException("输入 byte 数组长度不为 2");
        }

        // 创建一个布尔数组来存储故障状态
        boolean[] faults = new boolean[14];

        // 读取每个比特并设置到布尔数组中
        for (int i = 0; i < 14; i++) {
            // 计算对应的字节和比特位置
            int byteIndex = i / 8; // 字节索引
            int bitIndex = i % 8;  // 比特索引

            // 使用位运算检查该比特位
            faults[i] = ((bytes[byteIndex] >> bitIndex) & 1) == 1; // 如果为 1 则故障
        }

        return faults;
    }

    public static List<String> getFaultDescriptions(boolean[] faults) {
        List<String> faultList = new ArrayList<>();

        // 遍历布尔数组，筛选出有故障的说明
        for (int i = 0; i < faults.length; i++) {
            if (faults[i]) {
                faultList.add(faultDescriptions[i]);
            }
        }

        // 转换 List 为数组并返回
        return faultList;
    }

}