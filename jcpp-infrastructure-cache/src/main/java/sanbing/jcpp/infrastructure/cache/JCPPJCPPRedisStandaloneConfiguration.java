/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

@Configuration
@ConditionalOnExpression("'${cache.type:null}'=='redis' && '${redis.connection.type:null}'=='standalone'")
@Slf4j
public class JCPPJCPPRedisStandaloneConfiguration extends JCPPRedisCacheConfiguration {

    @Value("${redis.standalone.host:localhost}")
    private String host;

    @Value("${redis.standalone.port:6379}")
    private Integer port;

    @Value("${redis.standalone.clientName:standalone}")
    private String clientName;

    @Value("${redis.standalone.commandTimeout:30000}")
    private Long commandTimeout;

    @Value("${redis.standalone.shutdownTimeout:5000}")
    private Long shutdownTimeout;

    @Value("${redis.standalone.useDefaultClientConfig:false}")
    private boolean useDefaultClientConfig;

    @Value("${redis.standalone.usePoolConfig:true}")
    private boolean usePoolConfig;

    @Value("${redis.db:0}")
    private Integer db;

    @Value("${redis.password:}")
    private String password;

    @Override
    public LettuceConnectionFactory loadFactory() {
        log.info("Initializing Redis Standalone on {}:{}", host, port);
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(host);
        standaloneConfiguration.setPort(port);
        standaloneConfiguration.setDatabase(db);
        standaloneConfiguration.setPassword(password);
        return new LettuceConnectionFactory(standaloneConfiguration, buildClientConfig());
    }

    private LettucePoolingClientConfiguration buildClientConfig() {

        var clientConfigurationBuilder = LettucePoolingClientConfiguration.builder();

        if (!useDefaultClientConfig) {
            clientConfigurationBuilder
                    .clientName(clientName)
                    .commandTimeout(Duration.ofMillis(commandTimeout))
                    .shutdownTimeout(Duration.ofMillis(shutdownTimeout));
        }

        if (usePoolConfig) {
            clientConfigurationBuilder.poolConfig(buildPoolConfig());
        }
        return clientConfigurationBuilder.build();
    }
}
