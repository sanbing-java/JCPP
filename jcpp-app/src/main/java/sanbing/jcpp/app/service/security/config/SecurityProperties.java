/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * 统一的安全配置属性
 * 包含安全设置和JWT设置
 * 
 * @author 九筒
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    
    /**
     * 安全设置
     */
    private SecuritySettingsProperties settings;
    
    /**
     * JWT设置
     */
    private JwtProperties jwt;
    
    /**
     * 安全设置属性
     */
    @Data
    public static class SecuritySettingsProperties {
        
        /**
         * 密码策略配置
         */
        private PasswordPolicyProperties passwordPolicy;
        
        /**
         * 最大登录失败次数
         */
        private Integer maxFailedLoginAttempts;
        
        /**
         * 用户锁定通知邮箱
         */
        private String userLockoutNotificationEmail;
        
        /**
         * 移动端密钥长度
         */
        private Integer mobileSecretKeyLength;
        
        /**
         * 用户激活令牌TTL (小时)
         */
        private Integer userActivationTokenTtl;
        
        /**
         * 密码重置令牌TTL (小时)
         */
        private Integer passwordResetTokenTtl;
    }
    
    /**
     * 密码策略属性配置
     */
    @Data
    public static class PasswordPolicyProperties implements Serializable {
        
        /**
         * 最小长度
         */
        private Integer minimumLength;
        
        /**
         * 最大长度
         */
        private Integer maximumLength;
        
        /**
         * 最小大写字母数
         */
        private Integer minimumUppercaseLetters;
        
        /**
         * 最小小写字母数
         */
        private Integer minimumLowercaseLetters;
        
        /**
         * 最小数字位数
         */
        private Integer minimumDigits;
        
        /**
         * 最小特殊字符数
         */
        private Integer minimumSpecialCharacters;
        
        /**
         * 允许空格
         */
        private Boolean allowWhitespaces;
    }
    
    /**
     * JWT配置属性
     */
    @Data
    public static class JwtProperties {
        
        /**
         * JWT令牌过期时间（秒）
         */
        private Integer tokenExpirationTime;
        
        /**
         * 刷新令牌过期时间（秒）
         */
        private Integer refreshTokenExpTime;
        
        /**
         * 令牌发行者
         */
        private String tokenIssuer;
        
        /**
         * 令牌签名密钥
         */
        private String tokenSigningKey;
    }
}
