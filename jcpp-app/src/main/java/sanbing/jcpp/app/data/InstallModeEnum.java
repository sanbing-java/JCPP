/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.data;

import lombok.Getter;

/**
 * 数据库安装模式枚举
 * 
 * @author 九筒
 */
@Getter
public enum InstallModeEnum {
    
    /**
     * 初始化数据库，执行schema-init.sql并加载演示数据
     */
    INIT("init", "初始化数据库"),
    
    /**
     * 升级数据库，根据版本执行升级脚本
     */
    UPGRADE("upgrade", "升级数据库"),
    
    /**
     * 不执行任何操作
     */
    DISABLED("disabled", "禁用安装功能");
    
    private final String mode;
    private final String description;
    
    InstallModeEnum(String mode, String description) {
        this.mode = mode;
        this.description = description;
    }
    
    /**
     * 根据mode字符串获取枚举值
     */
    public static InstallModeEnum fromMode(String mode) {
        if (mode == null || mode.isEmpty()) {
            return DISABLED;
        }
        
        for (InstallModeEnum installMode : values()) {
            if (installMode.mode.equals(mode)) {
                return installMode;
            }
        }
        
        return DISABLED;
    }
}
