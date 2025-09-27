/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 启动充电DTO
 * 
 * @author 九筒
 */
@Data
public class StartChargeDTO {

    /**
     * 充电桩编码
     */
    @NotBlank(message = "充电桩编码不能为空")
    private String pileCode;

    /**
     * 充电枪编号
     */
    @NotBlank(message = "充电枪编号不能为空")
    private String gunNo;

    /**
     * 限制金额（元）
     */
    @NotNull(message = "限制金额不能为空")
    private BigDecimal limitYuan;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 逻辑卡号
     */
    private String logicalCardNo;

    /**
     * 物理卡号
     */
    private String physicalCardNo;

    /**
     * 并充序号（当不为空时，自动使用并充启机命令）
     */
    private String parallelNo;
}
