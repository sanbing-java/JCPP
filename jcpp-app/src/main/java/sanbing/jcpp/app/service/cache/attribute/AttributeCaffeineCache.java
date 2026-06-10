/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.service.cache.attribute;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.infrastructure.cache.CacheConstants;
import sanbing.jcpp.infrastructure.cache.VersionedCaffeineCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AttributeCache")
public class AttributeCaffeineCache extends VersionedCaffeineCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ATTRIBUTES_CACHE);
    }

}
