/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 充电站选项响应
 * 用于下拉选择组件
 * 
 * @author 九筒
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationOption {
    
    private UUID id;        // 充电站ID
    private String label;      // 显示名称：stationName (stationCode)
    private String stationName; // 充电站名称
    private String stationCode; // 充电站编码
    
    public static StationOption of(UUID id, String stationName, String stationCode) {
        StationOption option = new StationOption();
        option.setId(id);
        option.setStationName(stationName);
        option.setStationCode(stationCode);
        option.setLabel(stationName + " (" + stationCode + ")");
        return option;
    }
}
