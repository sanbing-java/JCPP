/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.response;

/**
 * 统一错误码管理
 * 避免魔法值硬编码，便于维护和扩展
 * 
 * @author 九筒
 */
public enum ErrorCode {
    
    // ==================== 通用错误码 ====================
    /**
     * 成功
     */
    SUCCESS("SUCCESS", "操作成功"),
    
    /**
     * 系统异常
     */
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常，请稍后重试"),
    
    /**
     * 业务异常
     */
    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败"),
    
    // ==================== 参数校验相关 ====================
    /**
     * 参数校验失败
     */
    VALIDATION_ERROR("VALIDATION_ERROR", "参数校验失败"),
    
    /**
     * 数据绑定异常
     */
    BINDING_ERROR("BINDING_ERROR", "数据绑定异常"),
    
    /**
     * 非法参数
     */
    ILLEGAL_ARGUMENT("ILLEGAL_ARGUMENT", "参数错误"),
    
    /**
     * 非法状态
     */
    ILLEGAL_STATE("ILLEGAL_STATE", "状态错误"),
    
    // ==================== 认证授权相关 ====================
    /**
     * 未认证
     */
    UNAUTHORIZED("UNAUTHORIZED", "用户未认证"),
    
    /**
     * 认证失败
     */
    AUTH_FAILED("AUTH_FAILED", "用户名或密码错误"),
    
    /**
     * JWT认证失败
     */
    JWT_AUTH_FAILED("JWT_AUTH_FAILED", "JWT Token认证失败"),
    
    /**
     * 权限不足
     */
    FORBIDDEN("FORBIDDEN", "权限不足"),
    
    // ==================== 资源相关 ====================
    /**
     * 资源不存在
     */
    NOT_FOUND("NOT_FOUND", "请求的资源不存在"),
    
    /**
     * 资源冲突
     */
    CONFLICT("CONFLICT", "资源冲突"),
    
    // ==================== 业务特定错误码 ====================
    /**
     * 充电桩编码已存在
     */
    PILE_CODE_EXISTS("PILE_CODE_EXISTS", "充电桩编码已存在"),
    
    /**
     * 充电站名称已存在
     */
    STATION_NAME_EXISTS("STATION_NAME_EXISTS", "充电站名称已存在"),
    
    /**
     * 充电枪编号已存在
     */
    GUN_CODE_EXISTS("GUN_CODE_EXISTS", "充电枪编号已存在"),
    
    /**
     * 充电桩不存在
     */
    PILE_NOT_FOUND("PILE_NOT_FOUND", "充电桩不存在"),
    
    /**
     * 充电站不存在
     */
    STATION_NOT_FOUND("STATION_NOT_FOUND", "充电站不存在"),
    
    /**
     * 充电枪不存在
     */
    GUN_NOT_FOUND("GUN_NOT_FOUND", "充电枪不存在");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 根据错误码查找枚举
     * 
     * @param code 错误码
     * @return 对应的枚举，如果不存在返回null
     */
    public static ErrorCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        
        return null;
    }
}

















