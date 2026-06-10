/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import io.lettuce.core.api.StatefulConnection;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Data
@Slf4j
public abstract class JCPPRedisCacheConfiguration {

    private static final String COMMA = ",";
    private static final String COLON = ":";

    @Value("${redis.evictTtlInMs:60000}")
    private int evictTtlInMs;

    @Value("${redis.pool_config.maxTotal:128}")
    private int maxTotal;

    @Value("${redis.pool_config.maxIdle:128}")
    private int maxIdle;

    @Value("${redis.pool_config.minIdle:16}")
    private int minIdle;

    @Value("${redis.pool_config.testOnBorrow:true}")
    private boolean testOnBorrow;

    @Value("${redis.pool_config.testOnReturn:true}")
    private boolean testOnReturn;

    @Value("${redis.pool_config.testWhileIdle:true}")
    private boolean testWhileIdle;

    @Value("${redis.pool_config.minEvictableMs:60000}")
    private long minEvictableMs;

    @Value("${redis.pool_config.evictionRunsMs:30000}")
    private long evictionRunsMs;

    @Value("${redis.pool_config.maxWaitMills:60000}")
    private long maxWaitMills;

    @Value("${redis.pool_config.numberTestsPerEvictionRun:3}")
    private int numberTestsPerEvictionRun;

    @Value("${redis.pool_config.blockWhenExhausted:true}")
    private boolean blockWhenExhausted;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(LettuceConnectionFactory loadFactory) {
        return loadFactory;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(LettuceConnectionFactory loadFactory) {
        return loadFactory;
    }

    @Bean
    protected abstract LettuceConnectionFactory loadFactory();

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        DefaultFormattingConversionService redisConversionService = new DefaultFormattingConversionService();
        RedisCacheConfiguration.registerDefaultConverters(redisConversionService);
        registerDefaultConverters(redisConversionService);
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig().withConversionService(redisConversionService);
        return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(configuration)
                .transactionAware()
                .build();
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(new StringRedisSerializer())
                .value(new GenericJackson2JsonRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(new GenericJackson2JsonRedisSerializer())
                .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    private static void registerDefaultConverters(ConverterRegistry registry) {
        Assert.notNull(registry, "ConverterRegistry must not be null!");
        registry.addConverter(UUID.class, String.class, UUID::toString);
    }

    protected GenericObjectPoolConfig<StatefulConnection<?, ?>> buildPoolConfig() {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setTestOnReturn(testOnReturn);
        poolConfig.setTestWhileIdle(testWhileIdle);
        poolConfig.setSoftMinEvictableIdleDuration(Duration.ofMillis(minEvictableMs));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(evictionRunsMs));
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMills));
        poolConfig.setNumTestsPerEvictionRun(numberTestsPerEvictionRun);
        poolConfig.setBlockWhenExhausted(blockWhenExhausted);
        return poolConfig;
    }

    protected List<RedisNode> getNodes(String nodes) {
        List<RedisNode> result;
        if (!StringUtils.hasText(nodes)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : nodes.split(COMMA)) {
                String host = hostPort.split(COLON)[0];
                int port = Integer.parseInt(hostPort.split(COLON)[1]);
                result.add(new RedisNode(host, port));
            }
        }
        return result;
    }

}
