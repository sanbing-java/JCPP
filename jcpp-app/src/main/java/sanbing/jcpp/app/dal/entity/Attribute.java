/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import sanbing.jcpp.app.data.kv.*;
import sanbing.jcpp.infrastructure.cache.HasVersion;

import java.io.Serializable;
import java.util.UUID;

/**
 * 属性实体，用于存储设备的最新属性数据
 * 采用键值对存储结构设计
 *
 * @author 九筒
 */
@Data
@TableName("t_attr")
public class Attribute implements Serializable, HasVersion {

    /**
     * 实体ID (UUID保证全局唯一)
     * 复合主键的一部分
     */
    @TableId(value = "entity_id", type = IdType.INPUT)
    private UUID entityId;

    /**
     * 属性键 (字符串类型提高可读性)
     * 复合主键的一部分
     */
    @TableField("attr_key")
    private String attrKey;

    /**
     * 布尔值
     */
    @TableField("bool_v")
    private Boolean boolV;

    /**
     * 字符串值
     */
    @TableField("str_v")
    private String strV;

    /**
     * 长整型值
     */
    @TableField("long_v")
    private Long longV;

    /**
     * 双精度值
     */
    @TableField("dbl_v")
    private Double dblV;

    /**
     * JSON值
     */
    @TableField("json_v")
    private String jsonV;

    /**
     * 最后更新时间戳
     */
    @TableField("last_update_ts")
    private Long lastUpdateTs;

    /**
     * 版本号（用于乐观锁控制）
     */
    @TableField
    private Integer version;

    public AttributeKvEntry toData() {
        KvEntry kvEntry = null;
        if (strV != null) {
            kvEntry = new StringDataEntry(attrKey, strV);
        } else if (boolV != null) {
            kvEntry = new BooleanDataEntry(attrKey, boolV);
        } else if (dblV != null) {
            kvEntry = new DoubleDataEntry(attrKey, dblV);
        } else if (longV != null) {
            kvEntry = new LongDataEntry(attrKey, longV);
        } else if (jsonV != null) {
            kvEntry = new JsonDataEntry(attrKey, jsonV);
        }

        return new BaseAttributeKvEntry(kvEntry, lastUpdateTs, version);
    }
}
