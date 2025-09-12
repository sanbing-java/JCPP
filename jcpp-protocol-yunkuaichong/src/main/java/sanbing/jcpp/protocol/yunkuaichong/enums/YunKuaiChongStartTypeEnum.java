/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.enums;

import lombok.Getter;

/**
 * 云快充协议启动方式枚举
 *
 * @author baiban
 * @since 2024-12-16
 */
@Getter
public enum YunKuaiChongStartTypeEnum {

    /** 通过刷卡启动充电 */
    CARD_START(0x01, "CARD_START", "刷卡启动"),
    
    /** 通过账号启动充电（暂不支持） */
    ACCOUNT_START(0x02, "ACCOUNT_START", "账号启动"),
    
    /** VIN码启动充电 */
    VIN_START(0x03, "VIN_START", "VIN码启动"),
    
    /** 未知启动方式 */
    UNKNOWN(0xFF, "UNKNOWN", "未知启动方式");

    /** 启动方式代码 */
    private final int code;

    /** 启动方式值 */
    private final String value;
    
    /** 启动方式描述 */
    private final String description;

    YunKuaiChongStartTypeEnum(int code, String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    /**
     * 根据代码获取启动方式枚举
     *
     * @param code 启动方式代码
     * @return 启动方式枚举，未找到时返回UNKNOWN
     */
    public static YunKuaiChongStartTypeEnum fromCode(int code) {
        for (YunKuaiChongStartTypeEnum startType : values()) {
            if (startType.code == code) {
                return startType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据值获取启动方式枚举
     *
     * @param value 启动方式值
     * @return 启动方式枚举，未找到时返回UNKNOWN
     */
    public static YunKuaiChongStartTypeEnum fromValue(String value) {
        for (YunKuaiChongStartTypeEnum startType : values()) {
            if (startType.value.equals(value)) {
                return startType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据代码获取启动方式值
     *
     * @param code 启动方式代码
     * @return 启动方式值
     */
    public static String getValue(int code) {
        return fromCode(code).getValue();
    }

    /**
     * 根据值获取启动方式代码
     *
     * @param value 启动方式值
     * @return 启动方式代码
     */
    public static int getCode(String value) {
        return fromValue(value).getCode();
    }

    /**
     * 根据值获取启动方式描述
     *
     * @param value 启动方式值
     * @return 启动方式描述
     */
    public static String getDescription(String value) {
        return fromValue(value).getDescription();
    }

    /**
     * 根据代码获取启动方式描述
     *
     * @param code 启动方式代码
     * @return 启动方式描述
     */
    public static String getDescription(int code) {
        return fromCode(code).getDescription();
    }

    /**
     * 检查是否为有效的启动方式代码
     *
     * @param code 启动方式代码
     * @return true表示有效，false表示无效
     */
    public static boolean isValid(int code) {
        return fromCode(code) != UNKNOWN;
    }
}
