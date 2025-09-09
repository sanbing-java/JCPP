/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import sanbing.jcpp.AbstractTestBase;
import sanbing.jcpp.app.dal.config.ibatis.enums.AuthorityEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.UserStatusEnum;
import sanbing.jcpp.app.dal.entity.User;
import sanbing.jcpp.app.service.security.model.UserCredentials;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author 九筒
 */
class UserMapperIT extends AbstractTestBase {
    static final UUID NORMAL_USER_ID = UUID.fromString("21cbf909-a23a-4396-840a-f34061f59f95");

    @Resource
    private UserMapper userMapper;

    @Test
    void curdTest() {
        userMapper.delete(Wrappers.lambdaQuery());

        // 创建UserCredentials对象
        UserCredentials credentials = new UserCredentials();
        credentials.setPassword("$2a$10$mE.qmcV0mFU5NcKh73TZx.z4ueI/.bDWbj0T1BYyqP481kGGarKLG"); // encoded "password123"
        credentials.setEnabled(true);
        credentials.setFailedLoginAttempts(0);

        User user = User.builder()
                .id(NORMAL_USER_ID)
                .createdTime(LocalDateTime.now())
                .additionalInfo(JacksonUtil.newObjectNode())
                .status(UserStatusEnum.ENABLE)
                .userName("sanbing")
                .userCredentials(credentials)
                .authority(AuthorityEnum.SYS_ADMIN)  // 添加权限字段
                .version(1)  // 添加版本字段
                .build();

        userMapper.insertOrUpdate(user);

        User savedUser = userMapper.selectById(NORMAL_USER_ID);
        log.info("Saved user: {}", savedUser);
        
        // 验证UserCredentials字段正确保存和读取
        assert savedUser != null;
        assert savedUser.getUserCredentials() != null;
        assert savedUser.getUserCredentials().isEnabled();
        assert "sanbing".equals(savedUser.getUserName());
        assert AuthorityEnum.SYS_ADMIN.equals(savedUser.getAuthority());
    }
}