/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.attribute;

import lombok.Builder;
import sanbing.jcpp.infrastructure.cache.VersionedCacheKey;

import java.io.Serial;
import java.util.UUID;

@Builder
public record AttributeCacheKey(UUID entityId, String attrKey) implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return entityId + ":" + attrKey;
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

}
