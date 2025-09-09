/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import sanbing.jcpp.app.dal.config.ibatis.enums.GunRunStatusEnum;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 充电枪响应DTO（包含状态信息）
 *
 * @author 九筒
 */
@Data
public class GunWithStatusResponse {

    /**
     * 充电枪ID
     */
    private UUID id;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 充电枪名称
     */
    private String gunName;

    /**
     * 充电枪编号，不允许修改
     */
    private String gunNo;

    /**
     * 充电枪编码，不允许修改
     */
    private String gunCode;

    /**
     * 充电站ID
     */
    private UUID stationId;

    /**
     * 充电桩ID
     */
    private UUID pileId;

    /**
     * 充电站名称
     */
    private String stationName;

    /**
     * 充电桩名称
     */
    private String pileName;

    /**
     * 充电桩编码
     */
    private String pileCode;

    /**
     * 附加信息
     */
    private JsonNode additionalInfo;

    /**
     * 版本号
     */
    private Integer version;

    // ========== 状态信息 ==========

    /**
     * 充电枪运行状态
     */
    private GunRunStatusEnum runStatus;
}
