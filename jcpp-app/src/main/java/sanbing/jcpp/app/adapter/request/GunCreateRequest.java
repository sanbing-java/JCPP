/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sanbing.jcpp.infrastructure.util.validation.NoXss;

import java.util.UUID;

@Data
public class GunCreateRequest {
    
    @NotBlank(message = "充电枪名称不能为空")
    @NoXss
    private String gunName;
    
    @NotBlank(message = "充电枪编号不能为空")
    @NoXss
    private String gunNo;
    
    @NotBlank(message = "充电枪编码不能为空")
    @NoXss
    private String gunCode;
    
    @NotNull(message = "充电站ID不能为空")
    private UUID stationId;
    
    @NotNull(message = "充电桩ID不能为空")
    private UUID pileId;
}
