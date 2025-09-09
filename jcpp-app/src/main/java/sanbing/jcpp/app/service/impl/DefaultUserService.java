/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.dal.entity.User;
import sanbing.jcpp.app.dal.mapper.UserMapper;
import sanbing.jcpp.app.service.UserService;
import sanbing.jcpp.app.service.security.config.SecurityProperties;
import sanbing.jcpp.app.service.security.model.UserCredentials;

import java.util.UUID;

/**
 * 用户服务实现
 * 
 * @author 九筒
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultUserService implements UserService {
    
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final SecurityProperties securityProperties;
    
    @Override
    public User findById(UUID id) {
        if (id == null) {
            log.warn("findById called with null id");
            return null;
        }
        
        try {
            return userMapper.selectById(id);
        } catch (Exception e) {
            log.error("Error finding user by id: {}", id, e);
            throw new RuntimeException("查询用户失败", e);
        }
    }
    
    @Override
    public User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("findUserByUsername called with null or empty username");
            return null;
        }
        
        try {
            return userMapper.findByUserName(username);
        } catch (Exception e) {
            log.error("Error finding user by username: {}", username, e);
            throw new RuntimeException("根据用户名查询用户失败", e);
        }
    }
    
    @Override
    public int increaseFailedLoginAttempts(UUID userId) {
        if (userId == null) {
            log.warn("increaseFailedLoginAttempts called with null userId");
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        try {
            // 1. 查询当前用户
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("User not found with id: {}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 获取当前的userCredentials
            UserCredentials credentials = user.getUserCredentials();
            if (credentials == null) {
                // 如果userCredentials为空，创建新的UserCredentials对象
                credentials = new UserCredentials();
            }
            
            // 3. 获取当前失败次数并累加
            int currentAttempts = credentials.getFailedLoginAttempts() != null ? credentials.getFailedLoginAttempts() : 0;
            int newAttempts = currentAttempts + 1;
            
            // 4. 更新失败次数
            credentials.setFailedLoginAttempts(newAttempts);
            
            // 5. 更新用户实体
            user.setUserCredentials(credentials);
            
            // 6. 保存到数据库
            int updateResult = userMapper.updateById(user);
            if (updateResult == 0) {
                log.error("Failed to update user credentials for userId: {}", userId);
                throw new RuntimeException("更新用户登录失败次数失败");
            }
            
            log.debug("Increased failed login attempts for user {} from {} to {}", userId, currentAttempts, newAttempts);
            return newAttempts;
            
        } catch (Exception e) {
            log.error("Error increasing failed login attempts for userId: {}", userId, e);
            throw e;
        }
    }
    
    @Override
    public void setUserCredentialsEnabled(UUID userId, boolean enabled) {
        if (userId == null) {
            log.warn("setUserCredentialsEnabled called with null userId");
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        try {
            // 1. 查询当前用户
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("User not found with id: {}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 获取当前的userCredentials
            UserCredentials credentials = user.getUserCredentials();
            if (credentials == null) {
                // 如果userCredentials为空，创建新的UserCredentials对象
                credentials = new UserCredentials();
            }
            
            // 3. 获取当前状态
            boolean currentEnabled = credentials.isEnabled();
            
            // 4. 如果状态没有变化，直接返回
            if (currentEnabled == enabled) {
                log.debug("User {} credentials enabled status already is {}, no update needed", userId, enabled);
                return;
            }
            
            // 5. 更新启用状态
            credentials.setEnabled(enabled);
            
            // 6. 更新用户实体
            user.setUserCredentials(credentials);
            
            // 7. 保存到数据库
            int updateResult = userMapper.updateById(user);
            if (updateResult == 0) {
                log.error("Failed to update user credentials enabled status for userId: {}", userId);
                throw new RuntimeException("更新用户凭证启用状态失败");
            }
            
            log.info("Updated user {} credentials enabled status from {} to {}", userId, currentEnabled, enabled);
            
        } catch (Exception e) {
            log.error("Error setting user credentials enabled status for userId: {}, enabled: {}", userId, enabled, e);
            throw e;
        }
    }
    
    @Override
    public void resetFailedLoginAttempts(UUID userId) {
        if (userId == null) {
            log.warn("resetFailedLoginAttempts called with null userId");
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        try {
            // 1. 查询当前用户
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("User not found with id: {}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 获取当前的userCredentials
            UserCredentials credentials = user.getUserCredentials();
            if (credentials == null) {
                // 如果userCredentials为空，说明本来就没有失败记录，直接返回
                log.debug("User {} has no credentials, no failed attempts to reset", userId);
                return;
            }
            
            // 3. 获取当前失败次数
            Integer currentAttempts = credentials.getFailedLoginAttempts();
            
            // 4. 如果失败次数已经是0或null，直接返回
            if (currentAttempts == null || currentAttempts == 0) {
                log.debug("User {} failed login attempts already is 0, no reset needed", userId);
                return;
            }
            
            // 5. 重置失败次数为0
            credentials.setFailedLoginAttempts(0);
            
            // 6. 更新用户实体
            user.setUserCredentials(credentials);
            
            // 7. 保存到数据库
            int updateResult = userMapper.updateById(user);
            if (updateResult == 0) {
                log.error("Failed to reset failed login attempts for userId: {}", userId);
                throw new RuntimeException("重置用户登录失败次数失败");
            }
            
            log.info("Reset failed login attempts for user {} from {} to 0", userId, currentAttempts);
            
        } catch (Exception e) {
            log.error("Error resetting failed login attempts for userId: {}", userId, e);
            throw e;
        }
    }
    
    @Override
    public void validateUserCredentials(UUID userId, UserCredentials userCredentials, String username, String password) throws AuthenticationException {
        if (!encoder.matches(password, userCredentials.getPassword())) {
            int failedLoginAttempts = increaseFailedLoginAttempts(userId);
            SecurityProperties.SecuritySettingsProperties settings = securityProperties.getSettings();
            if (settings.getMaxFailedLoginAttempts() != null && settings.getMaxFailedLoginAttempts() > 0) {
                if (failedLoginAttempts > settings.getMaxFailedLoginAttempts() && userCredentials.isEnabled()) {
                    lockAccount(userId, username, settings.getUserLockoutNotificationEmail(), settings.getMaxFailedLoginAttempts());
                    throw new LockedException("Authentication Failed. Username was locked due to security policy.");
                }
            }
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        resetFailedLoginAttempts(userId);
    }
    
    /**
     * 锁定账户
     */
    private void lockAccount(UUID userId, String username, String userLockoutNotificationEmail, Integer maxFailedLoginAttempts) {
        setUserCredentialsEnabled(userId, false);
        log.warn("User {} locked due to {} failed login attempts", username, maxFailedLoginAttempts);
    }
    
}
