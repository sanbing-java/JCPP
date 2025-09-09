/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import sanbing.jcpp.app.dal.config.ibatis.enums.GunRunStatusEnum;
import sanbing.jcpp.infrastructure.util.validation.NoXss;

@Data
public class GunUpdateRequest {
    
    @NotBlank(message = "充电枪名称不能为空")
    @NoXss
    private String gunName;
    
    private GunRunStatusEnum runStatus;
}
