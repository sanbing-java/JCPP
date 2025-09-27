/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileTypeEnum;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 充电桩响应DTO（包含状态信息）
 *
 * @author 九筒
 */
@Data
public class PileWithStatusResponse {

    /**
     * 充电桩ID
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
     * 充电桩名称
     */
    private String pileName;

    /**
     * 充电桩编码，不允许修改
     */
    private String pileCode;

    /**
     * 协议类型
     */
    private String protocol;

    /**
     * 充电站ID
     */
    private UUID stationId;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 型号
     */
    private String model;

    /**
     * 制造商
     */
    private String manufacturer;

    /**
     * 充电桩类型（交流/直流）
     */
    private PileTypeEnum type;

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
     * 充电桩状态
     */
    private PileStatusEnum status;

    /**
     * 最近连接时间（13位时间戳）
     */
    private Long connectedAt;

    /**
     * 最后断线时间（13位时间戳）
     */
    private Long disconnectedAt;

    /**
     * 最后活跃时间（13位时间戳）
     */
    private Long lastActiveTime;

    /**
     * 充电枪数量
     */
    private Long gunCount;
}
