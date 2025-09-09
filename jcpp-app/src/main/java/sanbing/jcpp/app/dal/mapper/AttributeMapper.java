/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import sanbing.jcpp.app.dal.entity.Attribute;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 属性数据访问层
 *
 * @author 九筒
 */
@Mapper
public interface AttributeMapper extends BaseMapper<Attribute> {

    /**
     * 查询实体的所有属性
     */
    List<Attribute> findByEntity(@Param("entityId") UUID entityId);

    /**
     * 查询实体的特定属性
     */
    Attribute findByEntityAndKey(@Param("entityId") UUID entityId, @Param("attrKey") String attrKey);

    /**
     * 查询实体在指定属性类型下的所有属性 (兼容原JPA方法)
     * 注意：此方法主要用于兼容性，实际t_attr表中没有attribute_type字段
     */
    List<Attribute> findAllByEntityIdAndAttributeType(@Param("entityId") UUID entityId);

    /**
     * 删除指定实体的指定属性
     */
    void deleteByEntityIdAndKey(@Param("entityId") UUID entityId,
                                @Param("attrKey") String attrKey);

    /**
     * 根据实体ID和属性键列表查询属性
     *
     */
    List<Attribute> findAllByIdAndAttrKey(UUID entityId, Collection<String> attrKeys);
}
