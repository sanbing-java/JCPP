/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.infrastructure.cache.CacheConstants;
import sanbing.jcpp.infrastructure.cache.CaffeineTransactionalCache;

/**
 * @author 九筒
 */
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("PileSessionCache")
public class PileSessionCaffeineCache  extends CaffeineTransactionalCache<PileSessionCacheKey, PileSession> {

    public PileSessionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.PILE_SESSION_CACHE);
    }
}