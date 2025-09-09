/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sanbing.jcpp.app.dal.config.ibatis.enums.AuthorityEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.UserStatusEnum;
import sanbing.jcpp.app.dal.config.ibatis.typehandlers.UserCredentialsTypeHandler;
import sanbing.jcpp.app.service.security.model.UserCredentials;
import sanbing.jcpp.infrastructure.cache.HasVersion;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@TableName("t_user")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable, HasVersion {

    public User(UUID id) {
       this.id = id;
    }

    public User(User user) {
        this.id = user.getId();
        this.createdTime = user.getCreatedTime();
        this.updatedTime = user.getUpdatedTime();
        this.additionalInfo = user.getAdditionalInfo();
        this.status = user.getStatus();
        this.userName = user.getUserName();
        this.userCredentials = user.getUserCredentials();
        this.authority = user.getAuthority();
        this.version = user.getVersion();
    }

    @TableId(type = IdType.INPUT)
    private UUID id;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private JsonNode additionalInfo;

    private UserStatusEnum status;

    private String userName;

    @TableField(typeHandler = UserCredentialsTypeHandler.class)
    private UserCredentials userCredentials;

    private AuthorityEnum authority;

    private Integer version;

}
