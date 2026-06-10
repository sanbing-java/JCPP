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
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

@Configuration
@ConditionalOnExpression("'${cache.type:null}'=='redis' && '${redis.connection.type:null}'=='sentinel'")
@Slf4j
public class JCPPJCPPRedisSentinelConfiguration extends JCPPRedisCacheConfiguration {

    @Value("${redis.sentinel.master:}")
    private String master;

    @Value("${redis.sentinel.sentinels:}")
    private String sentinels;

    @Value("${redis.sentinel.password:}")
    private String sentinelPassword;

    @Value("${redis.sentinel.useDefaultPoolConfig:true}")
    private boolean useDefaultPoolConfig;

    @Value("${redis.db:}")
    private Integer database;

    @Value("${redis.password:}")
    private String password;

    @Override
    public LettuceConnectionFactory loadFactory() {
        log.info("Initializing Redis Sentinel on {}, sentinels: {}", master, sentinels);
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration();
        redisSentinelConfiguration.setMaster(master);
        redisSentinelConfiguration.setSentinels(getNodes(sentinels));
        redisSentinelConfiguration.setSentinelPassword(sentinelPassword);
        redisSentinelConfiguration.setPassword(password);
        redisSentinelConfiguration.setDatabase(database);
        return new LettuceConnectionFactory(redisSentinelConfiguration, buildClientConfig());
    }

    private LettucePoolingClientConfiguration buildClientConfig() {
        var clientConfigurationBuilder = LettucePoolingClientConfiguration.builder();
        if (!useDefaultPoolConfig) {
            clientConfigurationBuilder.poolConfig(buildPoolConfig());
        }
        return clientConfigurationBuilder.build();
    }
}
