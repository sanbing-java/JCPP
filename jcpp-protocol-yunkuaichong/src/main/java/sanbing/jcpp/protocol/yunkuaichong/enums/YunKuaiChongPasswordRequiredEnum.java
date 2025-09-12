/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.enums;

import lombok.Getter;

/**
 * 云快充协议密码验证标志枚举
 *
 * @author baiban
 * @since 2024-12-16
 */
@Getter
public enum YunKuaiChongPasswordRequiredEnum {

    /** 不需要密码 */
    NOT_REQUIRED(0x00, false, "不需要密码"),
    
    /** 需要密码 */
    REQUIRED(0x01, true,"需要密码");

    /** 密码验证标志代码 */
    private final int code;

    /** 是否需要密码的布尔值 */
    private final boolean value;
    
    /** 密码验证标志描述 */
    private final String description;

    YunKuaiChongPasswordRequiredEnum(int code, boolean value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    /**
     * 根据代码获取密码验证标志枚举
     *
     * @param code 密码验证标志代码
     * @return 密码验证标志枚举，未找到时返回NOT_REQUIRED
     */
    public static YunKuaiChongPasswordRequiredEnum fromCode(int code) {
        for (YunKuaiChongPasswordRequiredEnum passwordRequired : values()) {
            if (passwordRequired.code == code) {
                return passwordRequired;
            }
        }
        return NOT_REQUIRED;
    }

    /**
     * 根据代码获取密码验证标志描述
     *
     * @param code 密码验证标志代码
     * @return 密码验证标志描述
     */
    public static String getDescription(int code) {
        return fromCode(code).getDescription();
    }

    /**
     * 根据代码检查是否需要密码
     *
     * @param code 密码验证标志代码
     * @return true表示需要密码，false表示不需要密码
     */
    public static boolean isPasswordRequired(int code) {
        return fromCode(code).isValue();
    }

    /**
     * 根据布尔值获取对应的枚举
     *
     * @param required 是否需要密码
     * @return 对应的枚举值
     */
    public static YunKuaiChongPasswordRequiredEnum fromBoolean(boolean required) {
        return required ? REQUIRED : NOT_REQUIRED;
    }
}
