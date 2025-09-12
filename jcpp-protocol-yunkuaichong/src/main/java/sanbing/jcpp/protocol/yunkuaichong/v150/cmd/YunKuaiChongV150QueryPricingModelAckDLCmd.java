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
import sanbing.jcpp.proto.gen.DownlinkProto.*;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static sanbing.jcpp.proto.gen.DownlinkProto.PricingModelFlag.*;
import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.QUERY_PRICING_ACK;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 计费模型请求应答
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x0A, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150QueryPricingModelAckDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0计费模型请求应答", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasQueryPricingResponse()) {
            return;
        }

        QueryPricingResponse queryPricingResponse = yunKuaiChongDwonlinkMessage.getMsg().getQueryPricingResponse();

        long pricingId = queryPricingResponse.getPricingId();
        String pileCode = queryPricingResponse.getPileCode();
        PricingModelProto pricingModel = queryPricingResponse.getPricingModel();
        // 适配新的protobuf结构：根据计费规则获取价格信息
        Map<Integer, FlagPriceProto> flagPriceMap = null;
        List<PeriodProto> periodList = null;
        
        if (pricingModel.hasPeakValleyPricing()) {
            // 峰谷计价：使用预定义的尖峰平谷时段
            PeakValleyPricingProto peakValleyPricing = pricingModel.getPeakValleyPricing();
            flagPriceMap = peakValleyPricing.getFlagPriceMap();
            periodList = peakValleyPricing.getPeriodList();
        } else {
            // 未知计费模式
            log.info("未知的计费模式，桩编号: {}, 计费ID: {}, 计费规则: {}", pileCode, pricingId, pricingModel.getRule());
            throw new IllegalArgumentException("未知的计费模式: " + pricingModel.getRule());
        }

        // 从上行报文中取出桩编号字节数组
        byte[] pileCodeBytes = encodePileCode(pileCode);

        // 创建ACK消息体7字节桩编号+2字节计费模型编号+4x4x2字节尖峰平谷电价和服务费+1字节计损比例+48字节时段标识
        ByteBuf queryPricingAckMsgBody = Unpooled.buffer(90);
        queryPricingAckMsgBody.writeBytes(pileCodeBytes);
        queryPricingAckMsgBody.writeBytes(encodePricingId(pricingId));

        // 4字节电价+4字节服务费
        BigDecimal accurate = new BigDecimal(1000);
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(TOP.ordinal()).getElec()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(TOP.ordinal()).getServ()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(PEAK.ordinal()).getElec()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(PEAK.ordinal()).getServ()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(FLAT.ordinal()).getElec()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(FLAT.ordinal()).getServ()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(VALLEY.ordinal()).getElec()).multiply(accurate).intValue());
        queryPricingAckMsgBody.writeIntLE(new BigDecimal(flagPriceMap.get(VALLEY.ordinal()).getServ()).multiply(accurate).intValue());

        // 计损比例
        queryPricingAckMsgBody.writeByte(0);

        // 48段半小时
        byte[] bytes = new byte[48];
        LocalTime currentTime = LocalTime.MIDNIGHT;
        for (int i = 0; i < 48; i++) {
            bytes[i] = getFlagForCurrentTime(periodList, currentTime);
            currentTime = currentTime.plusMinutes(30); // 每次时间增加30分钟
        }
        queryPricingAckMsgBody.writeBytes(bytes);

        encodeAndWriteFlush(QUERY_PRICING_ACK,
                queryPricingAckMsgBody,
                tcpSession);

    }
}