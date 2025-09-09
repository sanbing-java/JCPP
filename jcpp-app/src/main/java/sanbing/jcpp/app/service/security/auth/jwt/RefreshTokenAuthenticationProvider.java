/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import sanbing.jcpp.app.dal.entity.User;
import sanbing.jcpp.app.service.UserService;
import sanbing.jcpp.app.service.security.auth.RefreshAuthenticationToken;
import sanbing.jcpp.app.service.security.model.SecurityUser;
import sanbing.jcpp.app.service.security.model.UserPrincipal;
import sanbing.jcpp.app.service.security.model.token.JwtTokenFactory;
import sanbing.jcpp.app.service.security.model.token.RawAccessJwtToken;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {
    private final JwtTokenFactory tokenFactory;
    private final UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken.token());

        SecurityUser securityUser = authenticateByUserId(unsafeUser.getId());
        securityUser.setSessionId(unsafeUser.getSessionId());
        return new RefreshAuthenticationToken(securityUser);
    }

    private SecurityUser authenticateByUserId(UUID userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("User not found by refresh token");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user.getUserName());

        return new SecurityUser(user, userPrincipal);
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return (RefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
