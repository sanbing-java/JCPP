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

import java.util.List;
import java.util.UUID;

/**
 * 充电站-充电桩级联选择器选项响应
 * 用于Ant Design Cascader组件
 * 
 * @author 九筒
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationPileCascaderOption {
    
    private String value;        // 选项的值（充电站ID或充电桩ID）
    private String label;        // 显示的标签
    private boolean isLeaf;      // 是否为叶子节点
    private List<StationPileCascaderOption> children;  // 子选项（充电站下的充电桩）
    
    // 额外信息
    private String stationId;    // 充电站ID（当是充电桩选项时）
    private String stationName;  // 充电站名称
    private String stationCode;  // 充电站编码
    private String pileId;       // 充电桩ID（当是充电桩选项时）
    private String pileName;     // 充电桩名称（当是充电桩选项时）
    private String pileCode;     // 充电桩编码（当是充电桩选项时）
    
    /**
     * 创建充电站选项
     */
    public static StationPileCascaderOption createStationOption(UUID stationId, String stationName, String stationCode, List<StationPileCascaderOption> piles) {
        StationPileCascaderOption option = new StationPileCascaderOption();
        option.setValue(stationId.toString());
        option.setLabel(stationName + " (" + stationCode + ")");
        option.setLeaf(false);
        option.setChildren(piles);
        option.setStationId(stationId.toString());
        option.setStationName(stationName);
        option.setStationCode(stationCode);
        return option;
    }
    
    /**
     * 创建充电桩选项
     */
    public static StationPileCascaderOption createPileOption(UUID stationId, String stationName, String stationCode, 
                                                           UUID pileId, String pileName, String pileCode) {
        StationPileCascaderOption option = new StationPileCascaderOption();
        option.setValue(pileId.toString());
        option.setLabel(pileName + " (" + pileCode + ")");
        option.setLeaf(true);
        option.setChildren(null);
        option.setStationId(stationId.toString());
        option.setStationName(stationName);
        option.setStationCode(stationCode);
        option.setPileId(pileId.toString());
        option.setPileName(pileName);
        option.setPileCode(pileCode);
        return option;
    }
}
