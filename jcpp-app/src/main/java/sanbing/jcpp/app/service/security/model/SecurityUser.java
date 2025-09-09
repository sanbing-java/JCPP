/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.security.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import sanbing.jcpp.app.dal.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class SecurityUser extends User {

    private static final long serialVersionUID = -797397440703066079L;

    private Collection<GrantedAuthority> authorities;
    @Getter @Setter
    private boolean enabled;
    @Getter
    @Setter
    private UserPrincipal userPrincipal;
    @Getter
    @Setter
    private String sessionId = UUID.randomUUID().toString();

    public SecurityUser() {
        super();
    }

    public SecurityUser(UUID id) {
        super(id);
    }

    public SecurityUser(User user, UserPrincipal userPrincipal) {
        super(user);
        this.userPrincipal = userPrincipal;
    }
    public SecurityUser(User user, boolean enabled, UserPrincipal userPrincipal) {
        super(user);
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
    }
    public Collection<GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        }
        return authorities;
    }
}
