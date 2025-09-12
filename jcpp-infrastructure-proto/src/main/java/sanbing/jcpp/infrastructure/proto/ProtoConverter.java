/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.proto;


import sanbing.jcpp.infrastructure.proto.model.PricingModel;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.FlagPrice;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.Period;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.TimePeriodItem;
import sanbing.jcpp.infrastructure.util.trace.Tracer;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.*;
import sanbing.jcpp.proto.gen.GrpcProto.TracerProto;

import java.util.Map;

/**
 * @author 九筒
 */
public class ProtoConverter {

    public static TracerProto toTracerProto() {
        Tracer currentTracer = TracerContextUtil.getCurrentTracer();
        return TracerProto.newBuilder()
                .setId(currentTracer.getTraceId())
                .setOrigin(currentTracer.getOrigin())
                .setTs(currentTracer.getTracerTs())
                .build();
    }

    /**
     * 将业务层PricingModel转换为Protobuf格式
     * 根据计费规则自动选择对应的价格配置结构
     */
    public static PricingModelProto toPricingModel(PricingModel pricingModel) {
        PricingModelProto.Builder builder = PricingModelProto.newBuilder();

        // 设置基本信息
        builder.setType(PricingModelType.valueOf(pricingModel.getType().name()));
        builder.setRule(PricingModelRule.valueOf(pricingModel.getRule().name()));

        // 根据计费规则构建对应的价格配置
        switch (pricingModel.getRule()) {
            case STANDARD:
                // 标准计费：全天统一价格
                StandardPricingProto standardPricing = StandardPricingProto.newBuilder()
                        .setElecPrice(pricingModel.getStandardElec().toPlainString())
                        .setServPrice(pricingModel.getStandardServ().toPlainString())
                        .build();
                builder.setStandardPricing(standardPricing);
                break;

            case PEAK_VALLEY_PRICING:
                // 峰谷计价：按电网峰谷政策分时段
                PeakValleyPricingProto.Builder peakValleyBuilder = PeakValleyPricingProto.newBuilder();

                // 转换 flagPriceList
                if (pricingModel.getFlagPriceList() != null) {
                    for (Map.Entry<PricingModelFlag, FlagPrice> entry : pricingModel.getFlagPriceList().entrySet()) {
                        PricingModelFlag flag = entry.getKey();
                        FlagPrice flagPrice = entry.getValue();

                        FlagPriceProto flagPriceProto = FlagPriceProto.newBuilder()
                                .setFlag(PricingModelFlag.valueOf(flag.name()))
                                .setElec(flagPrice.getElec().toPlainString())
                                .setServ(flagPrice.getServ().toPlainString())
                                .build();

                        peakValleyBuilder.putFlagPrice(flag.ordinal(), flagPriceProto);
                    }
                }

                // 转换 PeriodsList
                if (pricingModel.getPeriodsList() != null) {
                    for (Period period : pricingModel.getPeriodsList()) {
                        PeriodProto periodProto = PeriodProto.newBuilder()
                                .setSn(period.getSn())
                                .setBegin(period.getBegin().toString())
                                .setEnd(period.getEnd().toString())
                                .setFlag(PricingModelFlag.valueOf(period.getFlag().name()))
                                .build();
                        peakValleyBuilder.addPeriod(periodProto);
                    }
                }

                builder.setPeakValleyPricing(peakValleyBuilder.build());
                break;

            case TIME_PERIOD_PRICING:
                // 时段计价：运营商自定义时段价格
                TimePeriodPricingProto.Builder timePeriodBuilder = TimePeriodPricingProto.newBuilder();
                
                if (pricingModel.getTimePeriodItems() != null) {
                    // 转换自定义时段计价数据
                    for (TimePeriodItem item : pricingModel.getTimePeriodItems()) {
                        TimePeriodItemProto.Builder itemBuilder = TimePeriodItemProto.newBuilder()
                                .setPeriodNo(item.getPeriodNo())
                                .setStartTime(item.getStartTime().toString())
                                .setEndTime(item.getEndTime().toString())
                                .setElecPrice(item.getElecPrice().toPlainString())
                                .setServPrice(item.getServPrice().toPlainString());

                        if (item.getDescription() != null) {
                            itemBuilder.setDescription(item.getDescription());
                        }

                        timePeriodBuilder.addPeriods(itemBuilder.build());
                    }
                } else if (pricingModel.getPeriodsList() != null) {
                    // 兼容处理：将峰谷时段数据转换为时段计价格式
                    for (Period period : pricingModel.getPeriodsList()) {
                        FlagPrice flagPrice = pricingModel.getFlagPriceList() != null ? 
                                pricingModel.getFlagPriceList().get(period.getFlag()) : null;
                        
                        TimePeriodItemProto.Builder itemBuilder = TimePeriodItemProto.newBuilder()
                                .setPeriodNo(period.getSn())
                                .setStartTime(period.getBegin().toString())
                                .setEndTime(period.getEnd().toString());

                        if (flagPrice != null) {
                            itemBuilder.setElecPrice(flagPrice.getElec().toPlainString())
                                      .setServPrice(flagPrice.getServ().toPlainString());
                        }

                        timePeriodBuilder.addPeriods(itemBuilder.build());
                    }
                }

                builder.setTimePeriodPricing(timePeriodBuilder.build());
                break;

            default:
                throw new IllegalArgumentException("Unsupported pricing rule: " + pricingModel.getRule());
        }

        return builder.build();
    }
}