/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.v340.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.proto.gen.DownlinkProto;
import sanbing.jcpp.proto.gen.DownlinkProto.SetPricingRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengDownlinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengDwonlinkMessage;
import sanbing.jcpp.protocol.lvneng.mapping.LvnengDownlinkCmdConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.SET_PRICING;
import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 服务器设置 24 时电费计价策略信息
 */
@Slf4j
@ProtocolCmd(value = 1103, protocolNames = {V340})
public class LvnengV340SetPricingModelDLCmd extends LvnengDownlinkCmdExe {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    @Override
    public void execute(TcpSession tcpSession, LvnengDwonlinkMessage lvnengDwonlinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4服务器设置 24 时电费计价策略信息", tcpSession);

        if (!lvnengDwonlinkMessage.getMsg().hasSetPricingRequest()) {
            return;
        }

       SetPricingRequest setPricingRequest = lvnengDwonlinkMessage.getMsg().getSetPricingRequest();

        long pricingId = setPricingRequest.getPricingId();
        String pileCode = setPricingRequest.getPileCode();
        DownlinkProto.PricingModelProto pricingModel = setPricingRequest.getPricingModel();
        // 适配新的protobuf结构：根据计费规则获取价格信息
        List<DownlinkProto.TimePeriodItemProto> periodList = null;

        if (pricingModel.hasTimePeriodPricing()) {
            // 时段计价：使用运营商自定义时段计费
            DownlinkProto.TimePeriodPricingProto timePeriodPricing = pricingModel.getTimePeriodPricing();
            periodList = timePeriodPricing.getPeriodsList();


        } else {
            // 未知计费模式
            log.error("未知的计费模式，桩编号: {}, 计费ID: {}, 计费规则: {}", pileCode, pricingId, pricingModel.getRule());
            throw new IllegalArgumentException("未知的计费模式: " + pricingModel.getRule());
        }

        // 一个时间段（开始小时1+开始分钟1+结束小时1+结束分钟1+4电费+4服务费）*48个时间段
        ByteBuf setPricingAckMsgBody = Unpooled.buffer(576);
        for (DownlinkProto.TimePeriodItemProto x : periodList) {
            LocalTime startTime = dateFormat(x.getStartTime());
            setPricingAckMsgBody.writeByte(startTime.getHour());
            setPricingAckMsgBody.writeByte(startTime.getMinute());

            LocalTime endTime = dateFormat(x.getEndTime());
            setPricingAckMsgBody.writeByte(endTime.getHour());
            setPricingAckMsgBody.writeByte(endTime.getMinute());

            setPricingAckMsgBody.writeIntLE(buildPrice(x.getElecPrice()));
            setPricingAckMsgBody.writeIntLE(buildPrice(x.getServPrice()));
        }


        // 放进缓存后再下发
        tcpSession.getRequestCache().put(pileCode + LvnengDownlinkCmdConverter.getInstance().convertToCmd(SET_PRICING), pricingId);

        encodeAndWriteFlush(SET_PRICING,
                setPricingAckMsgBody,
                tcpSession);
    }

    protected static LocalTime dateFormat(String time) {
        return   LocalTime.parse(time, DATE_TIME_FORMATTER);
    }

    protected static int buildPrice(String price) {
        return  new BigDecimal(price).setScale(4, RoundingMode.HALF_UP).multiply(new BigDecimal(10000)).intValueExact();

    }
}
