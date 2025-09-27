/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository;

import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.dal.entity.Gun;

import java.util.UUID;

/**
 * 充电枪数据访问接口
 * 
 * @author 九筒
 */
public interface GunRepository {
    

    /**
     * 根据充电桩编码和充电枪编号查询充电枪
     * 充电桩上报的是 pile_code + gun_no 的组合，这个组合是唯一的
     * 
     * @param pileCode 充电桩编码
     * @param gunNo 充电枪编号 (如: "01", "02")
     * @return 充电枪实体，如果不存在返回null
     */
    Gun findByPileCodeAndGunNo(String pileCode, String gunNo);

    /**
     * 根据枪编号查询充电枪
     * 
     * @param gunCode 充电枪编码
     * @return 充电枪实体，如果不存在返回null
     */
    Gun findByGunCode(String gunCode);

    GunWithStatusResponse findGunWithStatusByCode(String gunCode);
    
    /**
     * 根据充电枪ID查询充电枪
     * 
     * @param gunId 充电枪ID
     * @return 充电枪实体，如果不存在返回null
     */
    Gun findById(UUID gunId);
}
