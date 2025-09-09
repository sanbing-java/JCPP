/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import org.springframework.security.core.AuthenticationException;
import sanbing.jcpp.app.dal.entity.User;
import sanbing.jcpp.app.service.security.model.UserCredentials;

import java.util.UUID;

/**
 * 用户服务接口
 * 
 * @author 九筒
 */
public interface UserService {
    
    /**
     * 根据ID查询用户
     * 
     * @param id 用户ID
     * @return 用户实体，如果不存在返回null
     */
    User findById(UUID id);
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户实体，如果不存在返回null
     */
    User findUserByUsername(String username);
    
    /**
     * 增加用户登录失败次数
     * 
     * @param userId 用户ID
     * @return 更新后的失败次数
     */
    int increaseFailedLoginAttempts(UUID userId);
    
    /**
     * 设置用户凭证启用状态
     * 
     * @param userId 用户ID
     * @param enabled 是否启用，true-启用，false-禁用
     */
    void setUserCredentialsEnabled(UUID userId, boolean enabled);
    
    /**
     * 重置用户登录失败次数
     * 
     * @param userId 用户ID
     */
    void resetFailedLoginAttempts(UUID userId);
    
    /**
     * 验证用户凭证
     * 
     * @param userId 用户ID
     * @param userCredentials 用户凭证
     * @param username 用户名
     * @param password 密码
     * @throws AuthenticationException 验证失败时抛出
     */
    void validateUserCredentials(UUID userId, UserCredentials userCredentials, String username, String password) throws AuthenticationException;
    
}
