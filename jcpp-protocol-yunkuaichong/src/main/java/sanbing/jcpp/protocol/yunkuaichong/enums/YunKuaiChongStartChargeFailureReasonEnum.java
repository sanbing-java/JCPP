/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.enums;

import lombok.Getter;

/**
 * 云快充协议启动充电失败原因枚举
 *
 * @author baiban
 * @since 2024-12-16
 */
@Getter
public enum YunKuaiChongStartChargeFailureReasonEnum {

    /** 成功（无失败原因） */
    SUCCESS(0x00, "SUCCESS", "成功"),
    
    /** 账户不存在 */
    ACCOUNT_NOT_EXISTS(0x01, "ACCOUNT_NOT_EXISTS", "账户不存在"),
    
    /** 账户冻结 */
    ACCOUNT_FROZEN(0x02, "ACCOUNT_FROZEN", "账户冻结"),
    
    /** 账户余额不足 */
    INSUFFICIENT_BALANCE(0x03, "INSUFFICIENT_BALANCE", "账户余额不足"),
    
    /** 该卡存在未结账记录 */
    CARD_HAS_UNPAID_RECORD(0x04, "CARD_HAS_UNPAID_RECORD", "该卡存在未结账记录"),
    
    /** 桩停用 */
    PILE_DISABLED(0x05, "PILE_DISABLED", "桩停用"),
    
    /** 该账户不能在此桩上充电 */
    ACCOUNT_NOT_ALLOWED_ON_PILE(0x06, "ACCOUNT_NOT_ALLOWED_ON_PILE", "该账户不能在此桩上充电"),
    
    /** 密码错误 */
    PASSWORD_ERROR(0x07, "PASSWORD_ERROR", "密码错误"),
    
    /** 电站电容不足 */
    INSUFFICIENT_STATION_CAPACITY(0x08, "INSUFFICIENT_STATION_CAPACITY", "电站电容不足"),
    
    /** 系统中vin码不存在 */
    VIN_CODE_NOT_EXISTS(0x09, "VIN_CODE_NOT_EXISTS", "系统中vin码不存在"),
    
    /** 该桩存在未结账记录 */
    PILE_HAS_UNPAID_RECORD(0x0A, "PILE_HAS_UNPAID_RECORD", "该桩存在未结账记录"),
    
    /** 该桩不支持刷卡 */
    PILE_NOT_SUPPORT_CARD(0x0B, "PILE_NOT_SUPPORT_CARD", "该桩不支持刷卡"),
    
    /** 未知错误 */
    UNKNOWN(0xFF, "UNKNOWN", "未知错误");

    /** 失败原因代码 */
    private final int code;
    
    /** 失败原因值 */
    private final String value;
    
    /** 失败原因描述 */
    private final String description;

    YunKuaiChongStartChargeFailureReasonEnum(int code, String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    /**
     * 根据代码获取失败原因枚举
     *
     * @param code 失败原因代码
     * @return 失败原因枚举，未找到时返回UNKNOWN
     */
    public static YunKuaiChongStartChargeFailureReasonEnum fromCode(int code) {
        for (YunKuaiChongStartChargeFailureReasonEnum reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据值获取失败原因枚举
     *
     * @param value 失败原因值
     * @return 失败原因枚举，未找到时返回UNKNOWN
     */
    public static YunKuaiChongStartChargeFailureReasonEnum fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SUCCESS;
        }
        
        for (YunKuaiChongStartChargeFailureReasonEnum reason : values()) {
            if (reason.value.equals(value)) {
                return reason;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据描述获取失败原因枚举
     *
     * @param description 失败原因描述
     * @return 失败原因枚举，未找到时返回UNKNOWN
     */
    public static YunKuaiChongStartChargeFailureReasonEnum fromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return SUCCESS;
        }
        
        for (YunKuaiChongStartChargeFailureReasonEnum reason : values()) {
            if (reason.description.equals(description)) {
                return reason;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据代码获取失败原因值
     *
     * @param code 失败原因代码
     * @return 失败原因值
     */
    public static String getValue(int code) {
        return fromCode(code).getValue();
    }

    /**
     * 根据值获取失败原因代码
     *
     * @param value 失败原因值
     * @return 失败原因代码
     */
    public static int getCode(String value) {
        return fromValue(value).getCode();
    }

    /**
     * 根据值获取失败原因描述
     *
     * @param value 失败原因值
     * @return 失败原因描述
     */
    public static String getDescription(String value) {
        return fromValue(value).getDescription();
    }

    /**
     * 根据代码获取失败原因描述
     *
     * @param code 失败原因代码
     * @return 失败原因描述
     */
    public static String getDescription(int code) {
        return fromCode(code).getDescription();
    }

    /**
     * 检查是否为成功状态
     *
     * @param code 失败原因代码
     * @return true表示成功，false表示失败
     */
    public static boolean isSuccess(int code) {
        return code == SUCCESS.code;
    }

    /**
     * 检查是否为成功状态
     *
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 获取字节形式的代码
     *
     * @return 字节形式的失败原因代码
     */
    public byte getByteCode() {
        return (byte) code;
    }
}
