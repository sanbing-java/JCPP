/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.proto.model;

import lombok.*;
import sanbing.jcpp.proto.gen.DownlinkProto.PricingModelFlag;
import sanbing.jcpp.proto.gen.DownlinkProto.PricingModelRule;
import sanbing.jcpp.proto.gen.DownlinkProto.PricingModelType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 计费模型 - 支持标准计费、峰谷计价、时段计价三种模式
 */
@Data
public class PricingModel {

    private UUID id;

    // 序列号，用于充电桩协议通信
    private int sequenceNumber;

    private String pileCode;

    private PricingModelType type;      // 计费类型：充电/放电

    private PricingModelRule rule;      // 计费规则：标准/峰谷/时段

    /**
     * 标准电价（元/度）- 标准计费模式使用
     */
    private BigDecimal standardElec;

    /**
     * 标准服务费（元/度）- 标准计费模式使用
     */
    private BigDecimal standardServ;

    /**
     * 峰谷价格配置 - 峰谷计费模式使用
     * key: 时段标志(尖峰/峰/平/谷/深谷)
     * value: 对应的电费和服务费
     */
    private Map<PricingModelFlag, FlagPrice> flagPriceList;

    /**
     * 峰谷时段划分 - 峰谷计费模式使用
     */
    private List<Period> periodsList;

    /**
     * 自定义时段配置 - 时段计价模式使用
     */
    private List<TimePeriodItem> timePeriodItems;

    /**
     * 峰谷时段定义 - 用于峰谷计价模式
     */
    @Setter
    @Getter
    public static class Period {
        private int sn;                         // 时段序号
        private LocalTime begin;                // 起始时间
        private LocalTime end;                  // 结束时间
        private PricingModelFlag flag;          // 时段标志（尖峰/峰/平/谷/深谷）
    }

    /**
     * 峰谷价格定义 - 对应各时段标志的价格
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FlagPrice {
        private BigDecimal elec;                // 电费价格（元/度）
        private BigDecimal serv;                // 服务费价格（元/度）
    }

    /**
     * 自定义时段定义 - 用于时段计价模式
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimePeriodItem {
        private int periodNo;                   // 时段编号（从1开始）
        private LocalTime startTime;            // 开始时间
        private LocalTime endTime;              // 结束时间
        private BigDecimal elecPrice;           // 该时段电费（元/度）
        private BigDecimal servPrice;           // 该时段服务费（元/度）
        private String description;             // 时段名称（如"早高峰"）
    }

}