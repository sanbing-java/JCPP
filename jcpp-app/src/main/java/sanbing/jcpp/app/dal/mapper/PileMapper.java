/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import sanbing.jcpp.app.adapter.request.PileQueryRequest;
import sanbing.jcpp.app.adapter.response.PileWithStatusResponse;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.data.kv.AttrKeyEnum;

import java.util.UUID;

/**
 * @author 九筒
 */
public interface PileMapper extends BaseMapper<Pile> {

    /**
     * 根据充电桩编码查询充电桩
     * 
     * @param pileCode 充电桩编码
     * @return 充电桩实体
     */
    Pile selectByCode(String pileCode);

    /**
     * 分页查询充电桩及其状态信息
     * 使用MyBatis XML配置，避免魔法值错误，提高SQL可读性和可维护性
     * 
     * @param page 分页参数
     * @param request 查询请求参数
     * @param statusKey 状态属性键
     * @param connectedAtKey 连接时间属性键
     * @param disconnectedAtKey 断开连接时间属性键
     * @param lastActiveTimeKey 最后活跃时间属性键
     */
    IPage<PileWithStatusResponse> selectPileWithStatusPage(
            Page<PileWithStatusResponse> page, 
            @Param("request") PileQueryRequest request,
            @Param("statusKey") AttrKeyEnum statusKey,
            @Param("connectedAtKey") AttrKeyEnum connectedAtKey,
            @Param("disconnectedAtKey") AttrKeyEnum disconnectedAtKey,
            @Param("lastActiveTimeKey") AttrKeyEnum lastActiveTimeKey
    );
    
    /**
     * 统计充电站下的充电桩数量
     * 
     * @param stationId 充电站ID
     * @return 充电桩数量
     */
    long countByStationId(@Param("stationId") UUID stationId);
    
    /**
     * 统计在线充电桩数量
     * 
     * @param statusKey 状态属性键
     * @param onlineStatus 在线状态值
     * @return 在线充电桩数量
     */
    long countOnlinePiles(@Param("statusKey") String statusKey, @Param("onlineStatus") String onlineStatus);
    
    /**
     * 统计离线充电桩数量（包括未设置状态的）
     * 
     * @param statusKey 状态属性键
     * @param offlineStatus 离线状态值
     * @return 离线充电桩数量
     */
    long countOfflinePiles(@Param("statusKey") String statusKey, @Param("offlineStatus") String offlineStatus);
}