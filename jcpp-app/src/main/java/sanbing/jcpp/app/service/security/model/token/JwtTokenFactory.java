/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.security.model.token;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import sanbing.jcpp.app.dal.config.ibatis.enums.AuthorityEnum;
import sanbing.jcpp.app.service.security.config.SecurityProperties;
import sanbing.jcpp.app.service.security.exception.JwtExpiredTokenException;
import sanbing.jcpp.app.service.security.model.JwtPair;
import sanbing.jcpp.app.service.security.model.JwtToken;
import sanbing.jcpp.app.service.security.model.SecurityUser;
import sanbing.jcpp.app.service.security.model.UserPrincipal;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFactory {

    public static int KEY_LENGTH = Jwts.SIG.HS512.getKeyBitLength();

    private static final String SCOPES = "scopes";
    private static final String USER_ID = "userId";
    private static final String USER_NAME = "userName";
    private static final String ENABLED = "enabled";
    private static final String SESSION_ID = "sessionId";

    @Lazy
    private final SecurityProperties securityProperties;

    private volatile JwtParser jwtParser;
    private volatile SecretKey secretKey;

    /**
     * Factory method for issuing new JWT Tokens.
     */
    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser) {
        if (securityUser.getAuthority() == null) {
            throw new IllegalArgumentException("User doesn't have any privileges");
        }

        JwtBuilder jwtBuilder = setUpToken(securityUser, securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()), securityProperties.getJwt().getTokenExpirationTime());
        jwtBuilder.claim(USER_NAME, securityUser.getUserName())
                .claim(ENABLED, securityUser.isEnabled());

        String token = jwtBuilder.compact();

        return new AccessJwtToken(token);
    }

    public SecurityUser parseAccessJwtToken(String token) {
        Jws<Claims> jwsClaims = parseTokenClaims(token);
        Claims claims = jwsClaims.getPayload();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("JWT Token doesn't have any scopes");
        }

        SecurityUser securityUser = new SecurityUser(UUID.fromString(claims.get(USER_ID, String.class)));
        securityUser.setUserName(subject);
        securityUser.setAuthority(AuthorityEnum.parse(scopes.get(0)));
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }

        UserPrincipal   principal = new UserPrincipal(subject);
        securityUser.setUserPrincipal(principal);

        return securityUser;
    }

    public JwtToken createRefreshToken(SecurityUser securityUser) {

        String token = setUpToken(securityUser, Collections.singletonList(AuthorityEnum.REFRESH_TOKEN.name()), securityProperties.getJwt().getRefreshTokenExpTime())
                .id(UUID.randomUUID().toString()).compact();

        return new AccessJwtToken(token);
    }

    public SecurityUser parseRefreshToken(String token) {
        Jws<Claims> jwsClaims = parseTokenClaims(token);
        Claims claims = jwsClaims.getPayload();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Refresh Token doesn't have any scopes");
        }
        if (!scopes.get(0).equals(AuthorityEnum.REFRESH_TOKEN.name())) {
            throw new IllegalArgumentException("Invalid Refresh Token scope");
        }
        UserPrincipal principal = new UserPrincipal(subject);
        SecurityUser securityUser = new SecurityUser(UUID.fromString(claims.get(USER_ID, String.class)));
        securityUser.setUserPrincipal(principal);
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }
        return securityUser;
    }

    private JwtBuilder setUpToken(SecurityUser securityUser, List<String> scopes, long expirationTime) {
        if (StringUtils.isBlank(securityUser.getUserName())) {
            throw new IllegalArgumentException("Cannot create JWT Token without username/email");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        ClaimsBuilder claimsBuilder = Jwts.claims()
                .subject(principal.value())
                .add(USER_ID, securityUser.getId().toString())
                .add(SCOPES, scopes);
        if (securityUser.getSessionId() != null) {
            claimsBuilder.add(SESSION_ID, securityUser.getSessionId());
        }

        ZonedDateTime currentTime = ZonedDateTime.now();

        claimsBuilder.expiration(Date.from(currentTime.plusSeconds(expirationTime).toInstant()));

        return Jwts.builder()
                .claims(claimsBuilder.build())
                .issuer(securityProperties.getJwt().getTokenIssuer())
                .issuedAt(Date.from(currentTime.toInstant()))
                .signWith(getSecretKey(), Jwts.SIG.HS512);
    }

    public Jws<Claims> parseTokenClaims(String token) {
        try {
            return getJwtParser().parseSignedClaims(token);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT Token", ex);
            throw new BadCredentialsException("Invalid JWT token: ", ex);
        } catch (ExpiredJwtException expiredEx) {
            log.debug("JWT Token is expired", expiredEx);
            throw new JwtExpiredTokenException(token, "JWT Token expired", expiredEx);
        }
    }

    public JwtPair createTokenPair(SecurityUser securityUser) {
        securityUser.setSessionId(UUID.randomUUID().toString());
        JwtToken accessToken = createAccessJwtToken(securityUser);
        JwtToken refreshToken = createRefreshToken(securityUser);
        return new JwtPair(accessToken.token(), refreshToken.token());
    }

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    byte[] decodedToken = Base64.getDecoder().decode(securityProperties.getJwt().getTokenSigningKey());
                    secretKey = new SecretKeySpec(decodedToken, "HmacSHA512");
                }
            }
        }
        return secretKey;
    }

    private JwtParser getJwtParser() {
        if (jwtParser == null) {
            synchronized (this) {
                if (jwtParser == null) {
                    jwtParser = Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(securityProperties.getJwt().getTokenSigningKey())))
                            .build();
                }
            }
        }
        return jwtParser;
    }
}
