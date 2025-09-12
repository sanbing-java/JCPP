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
import sanbing.jcpp.proto.gen.UplinkProto.BmsParamConfigReportProto;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.math.BigDecimal;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充协议上行命令处理类 - 参数配置帧解析 (V1.5.0版本)
 * 命令码：0x17 (参数配置帧上行命令)
 */
@Slf4j
@ProtocolCmd(value = 0x17, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150BmsParamConfigReportULCmd extends YunKuaiChongUplinkCmdExe {
    // 电流值偏移量常量（单位：安培）
    private static final BigDecimal CURRENT_OFFSET = new BigDecimal("-400.0");
    // 温度值偏移量常量（单位：摄氏度）
    private static final BigDecimal TEMP_OFFSET = new BigDecimal("-50.0");

    /**
     * 执行命令解析
     * @param tcpSession TCP会话对象
     * @param yunKuaiChongUplinkMessage  上行消息对象
     * @param ctx        协议上下文
     */
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0充电桩参数配置帧请求", tcpSession);
        // 将消息体包装为ByteBuf以便读取
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());
        /* 按协议顺序解析消息体 */

        // 1. 交易流水号：16字节BCD编码字符串
        String tradeNo = readBcdString(byteBuf, 16);

        // 2. 桩编号：7字节BCD编码字符串
        String pileCode = readBcdString(byteBuf, 7);

        // 3. 枪号：1字节BCD编码字符串
        String gunCode = readBcdString(byteBuf, 1);

        // 4. BMS单体最高允许充电电压：2字节无符号整数，单位0.01V
        BigDecimal maxSingleCellVoltage = readVoltage(byteBuf, 100);

        // 5. BMS最高允许充电电流：2字节无符号整数，单位0.1A（需加-400A偏移量）
        BigDecimal maxChargeCurrent = readCurrent(byteBuf);

        // 6. BMS动力蓄电池标称总能量：2字节无符号整数，单位0.1kWh
        BigDecimal ratedEnergy = readEnergy(byteBuf); // 单位0.1kWh

        // 7. BMS最高允许充电总电压：2字节无符号整数，单位0.1V
        BigDecimal maxTotalChargeVoltage = readVoltage(byteBuf, 10);

        // 8. BMS最高允许温度：1字节无符号整数，单位1℃（需加-50℃偏移量）
        BigDecimal maxTemperature = readTemperature(byteBuf);

        // 9. BMS荷电状态SOC：2字节无符号整数，单位0.1%
        BigDecimal soc = readPercentage(byteBuf);

        // 10. BMS当前电池电压：2字节无符号整数，单位0.1V
        BigDecimal currentBatteryVoltage = readVoltage(byteBuf, 10);

        // 11. 电桩最高输出电压：2字节无符号整数，单位0.1V
        BigDecimal pileMaxOutputVoltage = readVoltage(byteBuf, 10);

        // 12. 电桩最低输出电压：2字节无符号整数，单位0.1V
        BigDecimal pileMinOutputVoltage = readVoltage(byteBuf, 10);

        // 13. 电桩最大输出电流：2字节无符号整数，单位0.1A（需加-400A偏移量）
        BigDecimal pileMaxOutputCurrent = readCurrent(byteBuf);

        // 14. 电桩最小输出电流：2字节无符号整数，单位0.1A（需加-400A偏移量）
        BigDecimal pileMinOutputCurrent = readCurrent(byteBuf);

        // 转发到后端
        BmsParamConfigReportProto bmsParamConfigReportProto = BmsParamConfigReportProto.newBuilder()
                .setPileCode(pileCode)
                .setTradeNo(tradeNo)
                .setGunCode(gunCode)
                .setMaxSingleCellVoltage(maxSingleCellVoltage.toPlainString())
                .setMaxChargeCurrent(maxChargeCurrent.toPlainString())
                .setRatedEnergy(ratedEnergy.toPlainString())
                .setMaxTotalChargeVoltage(maxTotalChargeVoltage.toPlainString())
                .setMaxTemperature(maxTemperature.toPlainString())
                .setSoc(soc.toPlainString())
                .setCurrentBatteryVoltage(currentBatteryVoltage.toPlainString())
                .setPileMaxOutputVoltage(pileMaxOutputVoltage.toPlainString())
                .setPileMinOutputVoltage(pileMinOutputVoltage.toPlainString())
                .setPileMaxOutputCurrent(pileMaxOutputCurrent.toPlainString())
                .setPileMinOutputCurrent(pileMinOutputCurrent.toPlainString())
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(bmsParamConfigReportProto.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setBmsParamConfigReportProto(bmsParamConfigReportProto)
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
    /**
     * 读取电压值（返回BigDecimal）
     * @param buf 字节缓冲区
     * @param magnification 放大倍数（如100表示原始值需除以100）
     * @return 电压值（BigDecimal）
     */
    public BigDecimal readVoltage(ByteBuf buf, int magnification) {
        long value = buf.readUnsignedShortLE();
        return reduceMagnification(value, magnification, 4);
    }
    /**
     * 读取电流值（返回BigDecimal）
     * @param buf 字节缓冲区
     * @return 电流值（BigDecimal），已应用偏移量
     */
    public BigDecimal readCurrent(ByteBuf buf) {
        long value = buf.readUnsignedShortLE();
        BigDecimal current = reduceMagnification(value, 10, 4); // 0.1倍率
        return current.add(CURRENT_OFFSET); // 应用电流偏移
    }

    /**
     * 读取能量值（返回BigDecimal）
     * @param buf 字节缓冲区
     * @return 能量值（BigDecimal）
     */
    private BigDecimal readEnergy(ByteBuf buf) {
        long value = buf.readUnsignedShortLE();
        return reduceMagnification(value, 10, 4); // 0.1倍率
    }

    /**
     * 读取温度值（返回BigDecimal）
     * @param buf 字节缓冲区
     * @return 温度值（BigDecimal），已应用偏移量
     */
    public BigDecimal readTemperature(ByteBuf buf) {
        long value = buf.readUnsignedByte();
        BigDecimal temp = reduceMagnification(value, 1, 4); // 原始值
        return temp.add(TEMP_OFFSET); // 应用温度偏移
    }

    /**
     * 读取百分比值（返回BigDecimal）
     * @param buf 字节缓冲区
     * @return 百分比值（BigDecimal）
     */
    public BigDecimal readPercentage(ByteBuf buf) {
        long value = buf.readUnsignedShortLE();
        return reduceMagnification(value, 10, 4); // 0.1倍率
    }
}