/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.app.adapter.request.GunCreateRequest;
import sanbing.jcpp.app.adapter.request.GunQueryRequest;
import sanbing.jcpp.app.adapter.request.GunUpdateRequest;
import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.proto.gen.UplinkProto.GunRunStatus;

import java.util.UUID;

public interface GunService {
    
    /**
     * 创建充电枪
     */
    Gun createGun(GunCreateRequest request);
    
    /**
     * 根据ID查询充电枪
     */
    Gun findById(UUID id);
    
    /**
     * 更新充电枪
     */
    Gun updateGun(UUID id, GunUpdateRequest request);
    
    /**
     * 删除充电枪
     */
    void deleteGun(UUID id);
    
    /**
     * 分页查询充电枪及状态信息
     */
    PageResponse<GunWithStatusResponse> queryGunsWithStatus(GunQueryRequest request);


    /**
     * 根据充电桩编码和充电枪编号查询充电枪
     * 充电桩上报的是 pile_code + gun_no 的组合，这个组合是唯一的
     */
    Gun findByPileCodeAndGunNo(String pileCode, String gunNo);

    /**
     * 根据枪编号查询充电枪
     */
    Gun findByGunCode(String gunCode);

    GunWithStatusResponse findGunWithStatusByCode(String gunCode);
    
    /**
     * 查询充电枪状态
     * 
     * @param gunId 充电枪ID
     * @return 状态字符串，如果不存在返回null
     */
    String findGunStatus(UUID gunId);
    
    /**
     * 保存充电枪状态变更时序数据 - 高性能版本
     * 
     * @param gunId 充电枪ID
     * @param status 状态
     * @param ts 时间戳，如果为null则使用当前时间
     */
    void saveGunStatusChange(UUID gunId, String status, Long ts);
    
    /**
     * 处理充电枪状态上报
     * 
     * @param pileCode 充电桩编码
     * @param gunNo 充电枪编号 (如: "01", "02")
     * @param protoStatus Proto状态
     * @param ts 时间戳
     * @return 是否需要更新充电桩状态
     */
    boolean handleGunRunStatus(String pileCode, String gunNo, GunRunStatus protoStatus, long ts);
    
}
