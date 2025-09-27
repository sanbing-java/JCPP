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
import sanbing.jcpp.app.adapter.request.GunQueryRequest;
import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.dal.entity.Gun;

import java.util.UUID;

/**
 * @author 九筒
 */
public interface GunMapper extends BaseMapper<Gun> {


    /**
     * 根据充电桩编码和充电枪编号查询充电枪
     * 充电桩上报的是 pile_code + gun_no 的组合，这个组合是唯一的
     */
    Gun selectByPileCodeAndGunNo(@Param("pileCode") String pileCode, @Param("gunNo") String gunNo);

    /**
     * 根据枪编号查询充电枪
     */
    Gun selectByGunCode(@Param("gunCode") String gunCode);

    GunWithStatusResponse selectGunWithStatusByCode(@Param("gunCode") String gunCode);

    /**
     * 分页查询充电枪及其状态信息
     * 使用MyBatis XML配置，避免魔法值错误，提高SQL可读性和可维护性
     */
    IPage<GunWithStatusResponse> selectGunWithStatusPage(Page<GunWithStatusResponse> page, @Param("request") GunQueryRequest request);
    
    /**
     * 统计充电桩下的充电枪数量
     * 
     * @param pileId 充电桩ID
     * @return 充电枪数量
     */
    long countByPileId(@Param("pileId") UUID pileId);
    
    /**
     * 统计空闲状态的充电枪数量 (IDLE)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 空闲充电枪数量
     */
    long countIdleGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计已插枪未充电状态的充电枪数量 (INSERTED)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 已插枪未充电充电枪数量
     */
    long countInsertedGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计充电中状态的充电枪数量 (CHARGING)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 充电中充电枪数量
     */
    long countChargingGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计充电完成状态的充电枪数量 (CHARGE_COMPLETE)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 充电完成充电枪数量
     */
    long countChargeCompleteGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计放电准备状态的充电枪数量 (DISCHARGE_READY)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 放电准备充电枪数量
     */
    long countDischargeReadyGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计放电中状态的充电枪数量 (DISCHARGING)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 放电中充电枪数量
     */
    long countDischargingGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计放电完成状态的充电枪数量 (DISCHARGE_COMPLETE)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 放电完成充电枪数量
     */
    long countDischargeCompleteGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计预约状态的充电枪数量 (RESERVED)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 预约充电枪数量
     */
    long countReservedGuns(@Param("statusKey") String statusKey, @Param("status") String status);
    
    /**
     * 统计故障状态的充电枪数量 (FAULT)
     * 
     * @param statusKey 状态属性键
     * @param status 状态值
     * @return 故障充电枪数量
     */
    long countFaultGuns(@Param("statusKey") String statusKey, @Param("status") String status);
}