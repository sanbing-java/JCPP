/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.gun;

import lombok.Builder;
import sanbing.jcpp.infrastructure.cache.VersionedCacheKey;

import java.io.Serial;
import java.util.UUID;

@Builder
public record GunCacheKey(UUID gunId, String pileCode, String gunCode) implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 1L;

    public GunCacheKey(UUID gunId) {
        this(gunId, null, null);
    }

    public GunCacheKey(String pileCode, String gunCode) {
        this(null, pileCode, gunCode);
    }

    @Override
    public String toString() {
        if (gunId != null) {
            return gunId.toString();
        } else if (pileCode != null && gunCode != null) {
            return pileCode + ":" + gunCode;
        } else {
            throw new IllegalStateException("GunCacheKey 必须包含有效的 gunId 或者 pileCode+gunCode 组合");
        }
    }

    @Override
    public boolean isVersioned() {
        return gunId == null;
    }
}
