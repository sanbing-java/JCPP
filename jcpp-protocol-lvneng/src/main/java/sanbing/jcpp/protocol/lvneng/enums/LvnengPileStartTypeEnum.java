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
 * 充电桩启动类型枚举
 */
@AllArgsConstructor
@Getter
public enum LvnengPileStartTypeEnum {
    /**
     * 0：刷卡启动
     * 1：服务器启动
     * 2：本地管理员启动
     * 3：VIN 启动 生产个枚举类
     */

    CARD_SWIPE(0, "刷卡启动"),


    SERVER(1, "服务器启动"),


    LOCAL_ADMIN(2, "本地管理员启动"),


    VIN(3, "VIN启动");


    private final int code;

    private final String description;

    private static final String UNKNOWN_DESC = "未知类型";
    private static final Map<Integer, LvnengPileStartTypeEnum> CODE_TO_ENUM_MAP = new HashMap<>();

    static {
        for (LvnengPileStartTypeEnum enumValue : LvnengPileStartTypeEnum.values()) {
            CODE_TO_ENUM_MAP.put(enumValue.getCode(), enumValue);
        }
    }

    public static String getByCode(int code) {
        LvnengPileStartTypeEnum lvnengPileStartTypeEnum = CODE_TO_ENUM_MAP.get(code);
        if (lvnengPileStartTypeEnum == null) {
            return UNKNOWN_DESC;
        }
        return lvnengPileStartTypeEnum.getDescription();


    }

}