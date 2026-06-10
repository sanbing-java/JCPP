/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.service.cache.pile;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.infrastructure.cache.*;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("PileCache")
public class PileRedisCache extends VersionedRedisCache<PileCacheKey, Pile> {

    public PileRedisCache(JCPPRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, LettuceConnectionFactory connectionFactory) {
        super(CacheConstants.PILE_CACHE, cacheSpecsMap, connectionFactory, configuration, new JCPPRedisSerializer<>() {

            @Override
            public byte[] serialize(Pile pile) throws SerializationException {
                return JacksonUtil.writeValueAsBytes(pile);
            }

            @Override
            public Pile deserialize(PileCacheKey key, byte[] bytes) throws SerializationException {
                return JacksonUtil.fromBytes(bytes, Pile.class);
            }
        });
    }
}
