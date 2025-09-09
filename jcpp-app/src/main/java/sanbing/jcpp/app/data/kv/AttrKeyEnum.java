/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.data.kv;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 属性键枚举，定义系统内置的属性键
 * 使用String类型提高可读性
 *
 * @author 九筒
 */
public enum AttrKeyEnum {

    /**
     * 状态
     */
    STATUS( "status"),

    /**
     * 连接时间
     */
    CONNECTED_AT("connectedAt"),

    /**
     * 断开连接时间
     */
    DISCONNECTED_AT("disconnectedAt"),

    /**
     * 最后活跃时间
     */
    LAST_ACTIVE_TIME("lastActiveTime"),

    /**
     * 充电枪运行状态
     */
    GUN_RUN_STATUS("gunRunStatus"),

    /**
     * 地锁状态
     */
    LOCK_STATUS("lockStatus"),

    /**
     * 车位状态
     */
    PARK_STATUS("parkStatus");

    @JsonValue
    private final String code;

    AttrKeyEnum( String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
