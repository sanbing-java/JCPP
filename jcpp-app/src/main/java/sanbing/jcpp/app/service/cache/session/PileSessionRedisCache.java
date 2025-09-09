/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.infrastructure.cache.*;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

/**
 * @author 九筒
 */
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("PileSessionCache")
public class PileSessionRedisCache extends RedisTransactionalCache<PileSessionCacheKey, PileSession> {

    public PileSessionRedisCache(JCPPRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, LettuceConnectionFactory connectionFactory) {
        super(CacheConstants.PILE_SESSION_CACHE, cacheSpecsMap, connectionFactory, configuration, new JCPPRedisSerializer<>() {

            @Override
            public byte[] serialize(PileSession pileSession) throws SerializationException {
                return JacksonUtil.writeValueAsBytes(pileSession);
            }

            @Override
            public PileSession deserialize(PileSessionCacheKey key, byte[] bytes) throws SerializationException {
                return JacksonUtil.fromBytes(bytes, PileSession.class);
            }
        });
    }
}