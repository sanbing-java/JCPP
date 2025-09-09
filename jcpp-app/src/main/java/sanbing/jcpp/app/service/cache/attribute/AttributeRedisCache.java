/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.attribute;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.app.data.kv.BaseAttributeKvEntry;
import sanbing.jcpp.infrastructure.cache.*;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AttributeCache")
public class AttributeRedisCache extends VersionedRedisCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeRedisCache(JCPPRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, LettuceConnectionFactory connectionFactory) {
        super(CacheConstants.ATTRIBUTES_CACHE, cacheSpecsMap, connectionFactory, configuration, new JCPPRedisSerializer<>() {

            @Override
            public byte[] serialize(AttributeKvEntry attribute) throws SerializationException {
                // 使用自定义序列化方法避免Optional类型问题
                if (attribute instanceof BaseAttributeKvEntry attributeKvEntry) {
                    return attributeKvEntry.toJsonBytes();
                } else {
                    // 兜底方案，如果不是BaseAttributeKvEntry类型，仍然使用JacksonUtil
                    return JacksonUtil.writeValueAsBytes(attribute);
                }
            }

            @Override
            public AttributeKvEntry deserialize(AttributeCacheKey key, byte[] bytes) throws SerializationException {
                // 使用自定义反序列化方法避免Optional类型问题
                return BaseAttributeKvEntry.fromJsonBytes(bytes);
            }
        });
    }
}
