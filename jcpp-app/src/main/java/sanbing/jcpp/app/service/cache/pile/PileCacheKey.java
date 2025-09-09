/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.pile;

import lombok.Builder;
import sanbing.jcpp.infrastructure.cache.VersionedCacheKey;

import java.io.Serial;
import java.util.Optional;
import java.util.UUID;

@Builder
public record PileCacheKey(UUID pileId, String pileCode) implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 1L;

    public PileCacheKey(UUID pileId) {
        this(pileId, null);
    }

    public PileCacheKey(String pileCode) {
        this(null, pileCode);
    }

    @Override
    public String toString() {
        return Optional.ofNullable(pileId).map(UUID::toString).orElse(pileCode);
    }

    @Override
    public boolean isVersioned() {
        return pileId == null;
    }

}
