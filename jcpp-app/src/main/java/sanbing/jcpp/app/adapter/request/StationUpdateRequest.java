/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import sanbing.jcpp.infrastructure.util.validation.NoXss;


/**
 * 更新充电站请求
 * 
 * @author 九筒
 */
@Data
public class StationUpdateRequest {
    
    @NotBlank(message = "充电站名称不能为空")
    @NoXss
    private String stationName;
    
    private Float longitude;        // 经度
    private Float latitude;         // 纬度
    
    @NoXss
    private String province;        // 省份
    
    @NoXss
    private String city;            // 城市
    
    @NoXss
    private String county;          // 区县
    
    @NoXss
    private String address;         // 详细地址
}
