/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.gun;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.infrastructure.cache.*;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("GunCache")
public class GunRedisCache extends VersionedRedisCache<GunCacheKey, Gun> {

    public GunRedisCache(JCPPRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, LettuceConnectionFactory connectionFactory) {
        super(CacheConstants.GUN_CACHE, cacheSpecsMap, connectionFactory, configuration, new JCPPRedisSerializer<>() {

            @Override
            public byte[] serialize(Gun gun) throws SerializationException {
                return JacksonUtil.writeValueAsBytes(gun);
            }

            @Override
            public Gun deserialize(GunCacheKey key, byte[] bytes) throws SerializationException {
                return JacksonUtil.fromBytes(bytes, Gun.class);
            }
        });
    }
}
