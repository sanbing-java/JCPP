/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 充电桩状态枚举
 */
@AllArgsConstructor
@Getter
public enum LvnengPileStatusEnum {
    /**
     * 0-空闲中
     * 1-正准备开始充电
     * 2-充电进行中
     * 3-充电结東
     * 4-启动失败
     * 5-预约状态
     * 6-系统故障（不能给汽车充电）
     * 注：目前工作状态只有 0,2,6
     */

    IDLE(0, "空闲中"),

    CHARGING(2, "充电进行中"),

    SYSTEM_FAILURE(6, "系统故障（不能给汽车充电）");


    private final int code;

    private final String description;


    private static final String UNKNOWN_DESC = "未知状态";
    private static final Map<Integer, LvnengPileStatusEnum> CODE_TO_ENUM_MAP = new HashMap<>();

    static {
        for (LvnengPileStatusEnum enumValue : LvnengPileStatusEnum.values()) {
            CODE_TO_ENUM_MAP.put(enumValue.getCode(), enumValue);
        }
    }

    public static String getByCode(int code) {
        LvnengPileStatusEnum lvnengPileStatusEnum = CODE_TO_ENUM_MAP.get(code);
        if (lvnengPileStatusEnum == null) {
            return UNKNOWN_DESC;
        }
        return lvnengPileStatusEnum.getDescription();


    }

}