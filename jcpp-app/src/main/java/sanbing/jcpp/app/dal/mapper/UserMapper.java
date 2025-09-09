/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import sanbing.jcpp.app.dal.entity.User;

/**
 * @author 九筒
 */
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户（默认不区分大小写）
     */
    @Select("SELECT * FROM t_user WHERE LOWER(user_name) = LOWER(#{userName})")
    User findByUserName(@Param("userName") String userName);
    
    /**
     * 检查用户名是否已存在（默认不区分大小写）
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE LOWER(user_name) = LOWER(#{userName})")
    int countByUserName(@Param("userName") String userName);
}