/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 充电站查询请求
 * 
 * @author 九筒
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StationQueryRequest extends PageRequest {
    
    private String stationName;     // 充电站名称
    private String stationCode;     // 充电站编码
    private String province;        // 省份
    private String city;            // 城市
    private String county;          // 区县
    private String address;         // 详细地址
    private String keyword;         // 关键字搜索（站名或编码）
}
