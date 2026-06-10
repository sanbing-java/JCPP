/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public interface VersionedCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends TransactionalCache<K, V> {

    CacheValueWrapper<V> get(K key);

    default V get(K key, Supplier<V> supplier) {
        return get(key, supplier, true);
    }

    default V get(K key, Supplier<V> supplier, boolean putToCache) {
        return Optional.ofNullable(get(key))
                .map(CacheValueWrapper::get)
                .orElseGet(() -> {
                    V value = supplier.get();
                    if (putToCache) {
                        put(key, value);
                    }
                    return value;
                });
    }

    void put(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evict(K key, Integer version);

    default Integer getVersion(V value) {
        if (value == null) {
            return 0;
        } else if (value.getVersion() != null) {
            return value.getVersion();
        } else {
            return null;
        }
    }

}
