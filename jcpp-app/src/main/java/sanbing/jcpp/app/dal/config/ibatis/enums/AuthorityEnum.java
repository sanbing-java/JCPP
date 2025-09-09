/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.config.ibatis.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 用户权限枚举
 * 对应 sanbing.jcpp.app.service.security.model.Authority
 * 
 * @author 九筒
 */
public enum AuthorityEnum implements IEnum<String> {
    
    /**
     * 系统管理员
     */
    SYS_ADMIN,

    /**
     * 刷新令牌
     */
    REFRESH_TOKEN,
   ;


    public static AuthorityEnum parse(String value) {
        AuthorityEnum authority = null;
        if (value != null && !value.isEmpty()) {
            for (AuthorityEnum current : AuthorityEnum.values()) {
                if (current.name().equalsIgnoreCase(value)) {
                    authority = current;
                    break;
                }
            }
        }
        return authority;
    }

    @Override
    public String getValue() {
        return this.name();
    }

}
