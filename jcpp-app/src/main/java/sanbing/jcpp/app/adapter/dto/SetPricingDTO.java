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
import sanbing.jcpp.proto.gen.DownlinkProto.SetPricingRequest;

/**
 * 设置计费策略DTO
 *
 * @author 九筒
 */
@Data
public class SetPricingDTO {

    /**
     * 充电桩编码
     */
    @NotBlank(message = "充电桩编码不能为空")
    private String pileCode;

    /**
     * 计费策略请求
     */
    @NotNull(message = "计费策略请求不能为空")
    private SetPricingRequest setPricingRequest;
}
